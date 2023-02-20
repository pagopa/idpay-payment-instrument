package it.gov.pagopa.payment.instrument.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WalletDTO {

  private String initiativeId;
  private String userId;
  private String hpan;
  private String brandLogo;
  private String brand;
  private String circuitType;
  private String maskedPan;
}
