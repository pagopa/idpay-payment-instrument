package it.gov.pagopa.payment.instrument.service.idpaycode;

import it.gov.pagopa.payment.instrument.exception.PaymentInstrumentException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EncryptCodeServiceImpl implements EncryptCodeService {

  public static final String GENERATE_PIN_BLOCK = "GENERATE_PIN_BLOCK";
  public static final String HASH_PIN_BLOCK = "HASH_PIN_BLOCK";

  @Override
  public String buildHashedPinBlock(String code, String secondFactor, String salt) {
    String pinBlock = calculatePinBlock(secondFactor, code);
    return createSHA256Digest(pinBlock, salt);
  }

  private String calculatePinBlock(String secondFactor, String code) {
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
      String pinBlock = Hex.encodeHexString(xorResult);
      performanceLog(startTime, GENERATE_PIN_BLOCK);

      return pinBlock;
    } catch (DecoderException ex) {
      throw new PaymentInstrumentException(500, "Something went wrong while creating pinBlock");
    }
  }

  // Hashing pinBlock with algorithm SHA-256
  private String createSHA256Digest(String pinBlock, String salt) {
    long startTime = System.currentTimeMillis();
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      md.update(salt.getBytes(StandardCharsets.UTF_8));
      byte[] hash = md.digest(pinBlock.getBytes(StandardCharsets.UTF_8));

      String hashedPinBlock = Base64.getEncoder().encodeToString(hash);

      log.debug("[{}] pinBlock hashing done successfully", HASH_PIN_BLOCK);
      performanceLog(startTime, HASH_PIN_BLOCK);
      return hashedPinBlock;
    } catch (NoSuchAlgorithmException e) {
      throw new PaymentInstrumentException(403, "Something went wrong creating SHA256 digest");
    }
  }

  private void performanceLog(long startTime, String service){
    log.info(
        "[PERFORMANCE_LOG] [{}] Time occurred to perform business logic: {} ms",
        service,
        System.currentTimeMillis() - startTime);
  }
}
