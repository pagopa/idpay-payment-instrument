package it.gov.pagopa.payment.instrument.service;

import feign.FeignException;
import it.gov.pagopa.payment.instrument.connector.WalletRestConnector;
import it.gov.pagopa.payment.instrument.dto.GenerateCodeReqDTO;
import it.gov.pagopa.payment.instrument.dto.GenerateCodeRespDTO;
import it.gov.pagopa.payment.instrument.exception.PaymentInstrumentException;
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

  public PaymentInstrumentCodeServiceImpl(
      PaymentInstrumentCodeRepository paymentInstrumentCodeRepository,
      WalletRestConnector walletRestConnector, AuditUtilities auditUtilities) {
    this.paymentInstrumentCodeRepository = paymentInstrumentCodeRepository;
    this.walletRestConnector = walletRestConnector;
    this.auditUtilities = auditUtilities;
    this.random = new SecureRandom();
  }

  @Override
  public GenerateCodeRespDTO generateCode(String userId, GenerateCodeReqDTO body) {
    long startTime = System.currentTimeMillis();

    String clearCode = buildCode();
    String idpayCode = encryptIdpayCode(clearCode);
    log.info("[{}] Code generated successfully on userId: {}", GENERATED_CODE, userId);

    if (StringUtils.isNotBlank(body.getInitiativeId())) {
      log.info("[{}] Code generated successfully, starting code enrollment on userId: {} and initiativeId: {}",
          ENROLL_CODE_AFTER_CODE_GENERATED, userId, body.getInitiativeId());
      try {
        walletRestConnector.enrollInstrumentCode(body.getInitiativeId(), userId);
        auditUtilities.logEnrollCodeAfterGeneratedCode(userId, body.getInitiativeId(), LocalDateTime.now());
        performanceLog(startTime, ENROLL_CODE_AFTER_CODE_GENERATED, userId, body.getInitiativeId());
      } catch (FeignException e) {
        log.info("[{}] Code enrollment on userId: {} and initiativeId: {} failed",
            ENROLL_CODE_AFTER_CODE_GENERATED, userId, body.getInitiativeId());
        switch (e.status()) {
          case 429 -> throw new PaymentInstrumentException(HttpStatus.TOO_MANY_REQUESTS.value(), "Too many request on the ms wallet");
          case 404 -> throw new PaymentInstrumentException(HttpStatus.NOT_FOUND.value(), "Resource not found while enrolling idpayCode on ms wallet");
          default -> throw new PaymentInstrumentException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "An error occurred in the microservice wallet");
        }
      }
    }

    paymentInstrumentCodeRepository.updateCode(userId, idpayCode, LocalDateTime.now());
    performanceLog(startTime, GENERATED_CODE, userId, body.getInitiativeId());
    auditUtilities.logGeneratedCode(userId, LocalDateTime.now());

    return new GenerateCodeRespDTO(idpayCode);
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

  private String encryptIdpayCode(String code){
    return code;
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
