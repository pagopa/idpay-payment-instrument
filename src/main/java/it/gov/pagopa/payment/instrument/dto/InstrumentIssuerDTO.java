package it.gov.pagopa.payment.instrument.dto;

import it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants;
import javax.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class InstrumentIssuerDTO {

  @NotBlank(message = PaymentInstrumentConstants.ERROR_MANDATORY_FIELD)
  String initiativeId;

  @NotBlank(message = PaymentInstrumentConstants.ERROR_MANDATORY_FIELD)
  String userId;

  @NotBlank(message = PaymentInstrumentConstants.ERROR_MANDATORY_FIELD)
  String hpan;

  @NotBlank(message = PaymentInstrumentConstants.ERROR_MANDATORY_FIELD)
  String channel;

  String brandLogo;

  String brand;

  String maskedPan;
}
