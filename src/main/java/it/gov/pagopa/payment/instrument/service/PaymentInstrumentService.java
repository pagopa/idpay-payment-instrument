package it.gov.pagopa.payment.instrument.service;

import it.gov.pagopa.payment.instrument.dto.DeactivationPMBodyDTO;
import it.gov.pagopa.payment.instrument.dto.HpanGetDTO;
import it.gov.pagopa.payment.instrument.dto.pm.PaymentMethodInfoList;
import it.gov.pagopa.payment.instrument.model.PaymentInstrument;
import java.time.LocalDateTime;
import java.util.List;

public interface PaymentInstrumentService {

  PaymentMethodInfoList enrollInstrument(String initiativeId, String userId, String idWallet,
      String channel, LocalDateTime activationDate);

  PaymentMethodInfoList deactivateInstrument(String initiativeId, String userId, String instrumentId, LocalDateTime deactivationDate);
  void deactivateInstrumentPM(DeactivationPMBodyDTO dto);
  void deactivateAllInstrument(String initiativeId, String userId, String deactivationDate);
  void rollbackInstruments(List<PaymentInstrument> paymentInstrumentList);

  int countByInitiativeIdAndUserIdAndStatus(String initiativeId, String userId, String status);

  HpanGetDTO gethpan(String initiativeId, String userId);

}
