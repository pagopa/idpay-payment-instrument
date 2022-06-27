package it.gov.pagopa.payment.instrument.dto;

import it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants;
import java.time.LocalDateTime;
import javax.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class EnrollmentBodyDTO {

  @NotBlank(message = PaymentInstrumentConstants.ERROR_MANDATORY_FIELD)
  String userId;

  @NotBlank(message = PaymentInstrumentConstants.ERROR_MANDATORY_FIELD)
  String initiativeId;

  @NotBlank(message = PaymentInstrumentConstants.ERROR_MANDATORY_FIELD)
  String hpan;

  @NotBlank(message = PaymentInstrumentConstants.ERROR_MANDATORY_FIELD)
  String channel;

  LocalDateTime activationDate;

}

