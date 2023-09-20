package it.gov.pagopa.payment.instrument.service;

import feign.FeignException;
import it.gov.pagopa.payment.instrument.connector.WalletRestConnector;
import it.gov.pagopa.payment.instrument.dto.GenerateCodeDTO;
import it.gov.pagopa.payment.instrument.dto.GeneratedCodeDTO;
import it.gov.pagopa.payment.instrument.exception.PaymentInstrumentException;
import it.gov.pagopa.payment.instrument.repository.PaymentInstrumentCodeRepository;
import it.gov.pagopa.payment.instrument.utils.AuditUtilities;
import java.time.LocalDateTime;
import java.util.Random;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PaymentInstrumentCodeServiceImpl implements PaymentInstrumentCodeService {

  public static final String GENERATED_CODE = "GENERATED_CODE";
  public static final String ENROLL_CODE_AFTER_CODE_GENERATED = "ENROLL_CODE_AFTER_CODE_GENERATED";
  private final PaymentInstrumentCodeRepository paymentInstrumentCodeRepository;
  private final WalletRestConnector walletRestConnector;
  private final Random random;
  private final AuditUtilities auditUtilities;

  public PaymentInstrumentCodeServiceImpl(
      PaymentInstrumentCodeRepository paymentInstrumentCodeRepository,
      WalletRestConnector walletRestConnector, AuditUtilities auditUtilities) {
    this.paymentInstrumentCodeRepository = paymentInstrumentCodeRepository;
    this.walletRestConnector = walletRestConnector;
    this.auditUtilities = auditUtilities;
    this.random = new Random();
  }

  @Override
  public GeneratedCodeDTO generateCode(String userId, GenerateCodeDTO body) {
    long startTime = System.currentTimeMillis();

    StringBuilder code = buildCode();
    log.info("[{}] Code generated successfully on userId: {}", GENERATED_CODE, userId);

    paymentInstrumentCodeRepository.updateCode(userId, code.toString(), LocalDateTime.now());
    performanceLog(startTime, GENERATED_CODE, userId, body.getInitiativeId());
    auditUtilities.logGeneratedCode(userId, LocalDateTime.now());

    if (!body.getInitiativeId().isBlank()) {
      log.info("[{}] Code generated successfully, starting code enrollment on userId: {} and initiativeId: {}",
          ENROLL_CODE_AFTER_CODE_GENERATED, userId, body.getInitiativeId());
      try {
        walletRestConnector.enrollInstrumentCode(body.getInitiativeId(), userId);
        auditUtilities.logEnrollCodeAfterGeneratedCode(userId, body.getInitiativeId(), LocalDateTime.now());
      } catch (FeignException e) {
        log.info("[{}] Code enrollment on userId: {} and initiativeId: {} failed",
            ENROLL_CODE_AFTER_CODE_GENERATED, userId, body.getInitiativeId());
        throw new PaymentInstrumentException(500, "An error occurred while enrolling code");
      }
    }

    performanceLog(startTime, ENROLL_CODE_AFTER_CODE_GENERATED, userId, body.getInitiativeId());
    return new GeneratedCodeDTO(code.toString());
  }

  @NotNull
  private StringBuilder buildCode() {
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
