package it.gov.pagopa.payment.instrument.test.fakers;

import it.gov.pagopa.payment.instrument.dto.InstrumentFromDiscountDTO;

public class InstrumentFromDiscountDTOFaker {

  public static InstrumentFromDiscountDTO mockInstance(Integer bias){
    return mockInstanceBuilder(bias).build();
  }

  public static InstrumentFromDiscountDTO.InstrumentFromDiscountDTOBuilder mockInstanceBuilder(Integer bias) {
    return InstrumentFromDiscountDTO.builder()
        .initiativeId("INITIATIVEID%d".formatted(bias))
        .userId("USERID%d".formatted(bias));
  }

}
