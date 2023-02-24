package it.gov.pagopa.payment.instrument.service;

import it.gov.pagopa.payment.instrument.dto.HpanGetDTO;
import it.gov.pagopa.payment.instrument.dto.InstrumentDetailDTO;
import it.gov.pagopa.payment.instrument.dto.InstrumentIssuerDTO;
import it.gov.pagopa.payment.instrument.dto.RuleEngineAckDTO;
import it.gov.pagopa.payment.instrument.dto.rtd.RTDEventsDTO;
import it.gov.pagopa.payment.instrument.model.PaymentInstrument;
import java.util.List;

public interface PaymentInstrumentService {

  void enrollInstrument(String initiativeId, String userId, String idWallet,
      String channel);

  void deactivateInstrument(String initiativeId, String userId, String instrumentId);
  void processRtdMessage(RTDEventsDTO dto);
  void deactivateAllInstruments(String initiativeId, String userId, String deactivationDate);
  void rollbackInstruments(List<PaymentInstrument> paymentInstrumentList);
  HpanGetDTO getHpan(String initiativeId, String userId);
  void processAck(RuleEngineAckDTO ruleEngineAckDTO);
  HpanGetDTO getHpanFromIssuer(String initiativeId, String userId, String channel);
  void enrollFromIssuer(InstrumentIssuerDTO body);
  InstrumentDetailDTO getInstrumentInitiativesDetail(String userId, String idWallet);
}
