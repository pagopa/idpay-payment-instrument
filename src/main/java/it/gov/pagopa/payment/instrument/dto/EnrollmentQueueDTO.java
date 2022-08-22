package it.gov.pagopa.payment.instrument.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnrollmentQueueDTO {

  private String userId;
  private String initiativeId;
  private String hpan;
  private String channel;
  private String queueDate;

}
