package it.gov.pagopa.payment.instrument.service;

import it.gov.pagopa.payment.instrument.dto.HpanGetDTO;
import it.gov.pagopa.payment.instrument.model.PaymentInstrument;
import java.time.LocalDateTime;
import java.util.List;

public interface PaymentInstrumentService {

  void enrollInstrument(String initiativeId, String userId, String hpan,
      String channel, LocalDateTime activationDate);

  void deactivateInstrument(String initiativeId, String userId, String hpan, LocalDateTime deactivationDate);

  void deactivateAllInstrument(String initiativeId, String userId, String deactivationDate);
  void rollbackInstruments(List<PaymentInstrument> paymentInstrumentList);

  int countByInitiativeIdAndUserIdAndStatus(String initiativeId, String userId, String status);

  HpanGetDTO gethpan(String initiativeId, String userId);

}
