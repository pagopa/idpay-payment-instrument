package it.gov.pagopa.payment.instrument.dto;

import it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants;
import java.time.LocalDateTime;
import javax.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DeactivationPMBodyDTO {

  @NotBlank(message = PaymentInstrumentConstants.ERROR_MANDATORY_FIELD)
  String fiscalCode;


  @NotBlank(message = PaymentInstrumentConstants.ERROR_MANDATORY_FIELD)
  String hpan;

  LocalDateTime deactivationDate;

}

