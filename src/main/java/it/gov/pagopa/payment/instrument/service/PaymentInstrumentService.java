package it.gov.pagopa.payment.instrument.service;

import java.time.LocalDateTime;

public interface PaymentInstrumentService {

  void enrollInstrument(String initiativeId, String userId, String hpan,
      String channel, LocalDateTime activationDate);

  void deactivateInstrument(String initiativeId, String userId, String hpan, LocalDateTime deactivationDate);

}
