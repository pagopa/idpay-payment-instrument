package it.gov.pagopa.payment.instrument.repository;

import java.time.LocalDateTime;

public interface PaymentInstrumentCodeRepositoryExt {

  void updateCode(String userId, String code, LocalDateTime creationDate);

}
