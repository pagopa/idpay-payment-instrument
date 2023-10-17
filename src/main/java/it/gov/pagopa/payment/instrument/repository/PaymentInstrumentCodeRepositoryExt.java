package it.gov.pagopa.payment.instrument.repository;

import java.time.LocalDateTime;

public interface PaymentInstrumentCodeRepositoryExt {

  void updateCode(String userId, String idpayCode, String salt, String secondFactor, String keyId, LocalDateTime creationDate);

}
