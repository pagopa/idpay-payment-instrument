package it.gov.pagopa.payment.instrument.service;

import it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants;
import it.gov.pagopa.payment.instrument.dto.BaseEnrollmentBodyDTO;
import it.gov.pagopa.payment.instrument.dto.InstrumentFromDiscountDTO;
import it.gov.pagopa.payment.instrument.dto.RuleEngineRequestDTO;
import it.gov.pagopa.payment.instrument.dto.mapper.BaseEnrollmentBodyDTO2PaymentInstrument;
import it.gov.pagopa.payment.instrument.dto.mapper.InstrumentFromDiscountDTO2PaymentInstrumentMapper;
import it.gov.pagopa.payment.instrument.dto.mapper.MessageMapper;
import it.gov.pagopa.payment.instrument.dto.pm.PaymentMethodInfoList;
import it.gov.pagopa.payment.instrument.event.producer.ErrorProducer;
import it.gov.pagopa.payment.instrument.event.producer.RuleEngineProducer;
import it.gov.pagopa.payment.instrument.exception.custom.IDPayCodeNotFoundException;
import it.gov.pagopa.payment.instrument.model.PaymentInstrument;
import it.gov.pagopa.payment.instrument.repository.PaymentInstrumentRepository;
import it.gov.pagopa.payment.instrument.service.idpaycode.PaymentInstrumentCodeService;
import it.gov.pagopa.payment.instrument.utils.AuditUtilities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import static it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants.ExceptionMessage.ERROR_IDPAYCODE_NOT_FOUND_MSG;

