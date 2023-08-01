package it.gov.pagopa.payment.instrument.service;

import it.gov.pagopa.payment.instrument.dto.*;
import it.gov.pagopa.payment.instrument.dto.rtd.RTDEventsDTO;

import java.util.List;

public interface PaymentInstrumentService {

  void enrollInstrument(String initiativeId, String userId, String idWallet,
      String channel);

  void deactivateInstrument(String initiativeId, String userId, String instrumentId);
  void processRtdMessage(RTDEventsDTO dto);
  void deactivateAllInstruments(String initiativeId, String userId, String deactivationDate);
  HpanGetDTO getHpan(String initiativeId, String userId);
  void processAck(RuleEngineAckDTO ruleEngineAckDTO);
  HpanGetDTO getHpanFromIssuer(String initiativeId, String userId, String channel);
  void enrollFromIssuer(InstrumentIssuerDTO body);
  InstrumentDetailDTO getInstrumentInitiativesDetail(String idWallet, String userId, List<String> statusList);
  void rollback(String initiativeId, String userId);
  void processOperation(QueueCommandOperationDTO queueCommandOperationDTO);

}
