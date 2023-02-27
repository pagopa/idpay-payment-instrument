package it.gov.pagopa.payment.instrument.service;

import feign.FeignException;
import it.gov.pagopa.payment.instrument.connector.DecryptRestConnector;
import it.gov.pagopa.payment.instrument.connector.EncryptRestConnector;
import it.gov.pagopa.payment.instrument.connector.PMRestClientConnector;
import it.gov.pagopa.payment.instrument.connector.WalletRestConnector;
import it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants;
import it.gov.pagopa.payment.instrument.dto.*;
import it.gov.pagopa.payment.instrument.dto.mapper.AckMapper;
import it.gov.pagopa.payment.instrument.dto.mapper.MessageMapper;
import it.gov.pagopa.payment.instrument.dto.pm.PaymentMethodInfoList;
import it.gov.pagopa.payment.instrument.dto.pm.WalletV2;
import it.gov.pagopa.payment.instrument.dto.pm.WalletV2ListResponse;
import it.gov.pagopa.payment.instrument.dto.rtd.RTDEnrollAckDTO;
import it.gov.pagopa.payment.instrument.dto.rtd.RTDEventsDTO;
import it.gov.pagopa.payment.instrument.dto.rtd.RTDHpanListDTO;
import it.gov.pagopa.payment.instrument.dto.rtd.RTDMessage;
import it.gov.pagopa.payment.instrument.dto.rtd.RTDOperationDTO;
import it.gov.pagopa.payment.instrument.dto.rtd.RTDRevokeCardDTO;
import it.gov.pagopa.payment.instrument.event.producer.ErrorProducer;
import it.gov.pagopa.payment.instrument.event.producer.RTDProducer;
import it.gov.pagopa.payment.instrument.event.producer.RuleEngineProducer;
import it.gov.pagopa.payment.instrument.exception.PaymentInstrumentException;
import it.gov.pagopa.payment.instrument.model.PaymentInstrument;
import it.gov.pagopa.payment.instrument.repository.PaymentInstrumentRepository;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import it.gov.pagopa.payment.instrument.utils.AuditUtilities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PaymentInstrumentServiceImpl implements PaymentInstrumentService {

  public static final String ENROLL_FROM_ISSUER = "ENROLL_FROM_ISSUER";
  public static final String ENROLL_INSTRUMENT = "ENROLL_INSTRUMENT";
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
  @Autowired
  DecryptRestConnector decryptRestConnector;
  @Autowired
  private EncryptRestConnector encryptRestConnector;
  @Autowired
  private WalletRestConnector walletRestConnector;
  @Autowired
  private AckMapper ackMapper;
  @Autowired
  AuditUtilities auditUtilities;
  @Value(
      "${spring.cloud.stream.binders.kafka-rtd.environment.spring.cloud.stream.kafka.binder.brokers}")
  String rtdServer;
  @Value("${spring.cloud.stream.bindings.paymentInstrumentQueue-out-1.destination}")
  String rtdTopic;
  @Value(
      "${spring.cloud.stream.binders.kafka-re.environment.spring.cloud.stream.kafka.binder.brokers}")
  String ruleEngineServer;
  @Value("${spring.cloud.stream.bindings.paymentInstrumentQueue-out-0.destination}")
  String ruleEngineTopic;


  @Override
  public void enrollInstrument(String initiativeId, String userId, String idWallet,
      String channel) {

    long startTime = System.currentTimeMillis();

    List<PaymentMethodInfoList> paymentMethodInfoList = new ArrayList<>();

    PaymentMethodInfoList infoList = getPaymentMethodInfoList(
        userId, idWallet, paymentMethodInfoList);

    List<PaymentInstrument> instrumentList = paymentInstrumentRepository.findByHpanAndStatusNotContaining(
        infoList.getHpan(), PaymentInstrumentConstants.STATUS_INACTIVE);

    RTDHpanListDTO hpanListDTO = new RTDHpanListDTO();
    hpanListDTO.setHpan(infoList.getHpan());
    hpanListDTO.setConsent(infoList.isConsent());

    for (PaymentInstrument pi : instrumentList) {
      if (!pi.getUserId().equals(userId)) {
        log.error(
            "[ENROLL_INSTRUMENT] The Payment Instrument is already in use by another citizen.");
        auditUtilities.logEnrollInstrumentKO(
            PaymentInstrumentConstants.ERROR_PAYMENT_INSTRUMENT_ALREADY_ACTIVE, idWallet, channel);
        performanceLog(startTime, ENROLL_INSTRUMENT);
        throw new PaymentInstrumentException(HttpStatus.FORBIDDEN.value(),
            PaymentInstrumentConstants.ERROR_PAYMENT_INSTRUMENT_ALREADY_ACTIVE);
      }

      if (pi.getInitiativeId().equals(initiativeId) && pi.getStatus()
          .equals(PaymentInstrumentConstants.STATUS_ENROLLMENT_FAILED)) {
        log.info(
            "[ENROLL_INSTRUMENT] Try enrolling again the instrument with status failed");
        enrollInstrumentFailed(pi, hpanListDTO, channel);
        performanceLog(startTime, ENROLL_INSTRUMENT);
        return;
      }

      if (pi.getInitiativeId().equals(initiativeId) && !pi.getStatus()
          .equals(PaymentInstrumentConstants.STATUS_ENROLLMENT_FAILED_KO_RE)) {
        log.info(
            "[ENROLL_INSTRUMENT] The Payment Instrument is already active, or there is a pending request on it.");
        performanceLog(startTime, ENROLL_INSTRUMENT);
        auditUtilities.logEnrollInstrumentKO("already active or in pending", idWallet, channel);
        return;
      }
    }

    PaymentInstrument newInstrument = savePaymentInstrument(
        initiativeId, userId, idWallet, channel, infoList);

    try {
      sendToRtd(List.of(hpanListDTO), PaymentInstrumentConstants.OPERATION_ADD, initiativeId);
      newInstrument.setStatus(PaymentInstrumentConstants.STATUS_PENDING_RTD);
      newInstrument.setUpdateDate(LocalDateTime.now());
      newInstrument.setCreationDate(LocalDateTime.now());
      paymentInstrumentRepository.save(newInstrument);
    } catch (Exception e) {
      log.info(
          "[ENROLL_INSTRUMENT] Couldn't send to RTD: resetting the Instrument.");
      auditUtilities.logEnrollInstrumentKO(e.getMessage(), newInstrument.getIdWallet(), channel);
      paymentInstrumentRepository.delete(newInstrument);
      performanceLog(startTime, ENROLL_INSTRUMENT);
      throw new PaymentInstrumentException(HttpStatus.BAD_REQUEST.value(), e.getMessage());
    }
    auditUtilities.logEnrollInstrumentComplete(newInstrument.getIdWallet(),
        newInstrument.getChannel());
    performanceLog(startTime, ENROLL_INSTRUMENT);
  }

  private void enrollInstrumentFailed(PaymentInstrument instrument, RTDHpanListDTO hpanListDTO,
      String channel) {
    try {
      sendToRtd(List.of(hpanListDTO), PaymentInstrumentConstants.OPERATION_ADD,
          instrument.getInitiativeId());
      instrument.setStatus(PaymentInstrumentConstants.STATUS_PENDING_RTD);
      instrument.setChannel(channel);
      instrument.setUpdateDate(LocalDateTime.now());
      paymentInstrumentRepository.save(instrument);
    } catch (Exception e) {
      log.info(
          "[ENROLL_INSTRUMENT] Couldn't send to RTD: resetting the Payment Instrument.");
      auditUtilities.logEnrollInstrumentKO(e.getMessage(), instrument.getIdWallet(), channel);
      paymentInstrumentRepository.delete(instrument);
      throw new PaymentInstrumentException(HttpStatus.BAD_REQUEST.value(), e.getMessage());
    }
    auditUtilities.logEnrollInstrumentComplete(instrument.getIdWallet(), channel);
  }

  private PaymentInstrument savePaymentInstrument(String initiativeId, String userId,
      String idWallet, String channel, PaymentMethodInfoList infoList) {
    PaymentInstrument newInstrument = PaymentInstrument.builder()
        .initiativeId(initiativeId)
        .userId(userId)
        .idWallet(idWallet)
        .hpan(infoList.getHpan())
        .maskedPan(infoList.getMaskedPan())
        .brandLogo(infoList.getBrandLogo())
        .brand(infoList.getBrand())
        .channel(channel)
        .consent(infoList.isConsent())
        .build();
    paymentInstrumentRepository.save(newInstrument);
    return newInstrument;
  }

  private PaymentMethodInfoList getPaymentMethodInfoList(String userId, String idWallet,
      List<PaymentMethodInfoList> paymentMethodInfoList) {
    PaymentMethodInfoList infoList = new PaymentMethodInfoList();
    WalletV2ListResponse walletV2ListResponse;
    try {
      DecryptCfDTO decryptedCfDTO = decryptRestConnector.getPiiByToken(userId);
      Instant start = Instant.now();
      log.debug("Calling PM service at: " + start);
      walletV2ListResponse = pmRestClientConnector.getWalletList(decryptedCfDTO.getPii());
      log.info(walletV2ListResponse.toString());
      Instant finish = Instant.now();
      long time = Duration.between(start, finish).toMillis();
      log.info("PM's call finished at: " + finish + " The PM service took: " + time + "ms");
    } catch (FeignException e) {
      throw new PaymentInstrumentException(e.status(),
          e.getMessage());
    }

    int countIdWallet = 0;

    for (WalletV2 v2 : walletV2ListResponse.getData()) {
      if (v2.getIdWallet().equals(idWallet) && v2.getEnableableFunctions()
          .contains(PaymentInstrumentConstants.BPD)) {
        switch (v2.getWalletType()) {
          case PaymentInstrumentConstants.SATISPAY -> {
            infoList.setHpan(v2.getInfo().getUuid());
            infoList.setMaskedPan(PaymentInstrumentConstants.SATISPAY);
            infoList.setBrandLogo(v2.getInfo().getBrandLogo());
            infoList.setBrand(PaymentInstrumentConstants.SATISPAY);
            infoList.setConsent(true);
            paymentMethodInfoList.add(infoList);
          }
          case PaymentInstrumentConstants.BPAY -> {
            infoList.setHpan(v2.getInfo().getUidHash());
            infoList.setMaskedPan(PaymentInstrumentConstants.BPAY);
            infoList.setBrandLogo(v2.getInfo().getBrandLogo());
            infoList.setBrand(PaymentInstrumentConstants.BPAY);
            infoList.setConsent(true);
            paymentMethodInfoList.add(infoList);
          }
          default -> {
            infoList.setHpan(v2.getInfo().getHashPan());
            infoList.setMaskedPan(v2.getInfo().getBlurredNumber());
            infoList.setBrand(v2.getInfo().getBrand());
            infoList.setBrandLogo(v2.getInfo().getBrandLogo());
            infoList.setConsent(true);
            paymentMethodInfoList.add(infoList);
          }
        }
      } else {
        countIdWallet++;
      }
    }

    if (countIdWallet == walletV2ListResponse.getData().size()) {
      throw new PaymentInstrumentException(HttpStatus.NOT_FOUND.value(),
          PaymentInstrumentConstants.ERROR_PAYMENT_INSTRUMENT_NOT_FOUND);
    }
    return infoList;
  }

  @Override
  public void deactivateAllInstruments(String initiativeId, String userId,
      String deactivationDate) {
    long startTime = System.currentTimeMillis();

    List<PaymentInstrument> paymentInstrumentList = paymentInstrumentRepository.findByInitiativeIdAndUserIdAndStatus(
        initiativeId, userId, PaymentInstrumentConstants.STATUS_ACTIVE);

    PaymentMethodInfoList infoList = new PaymentMethodInfoList();
    List<PaymentMethodInfoList> paymentMethodInfoList = new ArrayList<>();
    List<RTDHpanListDTO> hpanList = new ArrayList<>();
    RTDHpanListDTO rtdHpanListDTO = new RTDHpanListDTO();

    for (PaymentInstrument paymentInstrument : paymentInstrumentList) {
      paymentInstrument.setDeactivationDate(LocalDateTime.parse(deactivationDate));
      paymentInstrument.setStatus(PaymentInstrumentConstants.STATUS_INACTIVE);
      paymentInstrument.setDeleteChannel(PaymentInstrumentConstants.IO);
      infoList.setHpan(paymentInstrument.getHpan());
      infoList.setMaskedPan(paymentInstrument.getMaskedPan());
      infoList.setBrandLogo(paymentInstrument.getBrandLogo());
      paymentMethodInfoList.add(infoList);
      rtdHpanListDTO.setHpan(paymentInstrument.getHpan());
      rtdHpanListDTO.setConsent(paymentInstrument.isConsent());
      hpanList.add(rtdHpanListDTO);
      paymentInstrument.setUpdateDate(LocalDateTime.now());
    }
    paymentInstrumentRepository.saveAll(paymentInstrumentList);

    try {
      sendToRuleEngine(userId, initiativeId, PaymentInstrumentConstants.IO, paymentMethodInfoList,
          PaymentInstrumentConstants.OPERATION_DELETE);
    } catch (Exception e) {
      this.rollbackInstruments(paymentInstrumentList);
      performanceLog(startTime, "DEACTIVATE_ALL_INSTRUMENTS");
      throw new PaymentInstrumentException(HttpStatus.BAD_REQUEST.value(), e.getMessage());
    }
    sendToRtd(hpanList, PaymentInstrumentConstants.OPERATION_DELETE, initiativeId);
    performanceLog(startTime, "DEACTIVATE_ALL_INSTRUMENTS");
  }

  @Override
  public void deactivateInstrument(String initiativeId, String userId,
      String instrumentId) {
    long startTime = System.currentTimeMillis();

    log.info("[DEACTIVATE_INSTRUMENT] Deleting instrument");

    PaymentInstrument instrument = paymentInstrumentRepository.findByInitiativeIdAndUserIdAndId(
        initiativeId, userId, instrumentId).orElse(null);

    if (instrument == null) {
      throw new PaymentInstrumentException(HttpStatus.NOT_FOUND.value(),
          PaymentInstrumentConstants.ERROR_PAYMENT_INSTRUMENT_NOT_FOUND);
    }

    if (instrument.getStatus().equals(PaymentInstrumentConstants.STATUS_ACTIVE)) {
      instrument.setStatus(PaymentInstrumentConstants.STATUS_PENDING_DEACTIVATION_REQUEST);
      instrument.setUpdateDate(LocalDateTime.now());
      instrument.setDeleteChannel(PaymentInstrumentConstants.IO);
      paymentInstrumentRepository.save(instrument);
      PaymentMethodInfoList infoList = new PaymentMethodInfoList(instrument.getHpan(),
          instrument.getMaskedPan(), instrument.getBrandLogo(), instrument.getBrand(),
          instrument.isConsent());
      try {
        sendToRuleEngine(userId, initiativeId, PaymentInstrumentConstants.IO,
            List.of(infoList),
            PaymentInstrumentConstants.OPERATION_DELETE);
      } catch (Exception e) {
        log.info(
            "[DEACTIVATE_INSTRUMENT] Couldn't send to Rule Engine: resetting the Payment Instrument.");
        auditUtilities.logDeactivationKO(instrument.getIdWallet(), instrument.getChannel(),
            LocalDateTime.now());
        instrument.setStatus(PaymentInstrumentConstants.STATUS_ACTIVE);
        paymentInstrumentRepository.save(instrument);
        performanceLog(startTime, "DEACTIVATE_INSTRUMENT");
        throw new PaymentInstrumentException(HttpStatus.BAD_REQUEST.value(), e.getMessage());
      }
    }
    performanceLog(startTime, "DEACTIVATE_INSTRUMENT");
  }

  @Override
  public void processRtdMessage(RTDEventsDTO dto) {
    long startTime = System.currentTimeMillis();

    if (dto instanceof RTDRevokeCardDTO revokeCardDTO) {
      deactivateInstrumentFromPM(revokeCardDTO.getData());
    }

    if (dto instanceof RTDEnrollAckDTO enrollAckDTO) {
      saveAckFromRTD(enrollAckDTO);
    }
    performanceLog(startTime, "PROCESS_RTD_MESSAGE");
  }

  private void saveAckFromRTD(RTDEnrollAckDTO enrollAckDTO) {
    log.info("[SAVE_ACK_FROM_RTD] Processing new ACK from RTD");

    if (!enrollAckDTO.getData().getApplication().equals(PaymentInstrumentConstants.ID_PAY)) {
      log.info(
          "[SAVE_ACK_FROM_RTD] This message is for another application. No processing to be done");
      return;
    }

    PaymentInstrument instrument = paymentInstrumentRepository.findByInitiativeIdAndHpanAndStatus(
        enrollAckDTO.getCorrelationId(), enrollAckDTO.getData().getHpan(),
        PaymentInstrumentConstants.STATUS_PENDING_RTD);
    if (instrument == null) {
      log.info("[SAVE_ACK_FROM_RTD] No instrument to update");
      return;
    }

    List<PaymentMethodInfoList> paymentMethodInfoList = new ArrayList<>();
    PaymentMethodInfoList paymentMethodInfo = new PaymentMethodInfoList(instrument.getHpan(),
        instrument.getMaskedPan(), instrument.getBrandLogo(), instrument.getBrand(),
        instrument.isConsent());
    paymentMethodInfoList.add(paymentMethodInfo);

    instrument.setRtdAckDate(enrollAckDTO.getData().getTimestamp().toLocalDateTime());
    paymentInstrumentRepository.save(instrument);

    try {
      sendToRuleEngine(instrument.getUserId(), instrument.getInitiativeId(),
          instrument.getChannel(),
          paymentMethodInfoList, PaymentInstrumentConstants.OPERATION_ADD);

      instrument.setStatus(PaymentInstrumentConstants.STATUS_PENDING_RE);
      instrument.setUpdateDate(LocalDateTime.now());
      paymentInstrumentRepository.save(instrument);
    } catch (Exception e) {
      auditUtilities.logEnrollInstrumentKO("Couldn't send to Rule Engine", instrument.getIdWallet(),
          instrument.getChannel());
      log.info("[SAVE_ACK_FROM_RTD] Couldn't send to Rule Engine: payment instrument with ID {}",
          instrument.getId());
    }
  }

  @Scheduled(cron = "${retrieve-enroll.schedule}")
  private void checkPendingTimeLimit() {

    long startTime = System.currentTimeMillis();

    List<PaymentInstrument> instruments = paymentInstrumentRepository.findByStatusRegex(
        PaymentInstrumentConstants.REGEX_PENDING_ENROLL);
    LocalDateTime timeStampNow = LocalDateTime.now();
    for (PaymentInstrument instrument : instruments) {
      if (timeStampNow.isAfter(instrument.getUpdateDate().plusHours(4))) {
        log.info("[CHECK_PENDING_TIME_LIMIT] Pending time limit expired  for instrument ID {}",
            instrument.getId());
        List<PaymentInstrument> activeInstruments = paymentInstrumentRepository.findByHpanAndStatus(
            instrument.getHpan(), PaymentInstrumentConstants.STATUS_ACTIVE);
        if (activeInstruments.isEmpty()) {
          log.info(
              "[CHECK_INSTRUMENT] The instrument ID {} is not currently active on any other initiative",
              instrument.getId());
          RTDHpanListDTO rtdHpanListDTO = new RTDHpanListDTO();
          rtdHpanListDTO.setHpan(instrument.getHpan());
          rtdHpanListDTO.setConsent(instrument.isConsent());
          List<RTDHpanListDTO> hpanList = List.of(rtdHpanListDTO);
          sendToRtd(hpanList, PaymentInstrumentConstants.OPERATION_DELETE,
              instrument.getInitiativeId());
        }
        if (instrument.getStatus().equals(PaymentInstrumentConstants.STATUS_PENDING_RE)) {
          PaymentMethodInfoList infoList = new PaymentMethodInfoList();
          infoList.setHpan(instrument.getHpan());
          infoList.setMaskedPan(instrument.getMaskedPan());
          infoList.setBrandLogo(instrument.getBrandLogo());
          infoList.setConsent(instrument.isConsent());
          List<PaymentMethodInfoList> paymentMethodInfoList = List.of(infoList);
          sendToRuleEngine(instrument.getUserId(), instrument.getInitiativeId(),
              instrument.getChannel(),
              paymentMethodInfoList, PaymentInstrumentConstants.OPERATION_DELETE);
        }
        instrument.setStatus(PaymentInstrumentConstants.STATUS_ENROLLMENT_FAILED);
        instrument.setUpdateDate(LocalDateTime.now());
        paymentInstrumentRepository.save(instrument);
        auditUtilities.logEnrollInstrumentKO("Pending Time Limit expired", instrument.getIdWallet(),
            instrument.getChannel());
      }
      performanceLog(startTime, "CHECK_PENDING_TIME_LIMIT");
    }
  }

  private void deactivateInstrumentFromPM(RTDMessage rtdMessage) {

    log.info("[DEACTIVATE_INSTRUMENT_PM] Processing new revoke from RTD : {}", rtdMessage);

    EncryptedCfDTO encryptedCfDTO = new EncryptedCfDTO();

    try {
      encryptedCfDTO = encryptRestConnector.upsertToken(
          new CFDTO(rtdMessage.getFiscalCode()));
    } catch (Exception e) {
      log.info("[DEACTIVATE_INSTRUMENT_PM] Error while encrypting.");
    }
    List<PaymentInstrument> instruments = paymentInstrumentRepository.findByInitiativeIdAndUserIdAndStatusNotContaining(
        rtdMessage.getHpan(), encryptedCfDTO.getToken(),
        PaymentInstrumentConstants.STATUS_INACTIVE);
    if (instruments.isEmpty()) {
      log.info("[DEACTIVATE_INSTRUMENT_PM] No instrument to delete");
      return;
    }
    List<WalletDTO> walletDTOS = new ArrayList<>();
    for (PaymentInstrument instrument : instruments) {
      if (instrument.getStatus().equals(PaymentInstrumentConstants.STATUS_ACTIVE)
          || instrument.getStatus()
          .equals(PaymentInstrumentConstants.STATUS_PENDING_DEACTIVATION_REQUEST)) {
        WalletDTO walletDTO = new WalletDTO(instrument.getInitiativeId(), instrument.getUserId(),
            instrument.getHpan(),
            instrument.getBrandLogo(), instrument.getBrand(), instrument.getMaskedPan());
        walletDTOS.add(walletDTO);
      }
    }
    if (!walletDTOS.isEmpty()) {
      walletRestConnector.updateWallet(new WalletCallDTO(walletDTOS));
    }
    instruments.forEach(instrument ->
        checkAndDelete(instrument, LocalDateTime.from(rtdMessage.getTimestamp())));
  }

  private void checkAndDelete(PaymentInstrument instrument,
      LocalDateTime deactivationDate) {
    PaymentMethodInfoList infoList = new PaymentMethodInfoList();
    RTDHpanListDTO rtdHpanListDTO = new RTDHpanListDTO();

    if (instrument.getStatus().equals(PaymentInstrumentConstants.STATUS_INACTIVE)) {
      return;
    }

    instrument.setStatus(PaymentInstrumentConstants.STATUS_INACTIVE);
    instrument.setDeactivationDate(deactivationDate);
    instrument.setUpdateDate(LocalDateTime.now());
    instrument.setDeleteChannel(PaymentInstrumentConstants.PM);
    paymentInstrumentRepository.save(instrument);
    auditUtilities.logDeactivationComplete(instrument.getIdWallet(), instrument.getChannel(),
        deactivationDate);

    infoList.setHpan(instrument.getHpan());
    infoList.setMaskedPan(instrument.getMaskedPan());
    infoList.setBrandLogo(instrument.getBrandLogo());
    infoList.setConsent(instrument.isConsent());
    rtdHpanListDTO.setHpan(instrument.getHpan());
    rtdHpanListDTO.setConsent(instrument.isConsent());

    List<PaymentMethodInfoList> paymentMethodInfoList = List.of(infoList);
    List<RTDHpanListDTO> hpanList = List.of(rtdHpanListDTO);
    try {
      sendToRuleEngine(instrument.getUserId(), instrument.getInitiativeId(),
          PaymentInstrumentConstants.PM,
          paymentMethodInfoList, PaymentInstrumentConstants.OPERATION_DELETE);
    } catch (Exception exception) {

      RuleEngineQueueDTO ruleEngineQueueDTO = RuleEngineQueueDTO.builder()
          .userId(instrument.getUserId())
          .initiativeId(instrument.getInitiativeId())
          .infoList(paymentMethodInfoList)
          .channel(PaymentInstrumentConstants.PM)
          .operationType(PaymentInstrumentConstants.OPERATION_DELETE)
          .operationDate(LocalDateTime.now())
          .build();

      final MessageBuilder<?> errorMessage = MessageBuilder.withPayload(
          messageMapper.apply(ruleEngineQueueDTO));
      this.sendToQueueError(exception, errorMessage, ruleEngineServer, ruleEngineTopic);
    }
    sendToRtd(hpanList, PaymentInstrumentConstants.OPERATION_DELETE, instrument.getInitiativeId());
  }

  private void sendToRuleEngine(String userId, String initiativeId, String channel,
      List<PaymentMethodInfoList>
          paymentMethodInfoList, String operation) {

    RuleEngineQueueDTO ruleEngineQueueDTO = RuleEngineQueueDTO.builder()
        .userId(userId)
        .initiativeId(initiativeId)
        .infoList(paymentMethodInfoList)
        .channel(channel)
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

  private void sendToRtd(List<RTDHpanListDTO> hpanList, String operation, String initiativeId) {

    List<RTDHpanListDTO> toRtd = new ArrayList<>();

    for (RTDHpanListDTO hpan : hpanList) {
      if (operation.equals(PaymentInstrumentConstants.OPERATION_ADD) ||
          paymentInstrumentRepository.countByHpanAndStatusIn(hpan.getHpan(),
              List.of(PaymentInstrumentConstants.STATUS_ACTIVE,
                  PaymentInstrumentConstants.STATUS_PENDING_DEACTIVATION_REQUEST)) == 0) {
        toRtd.add(hpan);
      }
    }

    if (!toRtd.isEmpty()) {
      RTDOperationDTO rtdOperationDTO =
          RTDOperationDTO.builder()
              .hpanList(toRtd)
              .operationType(operation)
              .correlationId(initiativeId)
              .application(PaymentInstrumentConstants.ID_PAY)
              .build();

      log.info("[PaymentInstrumentService - Operation: {}] Sending message to RTD.", operation);

      try {
        rtdProducer.sendInstrument(rtdOperationDTO);
      } catch (Exception exception) {
        if (operation.equals(PaymentInstrumentConstants.OPERATION_ADD)) {
          throw new PaymentInstrumentException(HttpStatus.BAD_REQUEST.value(),
              exception.getMessage());
        }
        final MessageBuilder<?> errorMessage = MessageBuilder.withPayload(rtdOperationDTO);
        this.sendToQueueError(exception, errorMessage, rtdServer, rtdTopic);
      }
    }
  }

  private int countByInitiativeIdAndUserIdAndStatusIn(String initiativeId, String userId,
      List<String> status) {
    return paymentInstrumentRepository.countByInitiativeIdAndUserIdAndStatusIn(initiativeId,
        userId,
        status);
  }

  @Override
  public HpanGetDTO getHpan(String initiativeId, String userId) {
    long startTime = System.currentTimeMillis();
    checkPendingTimeLimit();

    List<PaymentInstrument> paymentInstrument = paymentInstrumentRepository.findByInitiativeIdAndUserIdAndStatusIn(
        initiativeId, userId, List.of(PaymentInstrumentConstants.STATUS_ACTIVE,
            PaymentInstrumentConstants.STATUS_PENDING_RTD,
            PaymentInstrumentConstants.STATUS_PENDING_RE,
            PaymentInstrumentConstants.STATUS_PENDING_DEACTIVATION_REQUEST));

    HpanGetDTO dto = buildHpanList(paymentInstrument);
    performanceLog(startTime, "GET_HPAN");
    return dto;
  }

  @Override
  public void processAck(RuleEngineAckDTO ruleEngineAckDTO) {
    long startTime = System.currentTimeMillis();
    log.info("[PROCESS_ACK_FROM_RULE_ENGINE] Processing new message.");

    if (ruleEngineAckDTO.getOperationType().equals(PaymentInstrumentConstants.OPERATION_ADD)) {
      log.info("[PROCESS_ACK_FROM_RULE_ENGINE] Processing ACK for an enrollment request.");
      processAckEnroll(ruleEngineAckDTO);
    }

    if (ruleEngineAckDTO.getOperationType().equals(PaymentInstrumentConstants.OPERATION_DELETE)) {
      log.info("[PROCESS_ACK_FROM_RULE_ENGINE] Processing ACK for a deactivation request.");
      processAckDeactivate(ruleEngineAckDTO);
    }
    performanceLog(startTime, "PROCESS_ACK_FROM_RULE_ENGINE");
  }

  @Override
  public HpanGetDTO getHpanFromIssuer(String initiativeId, String userId, String channel) {
    long startTime = System.currentTimeMillis();

    List<PaymentInstrument> paymentInstrument = paymentInstrumentRepository.findByInitiativeIdAndUserIdAndChannelAndStatusIn(
        initiativeId, userId, channel, List.of(PaymentInstrumentConstants.STATUS_ACTIVE,
            PaymentInstrumentConstants.STATUS_PENDING_RTD,
            PaymentInstrumentConstants.STATUS_PENDING_RE,
            PaymentInstrumentConstants.STATUS_PENDING_DEACTIVATION_REQUEST
        ));

    HpanGetDTO dto = buildHpanList(paymentInstrument);
    performanceLog(startTime, "GET_HPAN_FROM_ISSUER");
    return dto;
  }

  @Override
  public void enrollFromIssuer(InstrumentIssuerDTO body) {
    long startTime = System.currentTimeMillis();

    List<PaymentInstrument> instrumentList = paymentInstrumentRepository.findByHpanAndStatusNotContaining(
        body.getHpan(), PaymentInstrumentConstants.STATUS_INACTIVE);

    for (PaymentInstrument pi : instrumentList) {
      if (!pi.getUserId().equals(body.getUserId())) {
        log.error(
            "[ENROLL_FROM_ISSUER] The Payment Instrument is already in use by another citizen.");
        auditUtilities.logEnrollInstrFromIssuerKO(
            PaymentInstrumentConstants.ERROR_PAYMENT_INSTRUMENT_ALREADY_ACTIVE, body.getHpan(),
            body.getChannel());
        performanceLog(startTime, ENROLL_FROM_ISSUER);
        throw new PaymentInstrumentException(HttpStatus.FORBIDDEN.value(),
            PaymentInstrumentConstants.ERROR_PAYMENT_INSTRUMENT_ALREADY_ACTIVE);
      }

      if (pi.getInitiativeId().equals(body.getInitiativeId())) {
        log.info(
            "[ENROLL_FROM_ISSUER] The Payment Instrument is already active, or there is a pending request on it.");
        auditUtilities.logEnrollInstrFromIssuerKO("already active or in pending", body.getHpan(),
            body.getChannel());
        performanceLog(startTime, ENROLL_FROM_ISSUER);
        return;
      }
    }

    PaymentMethodInfoList infoList = new PaymentMethodInfoList(body.getHpan(), body.getMaskedPan(),
        body.getBrandLogo(), body.getBrand(), true);

    PaymentInstrument newInstrument = savePaymentInstrument(
        body.getInitiativeId(), body.getUserId(), null, body.getChannel(), infoList);

    RTDHpanListDTO hpanListDTO = new RTDHpanListDTO();
    hpanListDTO.setHpan(infoList.getHpan());
    hpanListDTO.setConsent(infoList.isConsent());

    try {
      sendToRtd(List.of(hpanListDTO), PaymentInstrumentConstants.OPERATION_ADD,
          newInstrument.getInitiativeId());
      newInstrument.setStatus(PaymentInstrumentConstants.STATUS_PENDING_RTD);
      newInstrument.setUpdateDate(LocalDateTime.now());
      paymentInstrumentRepository.save(newInstrument);
    } catch (Exception e) {
      log.info(
          "[ENROLL_FROM_ISSUER] Couldn't send to RTD: resetting the Payment Instrument.");
      auditUtilities.logEnrollInstrFromIssuerKO("error in RTD request", newInstrument.getIdWallet(),
          newInstrument.getChannel());
      paymentInstrumentRepository.delete(newInstrument);
      performanceLog(startTime, ENROLL_FROM_ISSUER);
      throw new PaymentInstrumentException(HttpStatus.BAD_REQUEST.value(), e.getMessage());
    }
    auditUtilities.logEnrollInstrFromIssuerComplete(newInstrument.getHpan(),
        newInstrument.getChannel());
    performanceLog(startTime, ENROLL_FROM_ISSUER);
  }

  private HpanGetDTO buildHpanList(List<PaymentInstrument> paymentInstrument) {

    HpanGetDTO hpanGetDTO = new HpanGetDTO();
    List<HpanDTO> hpanDTOList = new ArrayList<>();

    for (PaymentInstrument paymentInstruments : paymentInstrument) {
      HpanDTO hpanDTO = new HpanDTO();
      hpanDTO.setChannel(paymentInstruments.getChannel());
      hpanDTO.setBrandLogo(paymentInstruments.getBrandLogo());
      hpanDTO.setBrand(paymentInstruments.getBrand());
      hpanDTO.setMaskedPan(paymentInstruments.getMaskedPan());
      hpanDTO.setStatus(paymentInstruments.getStatus());
      if (paymentInstruments.getStatus().equals(PaymentInstrumentConstants.STATUS_PENDING_RE)
          || paymentInstruments.getStatus().equals(PaymentInstrumentConstants.STATUS_PENDING_RTD)) {
        hpanDTO.setStatus(PaymentInstrumentConstants.STATUS_PENDING_ENROLLMENT_REQUEST);
      }
      hpanDTO.setInstrumentId(paymentInstruments.getId());
      hpanDTO.setIdWallet(paymentInstruments.getIdWallet());
      hpanDTO.setActivationDate(paymentInstruments.getActivationDate());
      hpanDTOList.add(hpanDTO);
    }
    hpanGetDTO.setInstrumentList(hpanDTOList);
    return hpanGetDTO;
  }

  private void processAckDeactivate(RuleEngineAckDTO ruleEngineAckDTO) {
    RTDHpanListDTO hpanListDTO = new RTDHpanListDTO();

    String hpan =
        (!ruleEngineAckDTO.getHpanList().isEmpty()) ? ruleEngineAckDTO.getHpanList().get(0)
            : ruleEngineAckDTO.getRejectedHpanList().get(0);

    PaymentInstrument instrument = paymentInstrumentRepository.findByInitiativeIdAndUserIdAndHpanAndStatus(
            ruleEngineAckDTO.getInitiativeId(), ruleEngineAckDTO.getUserId(),
            hpan, PaymentInstrumentConstants.STATUS_PENDING_DEACTIVATION_REQUEST)
        .orElse(null);

    if (instrument == null) {
      log.info("[PROCESS_ACK_DEACTIVATE] No pending deactivation requests found for this ACK.");
      return;
    }

    if (!ruleEngineAckDTO.getHpanList().isEmpty()) {

      hpanListDTO.setHpan(hpan);
      hpanListDTO.setConsent(instrument.isConsent());

      log.info("[PROCESS_ACK_DEACTIVATE] Deactivation OK: updating instrument status to {}.",
          PaymentInstrumentConstants.STATUS_INACTIVE);

      instrument.setDeactivationDate(ruleEngineAckDTO.getTimestamp());
      instrument.setStatus(PaymentInstrumentConstants.STATUS_INACTIVE);
      instrument.setUpdateDate(LocalDateTime.now());
      paymentInstrumentRepository.save(instrument);

      auditUtilities.logDeactivationComplete(instrument.getIdWallet(), instrument.getChannel(),
          LocalDateTime.now());
      log.info("[PROCESS_ACK_DEACTIVATE] Deactivation OK: sending to RTD.");
      sendToRtd(List.of(hpanListDTO), ruleEngineAckDTO.getOperationType(),
          instrument.getInitiativeId());
    }

    if (!ruleEngineAckDTO.getRejectedHpanList().isEmpty()) {

      log.info(
          "[PROCESS_ACK_DEACTIVATE] Deactivation KO: resetting Payment Instrument to status {}.",
          PaymentInstrumentConstants.STATUS_ACTIVE);

      instrument.setDeleteChannel(null);
      instrument.setStatus(PaymentInstrumentConstants.STATUS_ACTIVE);
      paymentInstrumentRepository.save(instrument);
      auditUtilities.logDeactivationKO(instrument.getIdWallet(), instrument.getChannel(),
          LocalDateTime.now());
    }

    int nInstr = countByInitiativeIdAndUserIdAndStatusIn(instrument.getInitiativeId(),
        instrument.getUserId(), List.of(PaymentInstrumentConstants.STATUS_ACTIVE,
            PaymentInstrumentConstants.STATUS_PENDING_DEACTIVATION_REQUEST));

    InstrumentAckDTO dto = ackMapper.ackToWallet(ruleEngineAckDTO, instrument.getDeleteChannel(),
        instrument.getMaskedPan(), instrument.getBrandLogo(), instrument.getBrand(), nInstr);

    log.info("[PROCESS_ACK_DEACTIVATE] Deactivation OK: updating wallet.");

    walletRestConnector.processAck(dto);

  }

  private void processAckEnroll(RuleEngineAckDTO ruleEngineAckDTO) {

    String hpan =
        (!ruleEngineAckDTO.getHpanList().isEmpty()) ? ruleEngineAckDTO.getHpanList().get(0)
            : ruleEngineAckDTO.getRejectedHpanList().get(0);

    String status =
        (!ruleEngineAckDTO.getHpanList().isEmpty()) ? PaymentInstrumentConstants.STATUS_ACTIVE
            : PaymentInstrumentConstants.STATUS_ENROLLMENT_FAILED_KO_RE;

    PaymentInstrument instrument = paymentInstrumentRepository.findByInitiativeIdAndUserIdAndHpanAndStatus(
            ruleEngineAckDTO.getInitiativeId(), ruleEngineAckDTO.getUserId(),
            hpan, PaymentInstrumentConstants.STATUS_PENDING_RE)
        .orElse(null);

    if (instrument == null) {
      log.info("[PROCESS_ACK_ENROLL] No pending enrollment requests found for this ACK.");
      return;
    }

    if (status.equals(PaymentInstrumentConstants.STATUS_ENROLLMENT_FAILED_KO_RE)) {
      log.info("[PROCESS_ACK_ENROLL] ACK RULE ENGINE KO: updating instrument status to {}.",
          PaymentInstrumentConstants.STATUS_ENROLLMENT_FAILED);
      auditUtilities.logAckEnrollKO(instrument.getIdWallet(), instrument.getChannel(),
          ruleEngineAckDTO.getTimestamp());

      RTDHpanListDTO rtdHpanListDTO = new RTDHpanListDTO();
      rtdHpanListDTO.setHpan(instrument.getHpan());
      rtdHpanListDTO.setConsent(instrument.isConsent());
      List<RTDHpanListDTO> hpanList = List.of(rtdHpanListDTO);
      sendToRtd(hpanList, PaymentInstrumentConstants.OPERATION_DELETE,
          instrument.getInitiativeId());
    }

    if (status.equals(PaymentInstrumentConstants.STATUS_ACTIVE)) {
      log.info("[PROCESS_ACK_ENROLL] ACK RULE ENGINE OK: updating instrument status to {}.",
          PaymentInstrumentConstants.STATUS_ACTIVE);
      instrument.setActivationDate(ruleEngineAckDTO.getTimestamp());
      auditUtilities.logAckEnrollComplete(instrument.getIdWallet(), instrument.getChannel(),
          instrument.getActivationDate());
    }

    instrument.setStatus(status);
    instrument.setReAckDate(ruleEngineAckDTO.getTimestamp());
    instrument.setUpdateDate(LocalDateTime.now());
    paymentInstrumentRepository.save(instrument);

    int nInstr = countByInitiativeIdAndUserIdAndStatusIn(instrument.getInitiativeId(),
        instrument.getUserId(), List.of(PaymentInstrumentConstants.STATUS_ACTIVE,
            PaymentInstrumentConstants.STATUS_PENDING_DEACTIVATION_REQUEST));

    InstrumentAckDTO dto = ackMapper.ackToWallet(ruleEngineAckDTO, instrument.getChannel(),
        instrument.getMaskedPan(), instrument.getBrandLogo(), instrument.getBrand(), nInstr);

    log.info("[PROCESS_ACK_ENROLL] Updating wallet with status {}.", dto.getOperationType());
    walletRestConnector.processAck(dto);
  }

  @Override
  public void rollbackInstruments(List<PaymentInstrument> paymentInstrumentList) {
    long startTime = System.currentTimeMillis();

    for (PaymentInstrument instrument : paymentInstrumentList) {
      instrument.setStatus(PaymentInstrumentConstants.STATUS_ACTIVE);
      instrument.setDeactivationDate(null);
      instrument.setUpdateDate(LocalDateTime.now());
      auditUtilities.logDeactivationKO(instrument.getIdWallet(), instrument.getChannel(),
          instrument.getUpdateDate());
    }
    paymentInstrumentRepository.saveAll(paymentInstrumentList);
    log.info("[ROLLBACK_INSTRUMENTS] Instrument rollbacked: {}", paymentInstrumentList.size());
    performanceLog(startTime, "ROLLBACK_INSTRUMENTS");
  }

  private void sendToQueueError(Exception e, MessageBuilder<?> errorMessage, String server,
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

  private void performanceLog(long startTime, String service) {
    log.info(
        "[PERFORMANCE_LOG] [{}] Time occurred to perform business logic: {} ms",
        service,
        System.currentTimeMillis() - startTime);
  }

  @Override
  public InstrumentDetailDTO getInstrumentInitiativesDetail(String idWallet, String userId){
    long startTime = System.currentTimeMillis();

    InstrumentDetailDTO instrumentDetailDTO = new InstrumentDetailDTO();

    log.info("[GET_INSTRUMENT_INITIATIVES_DETAIL] Searching all instrument with idWallet: {}", idWallet);
    List<PaymentInstrument> instrumentList = paymentInstrumentRepository.findByIdWalletAndUserId(idWallet, userId);

    if (instrumentList.isEmpty()){
      log.info("[GET_INSTRUMENT_INITIATIVES_DETAIL] Getting info of payment instrument for user: {}", userId);
      PaymentMethodInfoList paymentInfo = this.getPaymentMethodInfoList(userId, idWallet, new ArrayList<>());
      instrumentDetailDTO.setMaskedPan(paymentInfo.getMaskedPan());
      instrumentDetailDTO.setBrand(paymentInfo.getBrand());
      instrumentDetailDTO.setInitiativeList(new ArrayList<>());
      performanceLog(startTime, "GET_INSTRUMENT_INITIATIVES_DETAIL");
      return instrumentDetailDTO;
    }

    instrumentDetailDTO.setMaskedPan(instrumentList.get(0).getMaskedPan());
    instrumentDetailDTO.setBrand(instrumentList.get(0).getBrand());

    List<StatusOnInitiativeDTO> initiativeList = new ArrayList<>();
    for (PaymentInstrument instr : instrumentList){
      StatusOnInitiativeDTO statusOnInitiativeDTO = new StatusOnInitiativeDTO();
      statusOnInitiativeDTO.setIdInstrument(instr.getId());
      statusOnInitiativeDTO.setInitiativeId(instr.getInitiativeId());
      statusOnInitiativeDTO.setStatus(instr.getStatus());
      if (instr.getStatus().equals(PaymentInstrumentConstants.STATUS_PENDING_RE)
              || instr.getStatus().equals(PaymentInstrumentConstants.STATUS_PENDING_RTD)){
        statusOnInitiativeDTO.setStatus(PaymentInstrumentConstants.STATUS_PENDING_ENROLLMENT_REQUEST);
      }
      if (instr.getStatus().equals(PaymentInstrumentConstants.STATUS_ENROLLMENT_FAILED_KO_RE)){
        statusOnInitiativeDTO.setStatus(PaymentInstrumentConstants.STATUS_ENROLLMENT_FAILED);
      }
      initiativeList.add(statusOnInitiativeDTO);
    }
    instrumentDetailDTO.setInitiativeList(initiativeList);

    performanceLog(startTime, "GET_INSTRUMENT_INITIATIVES_DETAIL");
    return instrumentDetailDTO;
  }
}