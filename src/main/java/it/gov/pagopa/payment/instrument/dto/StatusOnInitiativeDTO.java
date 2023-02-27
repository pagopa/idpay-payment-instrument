package it.gov.pagopa.payment.instrument.dto;

import lombok.Data;
@Data
public class StatusOnInitiativeDTO {
    String initiativeId;
    String idInstrument;
    String status;
}
