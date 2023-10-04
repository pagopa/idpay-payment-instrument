package it.gov.pagopa.payment.instrument.service.idpaycode;

import feign.FeignException;
import it.gov.pagopa.payment.instrument.connector.WalletRestConnector;
import it.gov.pagopa.payment.instrument.dto.GenerateCodeRespDTO;
import it.gov.pagopa.payment.instrument.exception.PaymentInstrumentException;
import it.gov.pagopa.payment.instrument.model.PaymentInstrumentCode;
import it.gov.pagopa.payment.instrument.repository.PaymentInstrumentCodeRepository;
import it.gov.pagopa.payment.instrument.utils.AuditUtilities;
import it.gov.pagopa.payment.instrument.utils.Utilities;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PaymentInstrumentCodeServiceImpl implements PaymentInstrumentCodeService {

  public static final String GENERATED_CODE = "GENERATED_CODE";
  public static final String ENROLL_CODE_AFTER_CODE_GENERATED = "ENROLL_CODE_AFTER_CODE_GENERATED";
  private final PaymentInstrumentCodeRepository paymentInstrumentCodeRepository;
  private final WalletRestConnector walletRestConnector;
  private final SecureRandom random;
  private final AuditUtilities auditUtilities;
  private final EncryptCodeService encryptCodeService;
  private final Utilities utilities;

  public PaymentInstrumentCodeServiceImpl(
      PaymentInstrumentCodeRepository paymentInstrumentCodeRepository,
      WalletRestConnector walletRestConnector, AuditUtilities auditUtilities,
      EncryptCodeService encryptCodeService, Utilities utilities) {
    this.paymentInstrumentCodeRepository = paymentInstrumentCodeRepository;
    this.walletRestConnector = walletRestConnector;
    this.auditUtilities = auditUtilities;
    this.encryptCodeService = encryptCodeService;
    this.utilities = utilities;
    this.random = new SecureRandom();
  }

  @Override
  public GenerateCodeRespDTO generateCode(String userId, String initiativeId) {
    long startTime = System.currentTimeMillis();

    // generate clear code
    String clearCode = buildCode();

    // generate Salt
    String salt = generateRandomEvenCharHexString(16);

    // generate secondFactor and add left pad
    String secondFactor = generateRandomEvenCharHexString(12);
    String secondFactorWithLeftPad = StringUtils.leftPad(secondFactor, 16, '0');

    // encrypt clear code
    String hashedPinBlock = encryptCodeService.buildHashedPinBlock(clearCode, secondFactorWithLeftPad, salt);
    log.info("[{}] Code generated successfully on userId: {}", GENERATED_CODE, userId);

    // save encrypted code
    paymentInstrumentCodeRepository.updateCode(userId, hashedPinBlock, salt, secondFactorWithLeftPad, LocalDateTime.now());
    performanceLog(startTime, GENERATED_CODE, userId, initiativeId);
    auditUtilities.logGeneratedCode(userId, LocalDateTime.now());

    // enroll code if an initiativeId was provided
    if (StringUtils.isNotBlank(initiativeId)) {
      log.info("[{}] Code generated successfully, starting code enrollment on userId: {} and initiativeId: {}",
          ENROLL_CODE_AFTER_CODE_GENERATED, userId, initiativeId);
      try {
        walletRestConnector.enrollInstrumentCode(initiativeId, userId);
        auditUtilities.logEnrollCodeAfterGeneratedCode(userId, initiativeId, LocalDateTime.now());
        performanceLog(startTime, ENROLL_CODE_AFTER_CODE_GENERATED, userId, initiativeId);
      } catch (FeignException e) {
        log.info("[{}] Code enrollment on userId: {} and initiativeId: {} failed",
            ENROLL_CODE_AFTER_CODE_GENERATED, userId, initiativeId);

        // delete code if enrollment have failed
        paymentInstrumentCodeRepository.deleteInstrument(userId);

        switch (e.status()) {
          case 429 -> throw new PaymentInstrumentException(HttpStatus.TOO_MANY_REQUESTS.value(), utilities.exceptionConverter(e));
          case 400 -> throw new PaymentInstrumentException(HttpStatus.BAD_REQUEST.value(), utilities.exceptionConverter(e));
          case 404 -> throw new PaymentInstrumentException(HttpStatus.NOT_FOUND.value(), utilities.exceptionConverter(e));
          default -> throw new PaymentInstrumentException(HttpStatus.INTERNAL_SERVER_ERROR.value(),
              "An error occurred in the microservice wallet");
        }
      }
    }

    return new GenerateCodeRespDTO(clearCode);
  }

  @Override
  public boolean codeStatus(String userId) {
    long startTime = System.currentTimeMillis();

    PaymentInstrumentCode paymentInstrumentCode = paymentInstrumentCodeRepository.findByUserId(
        userId).orElse(null);

    boolean idpayCodeEnabled = (paymentInstrumentCode != null) && (paymentInstrumentCode.getIdpayCode() != null);

    log.info("[IDPAY_CODE_STATUS] The userId {} has code with status {}", userId, idpayCodeEnabled);
    performanceLog(startTime, "IDPAY_CODE_STATUS", userId, null);
    return idpayCodeEnabled;
  }

  // Generate Salt or Second factor only with length even
  private String generateRandomEvenCharHexString(int length) {
    byte[] salt = new byte[length/2];
    random.nextBytes(salt);
    return Hex.encodeHexString(salt);
  }

  @NotNull
  private String buildCode() {
    StringBuilder code = new StringBuilder();
    int lastDigit = -2;
    int maxDigitRepetition = 1;

    int[] digitCounters = new int[10];

    for (int i = 0; i < 5; i++) {
      int newDigit;
      while (true) {
        newDigit = random.nextInt(10);

        if (newDigit != lastDigit + 1 && newDigit != lastDigit - 1
            && digitCounters[newDigit] < maxDigitRepetition) {
          lastDigit = newDigit;
          digitCounters[newDigit]++;
          code.append(newDigit);
          break;
        }
      }
    }
    return code.toString();
  }

  private void performanceLog(long startTime, String service, String userId, String initiativeId){
    log.info(
        "[PERFORMANCE_LOG] [{}] Time occurred to perform business logic: {} ms on userId: {} and initiativeId: {}",
        service,
        System.currentTimeMillis() - startTime,
        userId,
        initiativeId);
  }
}
