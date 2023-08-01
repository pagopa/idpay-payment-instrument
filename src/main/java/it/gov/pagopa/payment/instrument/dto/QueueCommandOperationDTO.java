package it.gov.pagopa.payment.instrument.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@Builder
public class QueueCommandOperationDTO {
    private String operationType;
    private String operationId;
    private LocalDateTime operationTime;
}
