package it.gov.pagopa.payment.instrument.service.idpaycode;

import feign.FeignException;
import it.gov.pagopa.common.web.dto.ErrorDTO;
import it.gov.pagopa.payment.instrument.connector.WalletRestConnector;
import it.gov.pagopa.payment.instrument.dto.EncryptedDataBlock;
import it.gov.pagopa.payment.instrument.dto.GenerateCodeRespDTO;
import it.gov.pagopa.payment.instrument.dto.PinBlockDTO;
import it.gov.pagopa.payment.instrument.exception.custom.*;
import it.gov.pagopa.payment.instrument.model.PaymentInstrumentCode;
import it.gov.pagopa.payment.instrument.repository.PaymentInstrumentCodeRepository;
import it.gov.pagopa.payment.instrument.utils.AuditUtilities;
import it.gov.pagopa.payment.instrument.utils.Utilities;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

import static it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants.ExceptionMessage.*;

@Slf4j
@Service
public class PaymentInstrumentCodeServiceImpl implements PaymentInstrumentCodeService {

  public static final String GENERATED_CODE = "GENERATED_CODE";
  public static final String ENROLL_CODE_AFTER_CODE_GENERATED = "ENROLL_CODE_AFTER_CODE_GENERATED";
  public static final String  WALLET_TOO_MANY_REQUESTS = "WALLET_TOO_MANY_REQUESTS";
  public static final String WALLET_USER_NOT_ONBOARDED = "WALLET_USER_NOT_ONBOARDED";
  public static final String WALLET_ENROLL_INSTRUMENT_NOT_ALLOW_FOR_REFUND_INITIATIVE = "WALLET_ENROLL_INSTRUMENT_NOT_ALLOW_FOR_REFUND_INITIATIVE";
  public static final String WALLET_USER_UNSUBSCRIBED = "WALLET_USER_UNSUBSCRIBED";
  public static final String WALLET_INITIATIVE_ENDED = "WALLET_INITIATIVE_ENDED";
  private final PaymentInstrumentCodeRepository paymentInstrumentCodeRepository;
  private final WalletRestConnector walletRestConnector;
  private final SecureRandom random;
  private final AuditUtilities auditUtilities;
  private final IdpayCodeEncryptionService idpayCodeEncryptionService;
  private final Utilities utilities;

  public PaymentInstrumentCodeServiceImpl(
      PaymentInstrumentCodeRepository paymentInstrumentCodeRepository,
      WalletRestConnector walletRestConnector, AuditUtilities auditUtilities,
      IdpayCodeEncryptionService idpayCodeEncryptionService, Utilities utilities) {
    this.paymentInstrumentCodeRepository = paymentInstrumentCodeRepository;
    this.walletRestConnector = walletRestConnector;
    this.auditUtilities = auditUtilities;
    this.idpayCodeEncryptionService = idpayCodeEncryptionService;
    this.utilities = utilities;
    this.random = new SecureRandom();
  }

