package it.gov.pagopa.payment.instrument.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RuleEngineAckDTO {
  String initiativeId;
  String userId;
  String operationType;
  List<String> hpanList;
  List<String> rejectedHpanList;
  LocalDateTime timestamp;
}
