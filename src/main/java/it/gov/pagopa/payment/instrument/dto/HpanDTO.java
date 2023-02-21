package it.gov.pagopa.payment.instrument.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(Include.NON_NULL)
public class HpanDTO {

  private String maskedPan;
  private String brandLogo;
  private String brand;
  private String idWallet;
  private String instrumentId;
  private String status;
  private String channel;
  private LocalDateTime activationDate;

}
