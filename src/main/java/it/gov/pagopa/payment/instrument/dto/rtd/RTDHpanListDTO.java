package it.gov.pagopa.payment.instrument.dto.rtd;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RTDHpanListDTO {

  private String hpan;
  private boolean consent;

}
