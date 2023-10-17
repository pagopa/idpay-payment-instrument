package it.gov.pagopa.payment.instrument.service.idpaycode;

import com.azure.security.keyvault.keys.KeyClient;
import com.azure.security.keyvault.keys.cryptography.CryptographyClient;
import com.azure.security.keyvault.keys.cryptography.models.EncryptionAlgorithm;
import com.azure.security.keyvault.keys.models.KeyVaultKey;
import it.gov.pagopa.common.azure.keyvault.AzureEncryptUtils;
import it.gov.pagopa.payment.instrument.dto.EncryptedDataBlock;
import it.gov.pagopa.payment.instrument.dto.PinBlockDTO;
import it.gov.pagopa.payment.instrument.exception.PaymentInstrumentException;
import lombok.NonNull;
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
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class IdpayCodeEncryptionServiceImpl implements IdpayCodeEncryptionService {

  public static final String GENERATE_PIN_BLOCK = "GENERATE_PIN_BLOCK";
  public static final String HASH_PIN_BLOCK = "HASH_PIN_BLOCK";
  private final String cipherInstance;
  private final byte[] iv = new byte[16];
  private final String keyName_dataBlock;
  private final String keyName_secretKey;
  private final KeyClient keyClient;
  private final Map<String, CryptographyClient> cryptoClientCache = new ConcurrentHashMap<>();

  public IdpayCodeEncryptionServiceImpl(
      @Value("${crypto.aes.cipherInstance}") String cipherInstance,
      KeyClient keyClient,
      @Value("${crypto.azure.key-vault.key-names.data-block}") String keyNameDataBlock,
      @Value("${crypto.azure.key-vault.key-names.secret-key}")String keyNameSecretKey) {
    this.cipherInstance = cipherInstance;
    this.keyName_dataBlock = keyNameDataBlock;
    this.keyName_secretKey = keyNameSecretKey;
    this.keyClient = keyClient;
  }

  @Override
  public String buildHashedDataBlock(String code, String secondFactor, String salt) {
    String dataBlock = calculateDataBlock(secondFactor, code);
    return createSHA256Digest(dataBlock, salt);
  }


  /** Calculate Data Block from plain code and second factor */
  private String calculateDataBlock(String secondFactor, String code) {
    long startTime = System.currentTimeMillis();
    // Control code length, must be at least 5
    try {
      if (code.length() < 5) {
        throw new PaymentInstrumentException(400, "Pin length is not valid");
      }

      // Standardizes code (adds padding with "F" to reach 16 digits)
      final String codeData = StringUtils.rightPad(code, 16,'F');

      // Combine CODE and SECOND_FACTOR with XOR
      final byte[] codeBytes = Hex.decodeHex(codeData.toCharArray());
      final byte[] secondFactorBytes = Hex.decodeHex(secondFactor.toCharArray());
      final byte[] xorResult = new byte[8];
      for (int i = 0; i < 8; i++) {
        xorResult[i] = (byte) (codeBytes[i] ^ secondFactorBytes[i]);
      }

      // Converts the result to a hexadecimal representation
      String dataBlock = Hex.encodeHexString(xorResult);
      // log to be deleted
      log.info("Code in chiaro: {}, DataBlock: {}", code, dataBlock);
      performanceLog(startTime, GENERATE_PIN_BLOCK);

      return dataBlock;
    } catch (DecoderException ex) {
      throw new PaymentInstrumentException(500, "Something went wrong while creating pinBlock");
    }
  }

  /**
   * Hash Pin Block that arrives in input in three steps
   * First step: Decrypt symmetric key with Azure API
   * Second step: Decrypt PinBlock with AES
   * Third step: Hash decrypted pinBlock with SHA256
   */
  @Override
  public String hashSHADecryptedDataBlock(String userId, PinBlockDTO pinBlockDTO, String salt) {
    String decryptedSymmetricKey = decryptSymmetricKey(pinBlockDTO.getEncryptedKey());
    String dataBlock = decryptPinBlockWithSymmetricKey(pinBlockDTO.getPinBlock(), decryptedSymmetricKey);

    return createSHA256Digest(dataBlock, salt);
  }

  /**  Encrypt hashed(SHA256) Data Block with Azure API */
  @Override
  public EncryptedDataBlock encryptSHADataBlock(String dataBlock) {
    KeyVaultKey key = keyClient.getKey(keyName_dataBlock);

    CryptographyClient cryptographyClient = cryptoClientCache.computeIfAbsent(
        key.getId(), AzureEncryptUtils::buildCryptographyClient);

    return new EncryptedDataBlock(AzureEncryptUtils.encrypt(dataBlock, EncryptionAlgorithm.RSA_OAEP, cryptographyClient), key.getId());
  }

  @Override
  public String decryptSymmetricKey(String symmetricKey) {
    KeyVaultKey key = keyClient.getKey(keyName_secretKey);

    CryptographyClient cryptographyClient = cryptoClientCache.computeIfAbsent(
        key.getId(), AzureEncryptUtils::buildCryptographyClient);
    return AzureEncryptUtils.decrypt(symmetricKey, EncryptionAlgorithm.RSA_OAEP, cryptographyClient);
  }

  /**  Hashing pinBlock with algorithm SHA-256 */
  @Override
  public String createSHA256Digest(String dataBlock, String salt) {
    long startTime = System.currentTimeMillis();
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      md.update(salt.getBytes(StandardCharsets.UTF_8));
      byte[] hash = md.digest(dataBlock.getBytes(StandardCharsets.UTF_8));

      String hashedPinBlock = Base64.getEncoder().encodeToString(hash);

      // this log must be of type debug and without hashedPinBlock
      log.info("[{}] pinBlock hashing done successfully: {}", HASH_PIN_BLOCK, hashedPinBlock);
      performanceLog(startTime, HASH_PIN_BLOCK);
      return hashedPinBlock;
    } catch (NoSuchAlgorithmException e) {
      throw new PaymentInstrumentException(403, "Something went wrong creating SHA256 digest");
    }
  }

  @Override
  public String decryptIdpayCode(EncryptedDataBlock encryptedDataBlock) {

    CryptographyClient cryptographyClient = cryptoClientCache.computeIfAbsent(
        encryptedDataBlock.getKeyId(), AzureEncryptUtils::buildCryptographyClient);

    return AzureEncryptUtils.decrypt(
        encryptedDataBlock.getEncryptedDataBlock(), EncryptionAlgorithm.RSA_OAEP, cryptographyClient);
  }

  /**  Decrypt(AES) PinBlock with symmetric key */
  @NonNull
  private String decryptPinBlockWithSymmetricKey(String encryptedPinBlock, String encryptedKey) {
    SecretKeySpec secretKeySpec = new SecretKeySpec(encryptedKey.getBytes(), "AES");

    try {
      byte[] decryptedBytes = decrypt(secretKeySpec, Hex.decodeHex(encryptedPinBlock));

      return new String(decryptedBytes);
    } catch (DecoderException e){
      throw new IllegalStateException("Something gone wrong while extracting datablock from pinblock", e);
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
