package it.gov.pagopa.payment.instrument.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RuleEngineQueueDTO {

  private String userId;
  private String initiativeId;
  private String hpan;
  private String operationType;
  private LocalDateTime operationDate;

}
