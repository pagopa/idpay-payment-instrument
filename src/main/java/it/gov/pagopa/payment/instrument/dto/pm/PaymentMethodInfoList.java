package it.gov.pagopa.payment.instrument.dto.pm;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentMethodInfoList {
  private String hpan;
  private String maskedPan;
  private String brandLogo;
}
