package it.gov.pagopa.payment.instrument.service;

import feign.FeignException;
import it.gov.pagopa.payment.instrument.connector.DecryptRestConnector;
import it.gov.pagopa.payment.instrument.connector.EncryptRestConnector;
import it.gov.pagopa.payment.instrument.connector.PMRestClientConnector;
import it.gov.pagopa.payment.instrument.connector.WalletRestConnector;
import it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants;
import it.gov.pagopa.payment.instrument.dto.CFDTO;
import it.gov.pagopa.payment.instrument.dto.DeactivationPMBodyDTO;
import it.gov.pagopa.payment.instrument.dto.DecryptCfDTO;
import it.gov.pagopa.payment.instrument.dto.EncryptedCfDTO;
import it.gov.pagopa.payment.instrument.dto.HpanDTO;
import it.gov.pagopa.payment.instrument.dto.HpanGetDTO;
import it.gov.pagopa.payment.instrument.dto.InstrumentAckDTO;
import it.gov.pagopa.payment.instrument.dto.RTDOperationDTO;
import it.gov.pagopa.payment.instrument.dto.RuleEngineAckDTO;
import it.gov.pagopa.payment.instrument.dto.RuleEngineQueueDTO;
import it.gov.pagopa.payment.instrument.dto.WalletCallDTO;
import it.gov.pagopa.payment.instrument.dto.WalletDTO;
import it.gov.pagopa.payment.instrument.dto.mapper.AckMapper;
import it.gov.pagopa.payment.instrument.dto.mapper.MessageMapper;
import it.gov.pagopa.payment.instrument.dto.pm.PaymentMethodInfoList;
import it.gov.pagopa.payment.instrument.dto.pm.WalletV2;
import it.gov.pagopa.payment.instrument.dto.pm.WalletV2ListResponse;
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
  @Autowired
  DecryptRestConnector decryptRestConnector;
  @Autowired
  private EncryptRestConnector encryptRestConnector;
  @Autowired
  private WalletRestConnector walletRestConnector;
  @Autowired
  private AckMapper ackMapper;


  @Override
  public void enrollInstrument(String initiativeId, String userId, String idWallet,
      String channel) {

    List<PaymentInstrument> instrumentList = paymentInstrumentRepository.findByIdWalletAndStatusNotContaining(
        idWallet, PaymentInstrumentConstants.STATUS_INACTIVE);
    List<PaymentMethodInfoList> paymentMethodInfoList = new ArrayList<>();

    for (PaymentInstrument pi : instrumentList) {
      if (!pi.getUserId().equals(userId)) {
        log.error(
            "[ENROLL_INSTRUMENT] The Payment Instrument is already in use by another citizen.");
        throw new PaymentInstrumentException(HttpStatus.FORBIDDEN.value(),
            PaymentInstrumentConstants.ERROR_PAYMENT_INSTRUMENT_ALREADY_ACTIVE);
      }

      if (pi.getInitiativeId().equals(initiativeId)) {
        log.info(
            "[ENROLL_INSTRUMENT] The Payment Instrument is already active, or there is a pending request on it.");
        return;
      }
    }

    PaymentMethodInfoList infoList = getPaymentMethodInfoList(
        userId, idWallet, paymentMethodInfoList);

    PaymentInstrument newInstrument = PaymentInstrument.builder()
        .initiativeId(initiativeId)
        .userId(userId)
        .idWallet(idWallet)
        .hpan(infoList.getHpan())
        .maskedPan(infoList.getMaskedPan())
        .brandLogo(infoList.getBrandLogo())
        .status(PaymentInstrumentConstants.STATUS_PENDING_ENROLLMENT_REQUEST)
        .channel(channel)
        .build();
    paymentInstrumentRepository.save(newInstrument);

    try {
      sendToRuleEngine(newInstrument.getUserId(), newInstrument.getInitiativeId(),
          paymentMethodInfoList,
          PaymentInstrumentConstants.OPERATION_ADD);
    } catch (Exception e) {
      log.info(
          "[ENROLL_INSTRUMENT] Couldn't send to Rule Engine: resetting the Payment Instrument.");
      paymentInstrumentRepository.delete(newInstrument);
      throw new PaymentInstrumentException(HttpStatus.BAD_REQUEST.value(), e.getMessage());
    }
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
      Instant finish = Instant.now();
      long time = Duration.between(start, finish).toMillis();
      log.info("PM's call finished at: " + finish + " The PM service took: " + time + "ms");
    } catch (FeignException e) {
      throw new PaymentInstrumentException(HttpStatus.INTERNAL_SERVER_ERROR.value(),
          e.getMessage());
    }

    int countIdWallet = 0;

    for (WalletV2 v2 : walletV2ListResponse.getData()) {
      if (v2.getIdWallet().equals(idWallet)) {
        switch (v2.getWalletType()) {
          case PaymentInstrumentConstants.SATISPAY -> {
            infoList.setHpan(v2.getInfo().getUuid());
            infoList.setBrandLogo(v2.getInfo().getBrandLogo());
            paymentMethodInfoList.add(infoList);
          }
          case PaymentInstrumentConstants.BPAY -> {
            infoList.setHpan(v2.getInfo().getUidHash());
            infoList.setMaskedPan(v2.getInfo().getNumberObfuscated());
            infoList.setBrandLogo(v2.getInfo().getBrandLogo());
            paymentMethodInfoList.add(infoList);
          }
          default -> {
            infoList.setHpan(v2.getInfo().getHashPan());
            infoList.setMaskedPan(v2.getInfo().getBlurredNumber());
            infoList.setBrandLogo(v2.getInfo().getBrandLogo());
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
  public void deactivateAllInstrument(String initiativeId, String userId, String deactivationDate) {
    List<PaymentInstrument> paymentInstrumentList = paymentInstrumentRepository.findByInitiativeIdAndUserIdAndStatus(
        initiativeId, userId, PaymentInstrumentConstants.STATUS_ACTIVE);

    PaymentMethodInfoList infoList = new PaymentMethodInfoList();
    List<PaymentMethodInfoList> paymentMethodInfoList = new ArrayList<>();
    List<String> hpanList = new ArrayList<>();

    for (PaymentInstrument paymentInstrument : paymentInstrumentList) {
      paymentInstrument.setDeactivationDate(LocalDateTime.parse(deactivationDate));
      paymentInstrument.setStatus(PaymentInstrumentConstants.STATUS_INACTIVE);
      paymentInstrument.setDeleteChannel(PaymentInstrumentConstants.IO);
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
  public void deactivateInstrument(String initiativeId, String userId,
      String instrumentId) {
    log.info("[DEACTIVATE_INSTRUMENT] Deleting instrument");

    PaymentInstrument instrument = paymentInstrumentRepository.findByInitiativeIdAndUserIdAndId(
        initiativeId, userId, instrumentId).orElse(null);

    if (instrument == null) {
      throw new PaymentInstrumentException(HttpStatus.NOT_FOUND.value(),
          PaymentInstrumentConstants.ERROR_PAYMENT_INSTRUMENT_NOT_FOUND);
    }

    if (instrument.getStatus().equals(PaymentInstrumentConstants.STATUS_ACTIVE)) {
      instrument.setStatus(PaymentInstrumentConstants.STATUS_PENDING_DEACTIVATION_REQUEST);
      instrument.setDeleteChannel(PaymentInstrumentConstants.IO);
      paymentInstrumentRepository.save(instrument);
      PaymentMethodInfoList infoList = new PaymentMethodInfoList(instrument.getHpan(),
          instrument.getMaskedPan(), instrument.getBrandLogo());
      try {
        sendToRuleEngine(userId, initiativeId,
            List.of(infoList),
            PaymentInstrumentConstants.OPERATION_DELETE);
      } catch (Exception e) {
        log.info(
            "[DEACTIVATE_INSTRUMENT] Couldn't send to Rule Engine: resetting the Payment Instrument.");
        instrument.setStatus(PaymentInstrumentConstants.STATUS_ACTIVE);
        paymentInstrumentRepository.save(instrument);
        throw new PaymentInstrumentException(HttpStatus.BAD_REQUEST.value(), e.getMessage());
      }
    }
  }

  @Override
  public void deactivateInstrumentPM(DeactivationPMBodyDTO dto) {
    log.info("[DEACTIVATE_INSTRUMENT_PM] Delete instrument from PM");
    EncryptedCfDTO encryptedCfDTO = new EncryptedCfDTO();

    try {
      encryptedCfDTO = encryptRestConnector.upsertToken(
          new CFDTO(dto.getFiscalCode()));
      log.info(String.valueOf(System.currentTimeMillis()));
    } catch (Exception e) {
      log.info("Error PDV - Encrypt ");
    }
    List<PaymentInstrument> instruments = paymentInstrumentRepository.findByHpanAndUserIdAndStatus(
        dto.getHashPan(), encryptedCfDTO.getToken(), PaymentInstrumentConstants.STATUS_ACTIVE);
    if (instruments.isEmpty()) {
      log.info("[DEACTIVATE_INSTRUMENT_PM] No instrument to delete");
      throw new PaymentInstrumentException(HttpStatus.NOT_FOUND.value(),
          PaymentInstrumentConstants.ERROR_PAYMENT_INSTRUMENT_NOT_FOUND);
    }
    List<WalletDTO> walletDTOS = new ArrayList<>();
    for (PaymentInstrument instrument : instruments) {
      WalletDTO walletDTO = new WalletDTO(instrument.getInitiativeId(), instrument.getUserId(),
          instrument.getHpan(),
          instrument.getBrandLogo(), instrument.getMaskedPan());
      walletDTOS.add(walletDTO);
    }
    walletRestConnector.updateWallet(new WalletCallDTO(walletDTOS));
    instruments.forEach(instrument ->
        checkAndDelete(instrument, LocalDateTime.parse(dto.getDeactivationDate())));
  }

  private void checkAndDelete(PaymentInstrument instrument,
      LocalDateTime deactivationDate) {
    PaymentMethodInfoList infoList = new PaymentMethodInfoList();

    if (instrument.getStatus().equals(PaymentInstrumentConstants.STATUS_INACTIVE)) {
      return;
    }

    instrument.setStatus(PaymentInstrumentConstants.STATUS_INACTIVE);
    instrument.setDeactivationDate(deactivationDate);
    instrument.setDeleteChannel(PaymentInstrumentConstants.PM);
    paymentInstrumentRepository.save(instrument);

    infoList.setHpan(instrument.getHpan());
    infoList.setMaskedPan(instrument.getMaskedPan());
    infoList.setBrandLogo(instrument.getBrandLogo());

    List<PaymentMethodInfoList> paymentMethodInfoList = List.of(infoList);
    List<String> hpanList = Arrays.asList(instrument.getHpan());
    try {
      sendToRuleEngine(instrument.getUserId(), instrument.getInitiativeId(),
          paymentMethodInfoList, PaymentInstrumentConstants.OPERATION_DELETE);
    } catch (Exception exception) {
      this.sendToQueueError(exception, hpanList, PaymentInstrumentConstants.OPERATION_DELETE);
    }
    sendToRtd(hpanList, PaymentInstrumentConstants.OPERATION_DELETE);
  }

  private void sendToRuleEngine(String userId, String initiativeId, List<PaymentMethodInfoList>
      paymentMethodInfoList, String operation) {

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

      try {
        rtdProducer.sendInstrument(rtdOperationDTO);
      } catch (Exception exception) {
        this.sendToQueueError(exception, hpanList, operation);
      }
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
      hpanDTO.setBrandLogo(paymentInstruments.getBrandLogo());
      hpanDTO.setMaskedPan(paymentInstruments.getMaskedPan());
      hpanDTO.setInstrumentId(paymentInstruments.getId());
      hpanDTO.setIdWallet(paymentInstruments.getIdWallet());
      hpanDTOList.add(hpanDTO);
    }
    hpanGetDTO.setHpanList(hpanDTOList);

    return hpanGetDTO;
  }

  @Override
  public void processAck(RuleEngineAckDTO ruleEngineAckDTO) {
    log.info("[PROCESS_ACK] Processing new message.");

    if (ruleEngineAckDTO.getOperationType().equals(PaymentInstrumentConstants.OPERATION_ADD)) {
      log.info("[PROCESS_ACK] Processing ACK for an enrollment request.");
      processAckEnroll(ruleEngineAckDTO);
    }

    if (ruleEngineAckDTO.getOperationType().equals(PaymentInstrumentConstants.OPERATION_DELETE)) {
      log.info("[PROCESS_ACK] Processing ACK for a deactivation request.");
      processAckDeactivate(ruleEngineAckDTO);
    }
  }

  private void processAckDeactivate(RuleEngineAckDTO ruleEngineAckDTO) {

    String hpan =
        (!ruleEngineAckDTO.getHpanList().isEmpty()) ? ruleEngineAckDTO.getHpanList().get(0)
            : ruleEngineAckDTO.getRejectedHpanList().get(0);

    String status =
        (!ruleEngineAckDTO.getHpanList().isEmpty()) ? PaymentInstrumentConstants.STATUS_INACTIVE
            : PaymentInstrumentConstants.STATUS_ACTIVE;

    PaymentInstrument instrument = paymentInstrumentRepository.findByInitiativeIdAndUserIdAndHpanAndStatus(
            ruleEngineAckDTO.getInitiativeId(), ruleEngineAckDTO.getUserId(),
            hpan, PaymentInstrumentConstants.STATUS_PENDING_DEACTIVATION_REQUEST)
        .orElse(null);

    if (instrument == null) {
      log.info("[PROCESS_ACK_DEACTIVATE] No pending deactivation requests found for this ACK.");
      return;
    }
    if (!ruleEngineAckDTO.getHpanList().isEmpty()) {
      log.info("[PROCESS_ACK_DEACTIVATE] Deactivation OK: sending to RTD.");

      sendToRtd(List.of(hpan), ruleEngineAckDTO.getOperationType());

      log.info("[PROCESS_ACK_DEACTIVATE] Deactivation OK: updating instrument status to {}.",
          PaymentInstrumentConstants.STATUS_INACTIVE);

      instrument.setDeactivationDate(ruleEngineAckDTO.getTimestamp());

    }
    instrument.setStatus(status);
    paymentInstrumentRepository.save(instrument);

    int nInstr = countByInitiativeIdAndUserIdAndStatus(instrument.getInitiativeId(),
        instrument.getUserId(), PaymentInstrumentConstants.STATUS_ACTIVE);

    InstrumentAckDTO dto = ackMapper.ackToWallet(ruleEngineAckDTO, instrument.getDeleteChannel(),
        instrument.getMaskedPan(), instrument.getBrandLogo(), nInstr);

    log.info("[PROCESS_ACK_DEACTIVATE] Deactivation OK: updating wallet.");

    walletRestConnector.processAck(dto);
  }

  private void processAckEnroll(RuleEngineAckDTO ruleEngineAckDTO) {

    String hpan =
        (!ruleEngineAckDTO.getHpanList().isEmpty()) ? ruleEngineAckDTO.getHpanList().get(0)
            : ruleEngineAckDTO.getRejectedHpanList().get(0);

    String status =
        (!ruleEngineAckDTO.getHpanList().isEmpty()) ? PaymentInstrumentConstants.STATUS_ACTIVE
            : PaymentInstrumentConstants.STATUS_FAILED_ENROLLMENT_REQUEST;

    PaymentInstrument instrument = paymentInstrumentRepository.findByInitiativeIdAndUserIdAndHpanAndStatus(
            ruleEngineAckDTO.getInitiativeId(), ruleEngineAckDTO.getUserId(),
            hpan, PaymentInstrumentConstants.STATUS_PENDING_ENROLLMENT_REQUEST)
        .orElse(null);

    if (instrument == null) {
      log.info("[PROCESS_ACK_ENROLL] No pending enrollment requests found for this ACK.");
      return;
    }
    if (!ruleEngineAckDTO.getHpanList().isEmpty()) {
      log.info("[PROCESS_ACK_ENROLL] Enrollment OK: sending to RTD.");

      sendToRtd(List.of(hpan), ruleEngineAckDTO.getOperationType());

      log.info("[PROCESS_ACK_ENROLL] Enrollment OK: updating instrument status to {}.",
          PaymentInstrumentConstants.STATUS_ACTIVE);

      instrument.setActivationDate(ruleEngineAckDTO.getTimestamp());

    }
    instrument.setStatus(status);
    paymentInstrumentRepository.save(instrument);

    int nInstr = countByInitiativeIdAndUserIdAndStatus(instrument.getInitiativeId(),
        instrument.getUserId(), PaymentInstrumentConstants.STATUS_ACTIVE);

    InstrumentAckDTO dto = ackMapper.ackToWallet(ruleEngineAckDTO, instrument.getChannel(),
        instrument.getMaskedPan(), instrument.getBrandLogo(), nInstr);

    log.info("[PROCESS_ACK_ENROLL] Enrollment OK: updating wallet.");

    walletRestConnector.processAck(dto);

  }

  @Override
  public void rollbackInstruments(List<PaymentInstrument> paymentInstrumentList) {
    for (PaymentInstrument instrument : paymentInstrumentList) {
      instrument.setStatus(PaymentInstrumentConstants.STATUS_ACTIVE);
      instrument.setDeactivationDate(null);
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