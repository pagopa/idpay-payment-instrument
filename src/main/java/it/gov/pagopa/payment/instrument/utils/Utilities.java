package it.gov.pagopa.payment.instrument.utils;

import feign.FeignException;
import it.gov.pagopa.common.web.dto.ErrorDTO;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import static it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants.ExceptionCode.GENERIC_ERROR;

@Component
public class Utilities {
  private final JsonMapper objectMapper;

  public Utilities(JsonMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public ErrorDTO exceptionErrorDTOConverter(FeignException e) {
    ErrorDTO errorDTO;
    try {
      errorDTO = objectMapper.readValue(e.contentUTF8(), ErrorDTO.class);
    } catch (JacksonException ex) {
      errorDTO = new ErrorDTO(GENERIC_ERROR, null);}
    return errorDTO;
  }

}
