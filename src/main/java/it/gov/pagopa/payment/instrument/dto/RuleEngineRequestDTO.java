package it.gov.pagopa.payment.instrument.dto;

import it.gov.pagopa.payment.instrument.dto.pm.PaymentMethodInfoList;
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
public class RuleEngineRequestDTO {

  private String userId;
  private String initiativeId;
  private String channel;
  private List<PaymentMethodInfoList> infoList;
  private String operationType;
  private LocalDateTime operationDate;

}
