package it.gov.pagopa.payment.instrument.dto.rtd;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RTDRevokeTokenDTO implements RTDEventsDTO {

  String type;

  RTDMessage data;

}

