package it.gov.pagopa.payment.instrument.dto.pm;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaymentMethodInfo {
  CardInfo cardInfo;
  SatispayInfo satispayInfo;
  BPayInfo bPayInfo;
}
