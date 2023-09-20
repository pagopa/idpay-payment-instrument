package it.gov.pagopa.payment.instrument.repository;

import it.gov.pagopa.payment.instrument.model.PaymentInstrument;

import java.util.List;

public interface PaymentInstrumentRepositoryExtended {
    List<PaymentInstrument> deletePaged(String initiativeId, int pageSize);
}
