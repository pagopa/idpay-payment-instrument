package it.gov.pagopa.payment.instrument.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
@Data
@AllArgsConstructor
public class StatusOnInitiativeDTO {
    String initiativeId;
    String idInstrument;
    String status;
}
