package it.gov.pagopa.payment.instrument.service;

import it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants;
import it.gov.pagopa.payment.instrument.dto.HpanDTO;
import it.gov.pagopa.payment.instrument.dto.HpanGetDTO;
import it.gov.pagopa.payment.instrument.dto.RuleEngineQueueDTO;
import it.gov.pagopa.payment.instrument.dto.mapper.MessageMapper;
import it.gov.pagopa.payment.instrument.event.RuleEngineProducer;
import it.gov.pagopa.payment.instrument.exception.PaymentInstrumentException;
import it.gov.pagopa.payment.instrument.model.PaymentInstrument;
import it.gov.pagopa.payment.instrument.repository.PaymentInstrumentRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PaymentInstrumentServiceImpl implements PaymentInstrumentService {

  @Autowired
  private PaymentInstrumentRepository paymentInstrumentRepository;
  @Autowired
  RuleEngineProducer ruleEngineProducer;
  @Autowired
  MessageMapper messageMapper;

  @Override
  public void enrollInstrument(String initiativeId, String userId, String hpan, String channel,
      LocalDateTime activationDate) {
    List<PaymentInstrument> instrumentList = paymentInstrumentRepository.findByHpanAndStatus(hpan,
        PaymentInstrumentConstants.STATUS_ACTIVE);

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

    RuleEngineQueueDTO ruleEngineQueueDTO = RuleEngineQueueDTO.builder()
        .userId(newInstrument.getUserId())
        .initiativeId(newInstrument.getInitiativeId())
        .hpan(newInstrument.getHpan())
        .operationType("ADD_INSTRUMENT")
        .operationDate(LocalDateTime.now())
        .build();

    log.info("[PaymentInstrumentService] Sending message to Rule Engine.");
    long start = System.currentTimeMillis();

    ruleEngineProducer.sendInstrument(messageMapper.apply(ruleEngineQueueDTO));

    long end = System.currentTimeMillis();
    log.info("[PaymentInstrumentService] Sent message to Rule Engine after " + (end - start) + " ms.");
  }

  @Override
  public void deactivateAllInstrument(String initiativeId, String userId, String deactivationDate) {
    List<PaymentInstrument> paymentInstrumentList = paymentInstrumentRepository.findByInitiativeIdAndUserIdAndStatus(
        initiativeId, userId, PaymentInstrumentConstants.STATUS_ACTIVE);

    for(PaymentInstrument paymentInstrument:paymentInstrumentList) {
      this.deactivateInstrument(initiativeId,userId,paymentInstrument.getHpan(),LocalDateTime.parse(deactivationDate));
    }
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
    instrument.setDeactivationDate(deactivationDate);
    paymentInstrumentRepository.save(instrument);

    RuleEngineQueueDTO ruleEngineQueueDTO = RuleEngineQueueDTO.builder()
        .userId(instrument.getUserId())
        .initiativeId(instrument.getInitiativeId())
        .hpan(instrument.getHpan())
        .operationType("DELETE_INSTRUMENT")
        .operationDate(LocalDateTime.now())
        .build();

    log.info("[PaymentInstrumentService] Sending message to Rule Engine.");
    long start = System.currentTimeMillis();

    ruleEngineProducer.sendInstrument(messageMapper.apply(ruleEngineQueueDTO));

    long end = System.currentTimeMillis();
    log.info("[PaymentInstrumentService] Sent message to Rule Engine after " + (end - start) + " ms.");
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
}