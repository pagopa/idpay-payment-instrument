package it.gov.pagopa.payment.instrument.test.fakers;

import it.gov.pagopa.payment.instrument.dto.GenerateCodeDTO;

public class GenerateCodeDTOFaker {

  public static GenerateCodeDTO mockInstance(Integer bias, boolean initiativeId){
    return mockInstanceBuilder(bias, initiativeId).build();
  }

  public static GenerateCodeDTO.GenerateCodeDTOBuilder mockInstanceBuilder(Integer bias, boolean initiativeId) {
    final GenerateCodeDTO.GenerateCodeDTOBuilder generateCodeDTOBuilder = GenerateCodeDTO.builder();
    if (initiativeId) {
      generateCodeDTOBuilder.initiativeId("INITIATIVEID%d".formatted(bias)).build();
    } else {
      generateCodeDTOBuilder.initiativeId("").build();
    }
    return generateCodeDTOBuilder;
  }
}

