package it.gov.pagopa.payment.instrument.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class InstrumentDetailDTO {
    String maskedPan;
    String brand;
    List<StatusOnInitiativeDTO> initiativeList;
}
