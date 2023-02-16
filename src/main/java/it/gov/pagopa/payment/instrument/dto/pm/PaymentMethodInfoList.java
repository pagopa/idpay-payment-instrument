package it.gov.pagopa.payment.instrument.dto.pm;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class PaymentMethodInfoList {
  private String hpan;
  private String maskedPan;
  private String brandLogo;
  private String circuitType;
  private boolean consent;
}
