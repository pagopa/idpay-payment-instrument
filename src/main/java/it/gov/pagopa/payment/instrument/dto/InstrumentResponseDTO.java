package it.gov.pagopa.payment.instrument.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class InstrumentResponseDTO {

  Integer ninstr = null;
  String brandLogo;
  String maskedPan;

}

