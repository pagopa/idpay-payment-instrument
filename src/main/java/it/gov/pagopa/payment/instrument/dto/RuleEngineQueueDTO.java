package it.gov.pagopa.payment.instrument.dto;

import java.time.LocalDateTime;
import java.util.List;
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
  private List<String> listHpan;
  private String operationType;
  private LocalDateTime operationDate;

}
