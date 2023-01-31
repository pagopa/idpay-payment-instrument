package it.gov.pagopa.payment.instrument.dto.rtd;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RTDEnrollAckDTO implements RTDEventsDTO {

  String type;
  String correlationId;
  RTDMessage data;

}

