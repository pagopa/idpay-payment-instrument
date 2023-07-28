package it.gov.pagopa.payment.instrument.dto;

import it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EnrollmentBodyDTO {

  @NotBlank(message = PaymentInstrumentConstants.ERROR_MANDATORY_FIELD)
  String userId;

  @NotBlank(message = PaymentInstrumentConstants.ERROR_MANDATORY_FIELD)
  String initiativeId;

  @NotBlank(message = PaymentInstrumentConstants.ERROR_MANDATORY_FIELD)
  String idWallet;

  @NotBlank(message = PaymentInstrumentConstants.ERROR_MANDATORY_FIELD)
  String channel;

}

