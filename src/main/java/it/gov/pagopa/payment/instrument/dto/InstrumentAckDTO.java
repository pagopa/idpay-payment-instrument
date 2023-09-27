package it.gov.pagopa.payment.instrument.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InstrumentAckDTO {

  String initiativeId;

  String userId;

  String channel;

  String instrumentType;

  String brandLogo;

  String brand;

  String maskedPan;

  String operationType;

  LocalDateTime operationDate;

  Integer ninstr;
}
