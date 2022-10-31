package it.gov.pagopa.payment.instrument.dto;

import it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants;
import javax.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DeactivationPMBodyDTO {

  @NotBlank(message = PaymentInstrumentConstants.ERROR_MANDATORY_FIELD)
  String fiscalCode;


  @NotBlank(message = PaymentInstrumentConstants.ERROR_MANDATORY_FIELD)
  String hashPan;

  String deactivationDate;

}

