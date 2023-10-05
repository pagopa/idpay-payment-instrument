package it.gov.pagopa.payment.instrument.repository;

import it.gov.pagopa.payment.instrument.model.PaymentInstrumentCode;
import java.time.LocalDateTime;

public interface PaymentInstrumentCodeRepositoryExt {

  void updateCode(String userId, String code, String salt, String secondFactor, LocalDateTime creationDate);

  PaymentInstrumentCode deleteInstrument(String userId);

}
