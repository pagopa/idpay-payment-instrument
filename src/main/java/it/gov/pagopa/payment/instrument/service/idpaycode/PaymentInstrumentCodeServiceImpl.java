package it.gov.pagopa.payment.instrument.service.idpaycode;

import feign.FeignException;
import it.gov.pagopa.payment.instrument.connector.WalletRestConnector;
import it.gov.pagopa.payment.instrument.dto.CheckEnrollmentDTO;
import it.gov.pagopa.payment.instrument.dto.GenerateCodeRespDTO;
import it.gov.pagopa.payment.instrument.exception.PaymentInstrumentException;
import it.gov.pagopa.payment.instrument.model.PaymentInstrumentCode;
import it.gov.pagopa.payment.instrument.repository.PaymentInstrumentCodeRepository;
import it.gov.pagopa.payment.instrument.utils.AuditUtilities;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
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

  public PaymentInstrumentCodeServiceImpl(
      PaymentInstrumentCodeRepository paymentInstrumentCodeRepository,
      WalletRestConnector walletRestConnector, AuditUtilities auditUtilities,
      EncryptCodeService encryptCodeService) {
    this.paymentInstrumentCodeRepository = paymentInstrumentCodeRepository;
    this.walletRestConnector = walletRestConnector;
    this.auditUtilities = auditUtilities;
    this.encryptCodeService = encryptCodeService;
    this.random = new SecureRandom();
  }

  @Override
  public GenerateCodeRespDTO generateCode(String userId, String initiativeId) {
    long startTime = System.currentTimeMillis();

    // generate clear code
    String clearCode = buildCode();

    // encrypt clear code
    String idpayCode = encryptCodeService.encryptIdpayCode(clearCode);
    log.info("[{}] Code generated successfully on userId: {}", GENERATED_CODE, userId);

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
        switch (e.status()) {
          case 429 -> throw new PaymentInstrumentException(HttpStatus.TOO_MANY_REQUESTS.value(),
              "Too many request on the ms wallet");
          case 404 -> throw new PaymentInstrumentException(HttpStatus.NOT_FOUND.value(),
              "Resource not found while enrolling idpayCode on ms wallet");
          default -> throw new PaymentInstrumentException(HttpStatus.INTERNAL_SERVER_ERROR.value(),
              "An error occurred in the microservice wallet");
        }
      }
    }

    paymentInstrumentCodeRepository.updateCode(userId, idpayCode, LocalDateTime.now());
    performanceLog(startTime, GENERATED_CODE, userId, initiativeId);
    auditUtilities.logGeneratedCode(userId, LocalDateTime.now());

    return new GenerateCodeRespDTO(idpayCode);
  }

  @Override
  public CheckEnrollmentDTO codeStatus(String userId) {

    PaymentInstrumentCode paymentInstrumentCode = paymentInstrumentCodeRepository.findByUserId(
        userId).orElse(null);

    boolean idPayCodeEnabled = (paymentInstrumentCode != null) && (paymentInstrumentCode.getIdpayCode() != null);

    return new CheckEnrollmentDTO(idPayCodeEnabled);
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
