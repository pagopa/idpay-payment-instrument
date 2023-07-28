package it.gov.pagopa.payment.instrument.service;

import it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants;
import it.gov.pagopa.payment.instrument.dto.InstrumentFromDiscountDTO;
import it.gov.pagopa.payment.instrument.dto.RuleEngineRequestDTO;
import it.gov.pagopa.payment.instrument.dto.mapper.InstrumentFromDiscountDTO2PaymentInstrumentMapper;
import it.gov.pagopa.payment.instrument.dto.mapper.MessageMapper;
import it.gov.pagopa.payment.instrument.dto.pm.PaymentMethodInfoList;
import it.gov.pagopa.payment.instrument.event.producer.ErrorProducer;
import it.gov.pagopa.payment.instrument.event.producer.RuleEngineProducer;
import it.gov.pagopa.payment.instrument.model.PaymentInstrument;
import it.gov.pagopa.payment.instrument.repository.PaymentInstrumentRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PaymentInstrumentDiscountServiceImpl implements
    PaymentInstrumentDiscountService {

  private final InstrumentFromDiscountDTO2PaymentInstrumentMapper instrumentFromDiscountDTO2PaymentInstrumentMapper;
  private final PaymentInstrumentRepository paymentInstrumentRepository;
  private final MessageMapper messageMapper;
  private final String ruleEngineServer;
  private final String ruleEngineTopic;
  private final ErrorProducer errorProducer;
  private final RuleEngineProducer ruleEngineProducer;

  public PaymentInstrumentDiscountServiceImpl(
      InstrumentFromDiscountDTO2PaymentInstrumentMapper instrumentFromDiscountDTO2PaymentInstrumentMapper,
      PaymentInstrumentRepository paymentInstrumentRepository, MessageMapper messageMapper,
      @Value("${spring.cloud.stream.binders.kafka-re.environment.spring.cloud.stream.kafka.binder.brokers}") String ruleEngineServer,
      @Value("${spring.cloud.stream.bindings.paymentInstrumentQueue-out-0.destination}") String ruleEngineTopic,
      ErrorProducer errorProducer, RuleEngineProducer ruleEngineProducer) {
    this.instrumentFromDiscountDTO2PaymentInstrumentMapper = instrumentFromDiscountDTO2PaymentInstrumentMapper;
    this.paymentInstrumentRepository = paymentInstrumentRepository;
    this.messageMapper = messageMapper;
    this.ruleEngineServer = ruleEngineServer;
    this.ruleEngineTopic = ruleEngineTopic;
    this.errorProducer = errorProducer;
    this.ruleEngineProducer = ruleEngineProducer;
  }

  @Override
  public void enrollDiscountInitiative(InstrumentFromDiscountDTO body) {
    long startTime = System.currentTimeMillis();
    PaymentInstrument paymentInstrument = instrumentFromDiscountDTO2PaymentInstrumentMapper.apply(
        body);
    PaymentMethodInfoList info = new PaymentMethodInfoList();
    info.setHpan(paymentInstrument.getHpan());

    sendToRuleEngine(body.getUserId(), body.getInitiativeId(), List.of(info));

    paymentInstrumentRepository.save(paymentInstrument);
    performanceLog(startTime, "ENROLL_FROM_DISCOUNT_INITIATIVE");
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

  private void sendToRuleEngine(String userId, String initiativeId,
      List<PaymentMethodInfoList>
          paymentMethodInfoList) {

    RuleEngineRequestDTO ruleEngineRequestDTO = RuleEngineRequestDTO.builder()
        .userId(userId)
        .initiativeId(initiativeId)
        .infoList(paymentMethodInfoList)
        .channel(PaymentInstrumentConstants.IDPAY_PAYMENT)
        .operationType(PaymentInstrumentConstants.OPERATION_ADD)
        .operationDate(LocalDateTime.now())
        .build();

    log.info("[PaymentInstrumentDiscountService] Sending message to Rule Engine.");

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