  @Override
  public GenerateCodeRespDTO generateCode(String userId, String initiativeId) {
    long startTime = System.currentTimeMillis();

    // generate plain code
    String plainCode = buildCode();

    // generate Salt
    String salt = generateRandomEvenCharHexString(16);

    // generate secondFactor and add left pad
    String secondFactor = generateRandomEvenCharHexString(12);
    String secondFactorWithLeftPad = StringUtils.leftPad(secondFactor, 16, '0');

    // hash and encrypt plain code
    String hashedDataBlock = idpayCodeEncryptionService.buildHashedDataBlock(plainCode, secondFactorWithLeftPad, salt);
    EncryptedDataBlock encryptedDataBlock = idpayCodeEncryptionService.encryptSHADataBlock(hashedDataBlock);
    log.info("[{}] Code generated successfully on userId: {}", GENERATED_CODE, userId);

    // save encrypted code
    paymentInstrumentCodeRepository.updateCode(userId, encryptedDataBlock.getEncryptedDataBlock(), salt,
        secondFactorWithLeftPad, encryptedDataBlock.getKeyId(), LocalDateTime.now());
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
        paymentInstrumentCodeRepository.deleteById(userId);

        ErrorDTO errorDTO = utilities.exceptionErrorDTOConverter(e);
        switch (errorDTO.getCode()) {
          case WALLET_TOO_MANY_REQUESTS -> {
            log.error("[ENROLL_INSTRUMENT_CODE] Too many requests on the ms wallet");
            throw new TooManyRequestsException(ERROR_TOO_MANY_REQUESTS_WALLET_MSG);}
          case WALLET_USER_NOT_ONBOARDED -> {
            log.error("[ENROLL_INSTRUMENT_CODE] The user {} is not onboarded on initiative {}", userId, initiativeId);
            throw new UserNotOnboardedException(String.format(ERROR_USER_NOT_ONBOARDED_MSG, initiativeId));}
          case WALLET_ENROLL_INSTRUMENT_NOT_ALLOW_FOR_REFUND_INITIATIVE -> {
            log.error("[ENROLL_INSTRUMENT_CODE] It is not possible to enroll a idpayCode for a refund type initiative {}", initiativeId);
            throw new EnrollmentNotAllowedException(String.format(ERROR_ENROLL_NOT_ALLOWED_FOR_REFUND_INITIATIVE_MSG, initiativeId));
          }
          case WALLET_USER_UNSUBSCRIBED -> {
            log.error("[ENROLL_INSTRUMENT_CODE] The user {} has unsubscribed from initiative {}", userId, initiativeId);
            throw new UserUnsubscribedException(String.format(ERROR_USER_UNSUBSCRIBED_MSG, initiativeId));
          }
          case WALLET_INITIATIVE_ENDED -> {
            log.error("[ENROLL_INSTRUMENT_CODE] The operation is not allowed because the initiative {} has already ended", initiativeId);
            throw new InitiativeInvalidException(String.format(ERROR_INITIATIVE_ENDED_MSG, initiativeId));
          }
          default -> {
            log.error("[ENROLL_INSTRUMENT_CODE] An error occurred while invoking the wallet microservice");
            throw new WalletInvocationException(ERROR_INVOCATION_WALLET_MSG);
          }
        }
      }
    }

    return new GenerateCodeRespDTO(plainCode);
  }

  @Override
  public boolean codeStatus(String userId) {
    long startTime = System.currentTimeMillis();

    PaymentInstrumentCode paymentInstrumentCode = findById(userId);

    boolean idpayCodeEnabled = (paymentInstrumentCode != null) && (paymentInstrumentCode.getIdpayCode() != null);

    log.info("[IDPAY_CODE_STATUS] The userId {} has code with status {}", userId, idpayCodeEnabled);
    performanceLog(startTime, "IDPAY_CODE_STATUS", userId, null);
    return idpayCodeEnabled;
  }

  /** Verify if pin block is correct */
  @Override
  public boolean verifyPinBlock(String userId, PinBlockDTO pinBlockDTO) {
    long startTime = System.currentTimeMillis();

    PaymentInstrumentCode paymentInstrumentCode = findById(userId);
    if (paymentInstrumentCode == null){
      log.info("[VERIFY_PINBLOCK] idpayCode is not found for the user {}", userId);
      throw new IDPayCodeNotFoundException(ERROR_IDPAYCODE_NOT_FOUND_MSG);
    }

    String inputPlainIdpayCode = idpayCodeEncryptionService.hashSHADecryptedDataBlock(userId, pinBlockDTO,
        paymentInstrumentCode.getSalt());

    String expectedPlainIdpayCode = idpayCodeEncryptionService.decryptIdpayCode(
        new EncryptedDataBlock(paymentInstrumentCode.getIdpayCode(), paymentInstrumentCode.getKeyId()));

    performanceLog(startTime, "VERIFY_IDPAY_CODE", userId, null);
    return inputPlainIdpayCode.equals(expectedPlainIdpayCode);
  }

  @Override
  public String getSecondFactor(String userId) {
    long startTime = System.currentTimeMillis();

    PaymentInstrumentCode paymentInstrumentCode = findById(userId);

    if (paymentInstrumentCode==null){
      log.info("[GET_SECOND_FACTOR] idpayCode is not found for the user {}", userId);
      throw new IDPayCodeNotFoundException(ERROR_IDPAYCODE_NOT_FOUND_MSG);
    }
    String secondFactor = paymentInstrumentCode.getSecondFactor();
    performanceLog(startTime, "IDPAY_CODE_SECOND_FACTOR", userId, null);
    return secondFactor;
  }

  /** Generate Salt or Second factor only with length even */
  private String generateRandomEvenCharHexString(int length) {
    byte[] salt = new byte[length/2];
    random.nextBytes(salt);
    return Hex.encodeHexString(salt);
  }

  /** Generate plain idpay code */
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

  @Nullable
  private PaymentInstrumentCode findById(String userId) {
    return paymentInstrumentCodeRepository.findById(userId).orElse(null);
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
