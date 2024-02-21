package it.gov.pagopa.payment.instrument.service.idpaycode;

import com.azure.security.keyvault.keys.KeyClient;
import com.azure.security.keyvault.keys.cryptography.CryptographyClient;
import com.azure.security.keyvault.keys.cryptography.models.EncryptionAlgorithm;
import com.azure.security.keyvault.keys.models.KeyVaultKey;
import it.gov.pagopa.common.azure.keyvault.AzureEncryptUtils;
import it.gov.pagopa.payment.instrument.dto.EncryptedDataBlock;
import it.gov.pagopa.payment.instrument.dto.PinBlockDTO;
import it.gov.pagopa.payment.instrument.exception.custom.IdpayCodeEncryptOrDecryptException;
import it.gov.pagopa.payment.instrument.exception.custom.PinBlockException;
import it.gov.pagopa.payment.instrument.exception.custom.PinBlockSizeException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants.ExceptionCode.DECRYPTION_ERROR;
import static it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants.ExceptionCode.ENCRYPTION_ERROR;
import static it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants.ExceptionMessage.*;

@Service
@Slf4j
public class IdpayCodeEncryptionServiceImpl implements IdpayCodeEncryptionService {

  public static final String GENERATE_PIN_BLOCK = "GENERATE_PIN_BLOCK";
  public static final String HASH_PIN_BLOCK = "HASH_PIN_BLOCK";
  private final String cipherInstance;
  private final byte[] iv = new byte[16];
  private final String keyNameDataBlock;
  private final String keyNameSecretKey;
  private final KeyClient keyClient;
  private final Map<String, CryptographyClient> cryptoClientCache = new ConcurrentHashMap<>();

  public IdpayCodeEncryptionServiceImpl(
      @Value("${crypto.aes.cipherInstance}") String cipherInstance,
      KeyClient keyClient,
      @Value("${crypto.azure.key-vault.key-names.data-block}") String keyNameDataBlock,
      @Value("${crypto.azure.key-vault.key-names.secret-key}")String keyNameSecretKey) {
    this.cipherInstance = cipherInstance;
    this.keyNameDataBlock = keyNameDataBlock;
    this.keyNameSecretKey = keyNameSecretKey;
    this.keyClient = keyClient;
  }

  @Override
  public byte[] buildHashedDataBlock(String code, String secondFactor, String salt) {
    byte[] dataBlock = calculateDataBlock(secondFactor, code);
    return createSHA256Digest(dataBlock, salt);
  }


  /** Calculate Data Block from plain code and second factor */
  private byte[] calculateDataBlock(String secondFactor, String code) {
    long startTime = System.currentTimeMillis();
    // Control code length, must be at least 5
    try {
      if (code.length() < 5) {
        throw new PinBlockSizeException(ERROR_PIN_LENGTH_NOT_VALID_MSG);
      }

      // Standardizes code (adds prefix 0 + code length and padding with "F" to reach 16 digits)
      String starter = "0";
      char padding = 'F';
      code = starter.concat(String.valueOf(code.length())).concat(code);
      final String codeData = StringUtils.rightPad(code, 16,padding);

      // Combine CODE and SECOND_FACTOR with XOR
      final byte[] codeBytes = Hex.decodeHex(codeData.toCharArray());
      final byte[] secondFactorBytes = Hex.decodeHex(secondFactor.toCharArray());
      final byte[] xorResult = new byte[8];
      for (int i = 0; i < 8; i++) {
        xorResult[i] = (byte) (codeBytes[i] ^ secondFactorBytes[i]);
      }

      performanceLog(startTime, GENERATE_PIN_BLOCK);
      return xorResult;
    } catch (DecoderException ex) {
      log.error("[GENERATE_PIN_BLOCK] Something went wrong while creating pinBlock");
      throw new PinBlockException(ERROR_CREATING_PINBLOCK_MSG,true,ex);
    }
  }

