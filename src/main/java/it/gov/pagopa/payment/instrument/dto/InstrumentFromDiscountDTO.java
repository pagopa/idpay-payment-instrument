package it.gov.pagopa.payment.instrument.dto;

import it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstrumentFromDiscountDTO {
  @NotBlank(message = PaymentInstrumentConstants.ERROR_MANDATORY_FIELD)
  String userId;

  @NotBlank(message = PaymentInstrumentConstants.ERROR_MANDATORY_FIELD)
  String initiativeId;

}
