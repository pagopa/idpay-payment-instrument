package it.gov.pagopa.payment.instrument.test.fakers;

import it.gov.pagopa.payment.instrument.dto.InstrumentFromDiscountDTO;
import it.gov.pagopa.payment.instrument.model.PaymentInstrument;

public class PaymentInstrumentFaker {

  public static PaymentInstrument mockInstance(Integer bias){
    return mockInstanceBuilder(bias).build();
  }

  public static PaymentInstrument.PaymentInstrumentBuilder mockInstanceBuilder(Integer bias) {
    return PaymentInstrument.builder()
        .initiativeId("INITIATIVEID%d".formatted(bias))
        .userId("USERID%d".formatted(bias))
        .channel("CHANNEL%d".formatted(bias))
        .hpan("HPAN%d".formatted(bias));
  }

}
