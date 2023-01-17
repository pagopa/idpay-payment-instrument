package it.gov.pagopa.payment.instrument.dto.rtd;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RTDOperationDTO {

  private List<RTDHpanListDTO> hpanList;
  private String correlationId;
  private String operationType;
  private String application;
}

