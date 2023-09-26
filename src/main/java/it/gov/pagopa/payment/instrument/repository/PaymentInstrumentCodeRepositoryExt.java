package it.gov.pagopa.payment.instrument.repository;

import it.gov.pagopa.payment.instrument.model.PaymentInstrumentCode;
import java.time.LocalDateTime;

public interface PaymentInstrumentCodeRepositoryExt {

  PaymentInstrumentCode updateCode(String userId, String code, LocalDateTime creationDate);

}
