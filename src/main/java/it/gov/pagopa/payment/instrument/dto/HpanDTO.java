package it.gov.pagopa.payment.instrument.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HpanDTO {

  private String hpan;
  private String maskedPan;
  private String brandLogo;
  private String idWallet;
  private String instrumentId;
  private String channel;

}
