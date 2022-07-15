package it.gov.pagopa.payment.instrument.dto;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class HpanGetDTO {

  private List<HpanDTO> hpanList;

}