@Slf4j
@Service
public class PaymentInstrumentDiscountServiceImpl implements
    PaymentInstrumentDiscountService {

  private static final String FLOW_ENROLL_FROM_DISCOUNT_INITIATIVE = "ENROLL_FROM_DISCOUNT_INITIATIVE";
  private static final String FLOW_ENROLL_INSTRUMENT_CODE = "ENROLL_INSTRUMENT_CODE";

  private final InstrumentFromDiscountDTO2PaymentInstrumentMapper instrumentFromDiscountDTO2PaymentInstrumentMapper;
  private final BaseEnrollmentBodyDTO2PaymentInstrument baseEnrollmentBodyDTO2PaymentInstrument;
  private final PaymentInstrumentRepository paymentInstrumentRepository;
  private final MessageMapper messageMapper;
  private final String ruleEngineServer;
  private final String ruleEngineTopic;
  private final ErrorProducer errorProducer;
  private final RuleEngineProducer ruleEngineProducer;
  private final AuditUtilities auditUtilities;

  private final PaymentInstrumentCodeService paymentInstrumentCodeService;

  @SuppressWarnings("squid:S00107") // suppressing too many parameters alert
  public PaymentInstrumentDiscountServiceImpl(
          InstrumentFromDiscountDTO2PaymentInstrumentMapper instrumentFromDiscountDTO2PaymentInstrumentMapper,
          BaseEnrollmentBodyDTO2PaymentInstrument baseEnrollmentBodyDTO2PaymentInstrument,
          PaymentInstrumentRepository paymentInstrumentRepository,
          MessageMapper messageMapper,
          @Value("${spring.cloud.stream.binders.kafka-re.environment.spring.cloud.stream.kafka.binder.brokers}") String ruleEngineServer,
          @Value("${spring.cloud.stream.bindings.paymentInstrumentQueue-out-0.destination}") String ruleEngineTopic,
          ErrorProducer errorProducer,
          RuleEngineProducer ruleEngineProducer,
          AuditUtilities auditUtilities, PaymentInstrumentCodeService paymentInstrumentCodeService) {
    this.instrumentFromDiscountDTO2PaymentInstrumentMapper = instrumentFromDiscountDTO2PaymentInstrumentMapper;
    this.baseEnrollmentBodyDTO2PaymentInstrument = baseEnrollmentBodyDTO2PaymentInstrument;
    this.paymentInstrumentRepository = paymentInstrumentRepository;
    this.messageMapper = messageMapper;
    this.ruleEngineServer = ruleEngineServer;
    this.ruleEngineTopic = ruleEngineTopic;
    this.errorProducer = errorProducer;
    this.ruleEngineProducer = ruleEngineProducer;
    this.auditUtilities = auditUtilities;
    this.paymentInstrumentCodeService = paymentInstrumentCodeService;
  }

  @Override
  public void enrollDiscountInitiative(InstrumentFromDiscountDTO body) {
    long startTime = System.currentTimeMillis();
    PaymentInstrument paymentInstrument = instrumentFromDiscountDTO2PaymentInstrumentMapper.apply(
        body);

    notifyRuleEngineAndSavePaymentInstrument(paymentInstrument);
    performanceLog(startTime, FLOW_ENROLL_FROM_DISCOUNT_INITIATIVE);
  }

  @Override
  public void enrollInstrumentCode(BaseEnrollmentBodyDTO body) {
    long startTime = System.currentTimeMillis();
    log.info("[IDPAY_CODE_STATUS] Checking idpayCode status");

    boolean idayCodeEnabled = paymentInstrumentCodeService.codeStatus(body.getUserId());

    log.info("[IDPAY_CODE_STATUS] The userId {} has code with status {}", body.getUserId(), idayCodeEnabled);

    if(!idayCodeEnabled){
      performanceLog(startTime, FLOW_ENROLL_INSTRUMENT_CODE);
      throw new IDPayCodeNotFoundException(ERROR_IDPAYCODE_NOT_FOUND_MSG);
    }

    log.info("[ENROLL_INSTRUMENT_CODE] Processing IDPayCode enrollment request of the user {} for the initiative {}",
        body.getUserId(), body.getInitiativeId());

    PaymentInstrument paymentInstrument = baseEnrollmentBodyDTO2PaymentInstrument.apply(body,
            PaymentInstrumentConstants.IDPAY_CODE_FAKE_INSTRUMENT_PREFIX.formatted(body.getUserId()));

    List<PaymentInstrument> instrumentList = paymentInstrumentRepository.findByHpan(paymentInstrument.getHpan());

    boolean isInstrumentAlreadyActiveOrPending = instrumentList.stream()
        .anyMatch(pi -> pi.getInitiativeId().equals(body.getInitiativeId())
            && !pi.getStatus().equals(PaymentInstrumentConstants.STATUS_INACTIVE));

    if (isInstrumentAlreadyActiveOrPending) {
      log.info("[ENROLL_INSTRUMENT_CODE] The Payment Instrument is already active, or there is a pending request on it.");
      auditUtilities.logEnrollInstrFromIssuerKO("already active or in pending", paymentInstrument.getHpan(), body.getChannel());
      performanceLog(startTime, FLOW_ENROLL_INSTRUMENT_CODE);
      return;
    }

    notifyRuleEngineAndSavePaymentInstrument(paymentInstrument);
    auditUtilities.logEnrollInstrumentCodeComplete(body.getUserId(), body.getInitiativeId(), body.getChannel(), body.getInstrumentType());
    performanceLog(startTime, FLOW_ENROLL_INSTRUMENT_CODE);

  }

  private void notifyRuleEngineAndSavePaymentInstrument(PaymentInstrument paymentInstrument) {
    PaymentMethodInfoList info = new PaymentMethodInfoList();
    info.setHpan(paymentInstrument.getHpan());

    sendToRuleEngine(paymentInstrument.getUserId(), paymentInstrument.getInitiativeId(), List.of(info), paymentInstrument.getChannel());

    paymentInstrumentRepository.save(paymentInstrument);
  }

  private void sendToErrorQueue(Exception e, MessageBuilder<?> errorMessage, String server,
      String topic) {

    errorMessage
        .setHeader(PaymentInstrumentConstants.ERROR_MSG_HEADER_SRC_TYPE,
            PaymentInstrumentConstants.KAFKA)
        .setHeader(PaymentInstrumentConstants.ERROR_MSG_HEADER_SRC_SERVER,
            server)
        .setHeader(PaymentInstrumentConstants.ERROR_MSG_HEADER_SRC_TOPIC,
            topic)
        .setHeader(PaymentInstrumentConstants.ERROR_MSG_HEADER_DESCRIPTION,
            PaymentInstrumentConstants.ERROR_QUEUE)
        .setHeader(PaymentInstrumentConstants.ERROR_MSG_HEADER_RETRYABLE, true)
        .setHeader(PaymentInstrumentConstants.ERROR_MSG_HEADER_STACKTRACE, e.getStackTrace())
        .setHeader(PaymentInstrumentConstants.ERROR_MSG_HEADER_CLASS, e.getClass())
        .setHeader(PaymentInstrumentConstants.ERROR_MSG_HEADER_MESSAGE, e.getMessage());
    errorProducer.sendEvent(errorMessage.build());
  }

  private void sendToRuleEngine(String userId,
                                String initiativeId,
                                List<PaymentMethodInfoList> paymentMethodInfoList,
                                String channel) {

    RuleEngineRequestDTO ruleEngineRequestDTO = RuleEngineRequestDTO.builder()
        .userId(userId)
        .initiativeId(initiativeId)
        .infoList(paymentMethodInfoList)
        .channel(channel)
        .operationType(PaymentInstrumentConstants.OPERATION_ADD)
        .operationDate(LocalDateTime.now())
        .build();

    log.info("[{}] Sending message to Rule Engine.", FLOW_ENROLL_INSTRUMENT_CODE);

    try {
      ruleEngineProducer.sendInstruments(messageMapper.apply(ruleEngineRequestDTO));
    } catch (Exception e) {
      final MessageBuilder<?> errorMessage = MessageBuilder.withPayload(
          messageMapper.apply(ruleEngineRequestDTO));
      sendToErrorQueue(e, errorMessage, ruleEngineServer, ruleEngineTopic);
    }
  }

  private void performanceLog(long startTime, String service) {
    log.info(
        "[PERFORMANCE_LOG] [{}] Time occurred to perform business logic: {} ms",
        service,
        System.currentTimeMillis() - startTime);
  }
}
