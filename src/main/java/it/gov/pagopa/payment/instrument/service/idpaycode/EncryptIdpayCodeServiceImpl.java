package it.gov.pagopa.payment.instrument.service.idpaycode;

import it.gov.pagopa.payment.instrument.dto.PinBlockDTO;
import it.gov.pagopa.payment.instrument.exception.PaymentInstrumentException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EncryptIdpayCodeServiceImpl implements EncryptIdpayCodeService {

  public static final String GENERATE_PIN_BLOCK = "GENERATE_PIN_BLOCK";
  public static final String HASH_PIN_BLOCK = "HASH_PIN_BLOCK";
  private final String cipherInstance;
  private final String iv;

  public EncryptIdpayCodeServiceImpl(
      @Value("${util.crypto.aes.cipherInstance}") String cipherInstance,
      @Value("${util.crypto.aes.mode.gcm.iv}") String iv) {
    this.cipherInstance = cipherInstance;
    this.iv = iv;
  }

  @Override
  public String buildHashedDataBlock(String code, String secondFactor, String salt) {
    String dataBlock = calculateDataBlock(secondFactor, code);
    return createSHA256Digest(dataBlock, salt);
  }


  /** Calculate Data Block from plain code and second factor */
  private String calculateDataBlock(String secondFactor, String code) {
    long startTime = System.currentTimeMillis();
    // Control code length, must be 5
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
      // log da rimuovere
      log.info("Code in chiaro: {}, DataBlock: {}", code, dataBlock);
      performanceLog(startTime, GENERATE_PIN_BLOCK);

      return dataBlock;
    } catch (DecoderException ex) {
      throw new PaymentInstrumentException(500, "Something went wrong while creating pinBlock");
    }
  }

  /**
   * Verify correct Pin Block in three steps
   * First step: Decrypt symmetric key with Azure API
   * Second step: Decrypt PinBlock with AES
   * Third step: Hash decrypted pinBlock with SHA256
   */
  @Override
  public String verifyPinBlock(String userId, PinBlockDTO pinBlockDTO, String salt) {
    String decryptSymmetricKey = decryptSymmetricKey(pinBlockDTO.getEncryptedKey());
    // TODO String decryptedPin = decryptPinBlockWithSymmetricKey(pinBlockDTO.getEncryptedPinBlock(), decryptSymmetricKey);

    // Change pinBlockDTO.getEncryptedPinBlock() with decryptSymmetricKey variable
    return createSHA256Digest(pinBlockDTO.getEncryptedPinBlock(), salt);
  }

  @Override
  public String encryptSHADataBlock(String dataBlock) {
    //TODO encrypt sha data block (POC)
    return dataBlock;
  }

  @Override
  public String decryptSymmetricKey(String symmetricKey) {
    //TODO decrypt symmetric key
    return symmetricKey;
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

      log.debug("[{}] pinBlock hashing done successfully", HASH_PIN_BLOCK);
      performanceLog(startTime, HASH_PIN_BLOCK);
      return hashedPinBlock;
    } catch (NoSuchAlgorithmException e) {
      throw new PaymentInstrumentException(403, "Something went wrong creating SHA256 digest");
    }
  }

  /**  Decrypt(AES) PinBlock with symmetric key */
  @NonNull
  private String decryptPinBlockWithSymmetricKey(String encryptedPinBlock, String encryptedKey) {
    SecretKeySpec secretKeySpec = new SecretKeySpec(encryptedKey.getBytes(), "AES");

    byte[] decryptedBytes = doFinal(secretKeySpec, Base64.getDecoder().decode(encryptedPinBlock));

    return new String(decryptedBytes);
  }

  private byte[] doFinal(SecretKey secretKeySpec, byte[] bytes) {
    try{
      Cipher cipher = Cipher.getInstance(cipherInstance);
      IvParameterSpec ivParameterSpec = new IvParameterSpec(iv.getBytes());

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
