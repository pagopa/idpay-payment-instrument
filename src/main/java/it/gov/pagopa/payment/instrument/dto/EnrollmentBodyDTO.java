package it.gov.pagopa.payment.instrument.dto;

import it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class  EnrollmentBodyDTO extends BaseEnrollmentBodyDTO{

  @NotBlank(message = PaymentInstrumentConstants.ERROR_MANDATORY_FIELD)
  String idWallet;

  public EnrollmentBodyDTO(String userId, String initiativeId,String idWallet, String channel, String instrumentType) {
    super(userId, initiativeId, channel, instrumentType);
    this.idWallet = idWallet;
  }
}

