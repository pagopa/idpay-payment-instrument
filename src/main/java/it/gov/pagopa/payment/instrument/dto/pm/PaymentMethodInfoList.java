package it.gov.pagopa.payment.instrument.dto.pm;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PaymentMethodInfoList {
  private String hpan;
  private String maskedPan;
  private String brandLogo;
  private boolean consent;
}
