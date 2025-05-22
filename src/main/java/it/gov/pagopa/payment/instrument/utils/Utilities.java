package it.gov.pagopa.payment.instrument.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import it.gov.pagopa.common.web.dto.ErrorDTO;
import org.springframework.stereotype.Component;

import static it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants.ExceptionCode.GENERIC_ERROR;

@Component
public class Utilities {
  private final ObjectMapper objectMapper;

  public Utilities(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public ErrorDTO exceptionErrorDTOConverter(FeignException e) {
    ErrorDTO errorDTO;
    try {
      errorDTO = objectMapper.readValue(e.contentUTF8(), ErrorDTO.class);
    } catch (JsonProcessingException ex) {
      errorDTO = new ErrorDTO(GENERIC_ERROR, null);}
    return errorDTO;

  }

}