  /**
   * Hash Pin Block that arrives in input in three steps
   * First step: Decrypt symmetric key with Azure API
   * Second step: Decrypt PinBlock with AES
   * Third step: Hash decrypted pinBlock with SHA256
   */
  @Override
  public byte[] hashSHADecryptedDataBlock(String userId, PinBlockDTO pinBlockDTO, String salt) {
    byte[] decryptedSymmetricKey = decryptSymmetricKey(pinBlockDTO.getEncryptedKey());
    byte[] dataBlock = decryptPinBlockWithSymmetricKey(pinBlockDTO.getPinBlock(), decryptedSymmetricKey);

    return createSHA256Digest(dataBlock, salt);
  }

  /**  Encrypt hashed(SHA256) Data Block with Azure API */
  @Override
  public EncryptedDataBlock encryptSHADataBlock(byte[] dataBlock) {
    KeyVaultKey key = keyClient.getKey(keyNameDataBlock);

    CryptographyClient cryptographyClient = cryptoClientCache.computeIfAbsent(
        key.getId(), AzureEncryptUtils::buildCryptographyClient);

    return new EncryptedDataBlock(AzureEncryptUtils.encrypt(dataBlock, EncryptionAlgorithm.RSA_OAEP_256, cryptographyClient), key.getId());
  }

  @Override
  public byte[] decryptSymmetricKey(String symmetricKey) {
    KeyVaultKey key = keyClient.getKey(keyNameSecretKey);

    CryptographyClient cryptographyClient = cryptoClientCache.computeIfAbsent(
        key.getId(), AzureEncryptUtils::buildCryptographyClient);
    return AzureEncryptUtils.decrypt(symmetricKey, EncryptionAlgorithm.RSA_OAEP_256, cryptographyClient);
  }

  /**
   * Hashing pinBlock with algorithm SHA-256
   */
  @Override
  public byte[] createSHA256Digest(byte[] dataBlock, String salt) {
    long startTime = System.currentTimeMillis();
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      md.update(salt.getBytes(StandardCharsets.UTF_8));
      byte[] hash = md.digest(dataBlock);

      log.debug("[{}] pinBlock hashing done successfully", HASH_PIN_BLOCK);
      performanceLog(startTime, HASH_PIN_BLOCK);
      return hash;
    } catch (NoSuchAlgorithmException e) {
      performanceLog(startTime, HASH_PIN_BLOCK);
      log.error("[{}] Something went wrong creating SHA256 digest", HASH_PIN_BLOCK);
      throw new IdpayCodeEncryptOrDecryptException(ENCRYPTION_ERROR, ENCRYPTION_ERROR_MSG, true, e);
    }
  }

  @Override
  public byte[] decryptIdpayCode(EncryptedDataBlock encryptedDataBlock) {

    CryptographyClient cryptographyClient = cryptoClientCache.computeIfAbsent(
        encryptedDataBlock.getKeyId(), AzureEncryptUtils::buildCryptographyClient);

    return AzureEncryptUtils.decrypt(
        encryptedDataBlock.getEncryptedDataBlock(), EncryptionAlgorithm.RSA_OAEP_256, cryptographyClient);
  }

  /**  Decrypt(AES) PinBlock with symmetric key */
  private byte[] decryptPinBlockWithSymmetricKey(String encryptedPinBlock, byte[] encryptedKey) {
    try {
      SecretKeySpec secretKeySpec = new SecretKeySpec(encryptedKey, "AES");
      return decrypt(secretKeySpec, Hex.decodeHex(encryptedPinBlock));
    } catch (DecoderException
             | IllegalStateException e){
      throw new IdpayCodeEncryptOrDecryptException(DECRYPTION_ERROR, DECRYPTION_ERROR_MSG, true, e);
    }
  }

  private byte[] decrypt(SecretKey secretKeySpec, byte[] bytes) {
    try{
      Cipher cipher = Cipher.getInstance(cipherInstance);
      IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

      cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
      return cipher.doFinal(bytes);

    }catch (InvalidKeyException
            | InvalidAlgorithmParameterException
            | IllegalBlockSizeException
            | NoSuchPaddingException
            | NoSuchAlgorithmException
            | BadPaddingException e) {
      throw new IllegalStateException(e);
    }
  }

  private void performanceLog(long startTime, String service){
    log.info(
        "[PERFORMANCE_LOG] [{}] Time occurred to perform business logic: {} ms",
        service,
        System.currentTimeMillis() - startTime);
  }
}
