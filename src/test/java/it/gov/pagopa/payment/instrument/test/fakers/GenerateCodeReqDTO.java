package it.gov.pagopa.payment.instrument.test.fakers;

import it.gov.pagopa.payment.instrument.dto.GenerateCodeReqDTO.GenerateCodeReqDTOBuilder;

public class GenerateCodeReqDTO {

  public static it.gov.pagopa.payment.instrument.dto.GenerateCodeReqDTO mockInstance(Integer bias, boolean initiativeId){
    return mockInstanceBuilder(bias, initiativeId).build();
  }

  public static GenerateCodeReqDTOBuilder mockInstanceBuilder(Integer bias, boolean initiativeId) {
    final GenerateCodeReqDTOBuilder generateCodeDTOBuilder = it.gov.pagopa.payment.instrument.dto.GenerateCodeReqDTO.builder();
    if (initiativeId) {
      generateCodeDTOBuilder.initiativeId("INITIATIVEID%d".formatted(bias)).build();
    } else {
      generateCodeDTOBuilder.initiativeId("").build();
    }
    return generateCodeDTOBuilder;
  }
}

