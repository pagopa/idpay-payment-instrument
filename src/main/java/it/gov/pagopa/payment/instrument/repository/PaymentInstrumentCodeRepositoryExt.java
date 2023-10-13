package it.gov.pagopa.payment.instrument.repository;

import it.gov.pagopa.payment.instrument.model.PaymentInstrumentCode;
import java.time.LocalDateTime;

public interface PaymentInstrumentCodeRepositoryExt {

  void updateCode(String userId, String idpayCode, String salt, String secondFactor, String keyId, LocalDateTime creationDate);

  PaymentInstrumentCode deleteInstrument(String userId);

}
