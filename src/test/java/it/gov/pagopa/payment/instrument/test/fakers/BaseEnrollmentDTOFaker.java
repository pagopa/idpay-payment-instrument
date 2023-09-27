package it.gov.pagopa.payment.instrument.test.fakers;

import it.gov.pagopa.payment.instrument.dto.BaseEnrollmentBodyDTO;

public class BaseEnrollmentDTOFaker {

  public static BaseEnrollmentBodyDTO mockInstance(Integer bias){
    return mockInstanceBuilder(bias).build();
  }

  public static BaseEnrollmentBodyDTO.BaseEnrollmentBodyDTOBuilder mockInstanceBuilder(Integer bias) {
    return BaseEnrollmentBodyDTO.builder()
            .initiativeId("INITIATIVEID%d".formatted(bias))
            .userId("USERID%d".formatted(bias))
            .channel("CHANNEL%d".formatted(bias))
            .instrumentType("INSTRUMENTTYPE%d".formatted(bias));
  }

}
