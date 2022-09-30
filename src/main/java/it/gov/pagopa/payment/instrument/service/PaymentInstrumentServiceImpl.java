package it.gov.pagopa.payment.instrument.service;

import it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants;
import it.gov.pagopa.payment.instrument.dto.HpanDTO;
import it.gov.pagopa.payment.instrument.dto.HpanGetDTO;
import it.gov.pagopa.payment.instrument.dto.RTDOperationDTO;
import it.gov.pagopa.payment.instrument.dto.RuleEngineQueueDTO;
import it.gov.pagopa.payment.instrument.dto.mapper.MessageMapper;
import it.gov.pagopa.payment.instrument.event.ErrorProducer;
import it.gov.pagopa.payment.instrument.event.RTDProducer;
import it.gov.pagopa.payment.instrument.event.RuleEngineProducer;
import it.gov.pagopa.payment.instrument.exception.PaymentInstrumentException;
import it.gov.pagopa.payment.instrument.model.PaymentInstrument;
import it.gov.pagopa.payment.instrument.repository.PaymentInstrumentRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PaymentInstrumentServiceImpl implements PaymentInstrumentService {

  @Autowired
  private PaymentInstrumentRepository paymentInstrumentRepository;
  @Autowired
  RuleEngineProducer ruleEngineProducer;
  @Autowired
  RTDProducer rtdProducer;
  @Autowired
  MessageMapper messageMapper;
  @Autowired
  ErrorProducer errorProducer;

  private static final Logger LOG = LoggerFactory.getLogger(
      PaymentInstrumentServiceImpl.class);

  @Override
  public void enrollInstrument(String initiativeId, String userId, String hpan, String channel,
      LocalDateTime activationDate) {
    List<PaymentInstrument> instrumentList = paymentInstrumentRepository.findByHpanAndStatus(hpan,
        PaymentInstrumentConstants.STATUS_ACTIVE);
    List<String> hpanList = new ArrayList<>();
    hpanList.add(hpan);

    for (PaymentInstrument pi : instrumentList) {
      if (!pi.getUserId().equals(userId)) {
        throw new PaymentInstrumentException(HttpStatus.FORBIDDEN.value(),
            PaymentInstrumentConstants.ERROR_PAYMENT_INSTRUMENT_ALREADY_ACTIVE);
      } else if (pi.getInitiativeId().equals(initiativeId)) {
        return;
      }
    }

    PaymentInstrument newInstrument = new PaymentInstrument(initiativeId, userId, hpan,
        PaymentInstrumentConstants.STATUS_ACTIVE, channel, activationDate);
    paymentInstrumentRepository.save(newInstrument);
    try {
        sendToRuleEngine(newInstrument.getUserId(), newInstrument.getInitiativeId(), hpanList,
        PaymentInstrumentConstants.OPERATION_ADD);
    }catch(Exception e){
    paymentInstrumentRepository.delete(newInstrument);
    throw new PaymentInstrumentException(HttpStatus.BAD_REQUEST.value(), e.getMessage());
    }
    try {
      sendToRtd(hpanList, PaymentInstrumentConstants.OPERATION_ADD);
    }catch(Exception e){
      this.sendToQueueError(e,hpanList, PaymentInstrumentConstants.OPERATION_ADD);
    }
  }

  @Override
  public void deactivateAllInstrument(String initiativeId, String userId, String deactivationDate) {
    List<PaymentInstrument> paymentInstrumentList = paymentInstrumentRepository.findByInitiativeIdAndUserIdAndStatus(
        initiativeId, userId, PaymentInstrumentConstants.STATUS_ACTIVE);
    List<String> hpanList = new ArrayList<>();
    for (PaymentInstrument paymentInstrument : paymentInstrumentList) {
      paymentInstrument.setRequestDeactivationDate(LocalDateTime.parse(deactivationDate));
      paymentInstrument.setStatus(PaymentInstrumentConstants.STATUS_INACTIVE);
      hpanList.add(paymentInstrument.getHpan());
    }
    paymentInstrumentRepository.saveAll(paymentInstrumentList);
    try {
      sendToRuleEngine(userId, initiativeId, hpanList, PaymentInstrumentConstants.OPERATION_DELETE);
    } catch (Exception e) {
      this.rollbackInstruments(paymentInstrumentList);
      throw new PaymentInstrumentException(HttpStatus.BAD_REQUEST.value(), e.getMessage());
    }
      sendToRtd(hpanList, PaymentInstrumentConstants.OPERATION_DELETE);
  }

  @Override
  public void deactivateInstrument(String initiativeId, String userId, String hpan,
      LocalDateTime deactivationDate) {
    List<PaymentInstrument> instruments = paymentInstrumentRepository.findByInitiativeIdAndUserIdAndHpan(
        initiativeId, userId, hpan);
    if (instruments.isEmpty()) {
      throw new PaymentInstrumentException(HttpStatus.NOT_FOUND.value(),
          PaymentInstrumentConstants.ERROR_PAYMENT_INSTRUMENT_NOT_FOUND);
    }
    instruments.forEach(instrument ->
        checkAndDelete(instrument, deactivationDate)
    );
  }

  private void checkAndDelete(PaymentInstrument instrument, LocalDateTime deactivationDate) {
    if (instrument.getStatus().equals(PaymentInstrumentConstants.STATUS_INACTIVE)) {
      return;
    }
    instrument.setStatus(PaymentInstrumentConstants.STATUS_INACTIVE);
    instrument.setRequestDeactivationDate(deactivationDate);
    paymentInstrumentRepository.save(instrument);
    List<String> hpanList = Arrays.asList(instrument.getHpan());
    sendToRuleEngine(instrument.getUserId(), instrument.getInitiativeId(), hpanList,
        PaymentInstrumentConstants.OPERATION_DELETE);
    sendToRtd(hpanList, PaymentInstrumentConstants.OPERATION_DELETE);
  }

  private void sendToRuleEngine(String userId, String initiativeId, List<String> hpanList,
      String operation) {

    RuleEngineQueueDTO ruleEngineQueueDTO = RuleEngineQueueDTO.builder()
        .userId(userId)
        .initiativeId(initiativeId)
        .hpanList(hpanList)
        .operationType(operation)
        .operationDate(LocalDateTime.now())
        .build();

    log.info("[PaymentInstrumentService] Sending message to Rule Engine.");
    long start = System.currentTimeMillis();

    ruleEngineProducer.sendInstruments(messageMapper.apply(ruleEngineQueueDTO));

    long end = System.currentTimeMillis();
    log.info(
        "[PaymentInstrumentService] Sent message to Rule Engine after " + (end - start) + " ms.");
  }

  private void sendToRtd(List<String> hpanList, String operation) {

    List<String> toRtd = new ArrayList<>();

    for (String hpan : hpanList) {
      if (operation.equals(PaymentInstrumentConstants.OPERATION_ADD) ||
          paymentInstrumentRepository.countByHpanAndStatus(hpan,
              PaymentInstrumentConstants.STATUS_ACTIVE) == 0) {
        toRtd.add(hpan);
      }
    }

    if (!toRtd.isEmpty()) {
      RTDOperationDTO rtdOperationDTO =
          RTDOperationDTO.builder()
              .hpanList(toRtd)
              .operationType(operation)
              .application("IDPAY")
              .operationDate(LocalDateTime.now())
              .build();

      log.info("[PaymentInstrumentService - Operation: {}] Sending message to RTD.", operation);

      rtdProducer.sendInstrument(rtdOperationDTO);
    }
  }

  @Override
  public int countByInitiativeIdAndUserIdAndStatus(String initiativeId, String userId,
      String status) {
    return paymentInstrumentRepository.countByInitiativeIdAndUserIdAndStatus(initiativeId, userId,
        status);
  }

  @Override
  public HpanGetDTO gethpan(String initiativeId, String userId) {
    List<PaymentInstrument> paymentInstrument = paymentInstrumentRepository.findByInitiativeIdAndUserId(
        initiativeId, userId);

    if (paymentInstrument.isEmpty()) {
      throw new PaymentInstrumentException(HttpStatus.NOT_FOUND.value(),
          PaymentInstrumentConstants.ERROR_INITIATIVE_USER);
    }

    HpanGetDTO hpanGetDTO = new HpanGetDTO();
    List<HpanDTO> hpanDTOList = new ArrayList<>();

    for (PaymentInstrument paymentInstruments : paymentInstrument) {
      HpanDTO hpanDTO = new HpanDTO();
      hpanDTO.setHpan(paymentInstruments.getHpan());
      hpanDTO.setChannel(paymentInstruments.getChannel());
      hpanDTOList.add(hpanDTO);
    }
    hpanGetDTO.setHpanList(hpanDTOList);

    return hpanGetDTO;
  }

  @Override
  public void rollbackInstruments(List<PaymentInstrument> paymentInstrumentList) {
    for (PaymentInstrument instrument : paymentInstrumentList) {
      instrument.setStatus(PaymentInstrumentConstants.STATUS_ACTIVE);
      instrument.setRequestDeactivationDate(null);
    }
    paymentInstrumentRepository.saveAll(paymentInstrumentList);
    LOG.info("Instrument rollbacked: {}", paymentInstrumentList.size());
  }

  private void sendToQueueError(Exception e, List<String> hpanList, String operation){
    RTDOperationDTO rtdOperationDTO =
        RTDOperationDTO.builder()
            .hpanList(hpanList)
            .operationType(operation)
            .application("IDPAY")
            .operationDate(LocalDateTime.now())
            .build();

    final MessageBuilder<?> errorMessage = MessageBuilder.withPayload(rtdOperationDTO)
        .setHeader(PaymentInstrumentConstants.ERROR_MSG_HEADER_SRC_TYPE, PaymentInstrumentConstants.KAFKA)
        .setHeader(PaymentInstrumentConstants.ERROR_MSG_HEADER_SRC_SERVER, PaymentInstrumentConstants.BROKER_RTD)
        .setHeader(PaymentInstrumentConstants.ERROR_MSG_HEADER_SRC_TOPIC, PaymentInstrumentConstants.TOPIC_RTD)
        .setHeader(PaymentInstrumentConstants.ERROR_MSG_HEADER_DESCRIPTION, PaymentInstrumentConstants.ERROR_RTD)
        .setHeader(PaymentInstrumentConstants.ERROR_MSG_HEADER_RETRYABLE, true)
        .setHeader(PaymentInstrumentConstants.ERROR_MSG_HEADER_STACKTRACE, e.getStackTrace())
        .setHeader(PaymentInstrumentConstants.ERROR_MSG_HEADER_CLASS, e.getClass())
        .setHeader(PaymentInstrumentConstants.ERROR_MSG_HEADER_MESSAGE, e.getMessage());
    errorProducer.sendEvent(errorMessage.build());
  }
}