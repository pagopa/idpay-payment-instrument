package it.gov.pagopa.payment.instrument.service;

import feign.FeignException;
import it.gov.pagopa.payment.instrument.connector.PMRestClientConnector;
import it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants;
import it.gov.pagopa.payment.instrument.dto.HpanDTO;
import it.gov.pagopa.payment.instrument.dto.HpanGetDTO;
import it.gov.pagopa.payment.instrument.dto.RTDOperationDTO;
import it.gov.pagopa.payment.instrument.dto.RuleEngineQueueDTO;
import it.gov.pagopa.payment.instrument.dto.mapper.MessageMapper;
import it.gov.pagopa.payment.instrument.dto.pm.PaymentMethodInfoList;
import it.gov.pagopa.payment.instrument.dto.pm.WalletV2;
import it.gov.pagopa.payment.instrument.dto.pm.WalletV2ListResponse;
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
  @Autowired
  PMRestClientConnector pmRestClientConnector;

  @Override
  public PaymentMethodInfoList enrollInstrument(String initiativeId, String userId, String idWallet,
      String channel,
      LocalDateTime activationDate) {
    List<PaymentInstrument> instrumentList = paymentInstrumentRepository.findByIdWalletAndStatus(
        idWallet,
        PaymentInstrumentConstants.STATUS_ACTIVE);

    for (PaymentInstrument pi : instrumentList) {
      if (!pi.getUserId().equals(userId)) {
        throw new PaymentInstrumentException(HttpStatus.FORBIDDEN.value(),
            PaymentInstrumentConstants.ERROR_PAYMENT_INSTRUMENT_ALREADY_ACTIVE);
      } else if (pi.getInitiativeId().equals(initiativeId)) {
        return null;
      }
    }

    WalletV2ListResponse walletV2ListResponse;
    try {
      log.info("[PaymentInstrumentService] Calling PM service.");
      walletV2ListResponse = pmRestClientConnector.getWalletList(userId);
      log.info("pm finito");
    } catch (FeignException e) {
      log.error("exception pm");
      throw new PaymentInstrumentException(HttpStatus.INTERNAL_SERVER_ERROR.value(),
          e.getMessage());
    }

    log.info("fuori chiamata");
    PaymentMethodInfoList infoList = new PaymentMethodInfoList();
    List<PaymentMethodInfoList> paymentMethodInfoList = new ArrayList<>();

    for (WalletV2 v2 : walletV2ListResponse.getData()) {
      if (v2.getIdWallet().equals(idWallet)) {
        switch (v2.getWalletType()) {
          case SATISPAY -> {
            infoList.setHpan(v2.getInfo().getSatispayInfo().getUuid());
            infoList.setBrandLogo(v2.getInfo().getSatispayInfo().getBrandLogo());
            paymentMethodInfoList.add(infoList);
          }
          case BPAY -> {
            infoList.setHpan(v2.getInfo().getBPayInfo().getUidHash());
            infoList.setMaskedPan(v2.getInfo().getBPayInfo().getNumberObfuscated());
            infoList.setBrandLogo(v2.getInfo().getBPayInfo().getBrandLogo());
            paymentMethodInfoList.add(infoList);
          }
          default -> {
            infoList.setHpan(v2.getInfo().getCardInfo().getHashPan());
            infoList.setMaskedPan(v2.getInfo().getCardInfo().getBlurredNumber());
            infoList.setBrandLogo(v2.getInfo().getCardInfo().getBrandLogo());
            paymentMethodInfoList.add(infoList);
          }
        }
      }
    }

    PaymentInstrument newInstrument = new PaymentInstrument(initiativeId, userId, idWallet,
        infoList.getHpan(), infoList.getMaskedPan(), infoList.getBrandLogo(),
        PaymentInstrumentConstants.STATUS_ACTIVE, channel, activationDate);
    paymentInstrumentRepository.save(newInstrument);
    try {
      sendToRuleEngine(newInstrument.getUserId(), newInstrument.getInitiativeId(),
          paymentMethodInfoList,
          PaymentInstrumentConstants.OPERATION_ADD);
    } catch (Exception e) {
      log.info("ROLLBACK PAYMENT INSTRUMENT");
      paymentInstrumentRepository.delete(newInstrument);
      throw new PaymentInstrumentException(HttpStatus.BAD_REQUEST.value(), e.getMessage());
    }
    List<String> hpanList = new ArrayList<>();
    paymentMethodInfoList.forEach(methodInfoList -> hpanList.add(methodInfoList.getHpan()));

    try {
      sendToRtd(hpanList, PaymentInstrumentConstants.OPERATION_ADD);
    } catch (Exception e) {
      this.sendToQueueError(e, hpanList, PaymentInstrumentConstants.OPERATION_ADD);
    }
    return infoList;
  }

  @Override
  public void deactivateAllInstrument(String initiativeId, String userId, String deactivationDate) {
    List<PaymentInstrument> paymentInstrumentList = paymentInstrumentRepository.findByInitiativeIdAndUserIdAndStatus(
        initiativeId, userId, PaymentInstrumentConstants.STATUS_ACTIVE);

    PaymentMethodInfoList infoList = new PaymentMethodInfoList();
    List<PaymentMethodInfoList> paymentMethodInfoList = new ArrayList<>();
    List<String> hpanList = new ArrayList<>();

    for (PaymentInstrument paymentInstrument : paymentInstrumentList) {
      paymentInstrument.setRequestDeactivationDate(LocalDateTime.parse(deactivationDate));
      paymentInstrument.setStatus(PaymentInstrumentConstants.STATUS_INACTIVE);
      infoList.setHpan(paymentInstrument.getHpan());
      infoList.setMaskedPan(paymentInstrument.getMaskedPan());
      infoList.setBrandLogo(paymentInstrument.getBrandLogo());
      paymentMethodInfoList.add(infoList);
      hpanList.add(paymentInstrument.getHpan());

    }
    paymentInstrumentRepository.saveAll(paymentInstrumentList);
    try {
      sendToRuleEngine(userId, initiativeId, paymentMethodInfoList,
          PaymentInstrumentConstants.OPERATION_DELETE);
    } catch (Exception e) {
      this.rollbackInstruments(paymentInstrumentList);
      throw new PaymentInstrumentException(HttpStatus.BAD_REQUEST.value(), e.getMessage());
    }
    sendToRtd(hpanList, PaymentInstrumentConstants.OPERATION_DELETE);
  }

  @Override
  public PaymentMethodInfoList deactivateInstrument(String initiativeId, String userId,
      String instrumentId,
      LocalDateTime deactivationDate) {
    List<PaymentInstrument> instruments = paymentInstrumentRepository.findByInitiativeIdAndUserIdAndId(
        initiativeId, userId, instrumentId);

    if (instruments.isEmpty()) {
      throw new PaymentInstrumentException(HttpStatus.NOT_FOUND.value(),
          PaymentInstrumentConstants.ERROR_PAYMENT_INSTRUMENT_NOT_FOUND);
    }
    PaymentMethodInfoList infoList = new PaymentMethodInfoList();
    instruments.forEach(instrument ->
        checkAndDelete(instrument, deactivationDate, infoList)
    );
    return infoList;
  }

  private void checkAndDelete(PaymentInstrument instrument, LocalDateTime deactivationDate,
      PaymentMethodInfoList infoList) {
    if (instrument.getStatus().equals(PaymentInstrumentConstants.STATUS_INACTIVE)) {
      return;
    }
    instrument.setStatus(PaymentInstrumentConstants.STATUS_INACTIVE);
    instrument.setRequestDeactivationDate(deactivationDate);
    paymentInstrumentRepository.save(instrument);

    infoList.setHpan(instrument.getHpan());
    infoList.setMaskedPan(instrument.getMaskedPan());
    infoList.setBrandLogo(instrument.getBrandLogo());

    List<PaymentMethodInfoList> paymentMethodInfoList = List.of(infoList);
    List<String> hpanList = Arrays.asList(instrument.getHpan());
    sendToRuleEngine(instrument.getUserId(), instrument.getInitiativeId(), paymentMethodInfoList,
        PaymentInstrumentConstants.OPERATION_DELETE);
    sendToRtd(hpanList, PaymentInstrumentConstants.OPERATION_DELETE);
  }

  private void sendToRuleEngine(String userId, String initiativeId, List<PaymentMethodInfoList>
      paymentMethodInfoList,
      String operation) {

    RuleEngineQueueDTO ruleEngineQueueDTO = RuleEngineQueueDTO.builder()
        .userId(userId)
        .initiativeId(initiativeId)
        .infoList(paymentMethodInfoList)
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
    log.info("Instrument rollbacked: {}", paymentInstrumentList.size());
  }

  private void sendToQueueError(Exception e, List<String> hpanList, String operation) {
    RTDOperationDTO rtdOperationDTO =
        RTDOperationDTO.builder()
            .hpanList(hpanList)
            .operationType(operation)
            .application("IDPAY")
            .operationDate(LocalDateTime.now())
            .build();

    final MessageBuilder<?> errorMessage = MessageBuilder.withPayload(rtdOperationDTO)
        .setHeader(PaymentInstrumentConstants.ERROR_MSG_HEADER_SRC_TYPE,
            PaymentInstrumentConstants.KAFKA)
        .setHeader(PaymentInstrumentConstants.ERROR_MSG_HEADER_SRC_SERVER,
            PaymentInstrumentConstants.BROKER_RTD)
        .setHeader(PaymentInstrumentConstants.ERROR_MSG_HEADER_SRC_TOPIC,
            PaymentInstrumentConstants.TOPIC_RTD)
        .setHeader(PaymentInstrumentConstants.ERROR_MSG_HEADER_DESCRIPTION,
            PaymentInstrumentConstants.ERROR_RTD)
        .setHeader(PaymentInstrumentConstants.ERROR_MSG_HEADER_RETRYABLE, true)
        .setHeader(PaymentInstrumentConstants.ERROR_MSG_HEADER_STACKTRACE, e.getStackTrace())
        .setHeader(PaymentInstrumentConstants.ERROR_MSG_HEADER_CLASS, e.getClass())
        .setHeader(PaymentInstrumentConstants.ERROR_MSG_HEADER_MESSAGE, e.getMessage());
    errorProducer.sendEvent(errorMessage.build());
  }
}