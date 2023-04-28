package it.gov.pagopa.payment.instrument.service;

import it.gov.pagopa.payment.instrument.dto.InstrumentFromDiscountDTO;

public interface PaymentInstrumentDiscountService {

  void enrollDiscountInitiative(InstrumentFromDiscountDTO body);
}
