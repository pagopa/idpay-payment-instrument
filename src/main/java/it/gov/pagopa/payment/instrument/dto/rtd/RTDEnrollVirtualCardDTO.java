package it.gov.pagopa.payment.instrument.dto.rtd;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RTDEnrollVirtualCardDTO implements RTDEventsDTO {

  String type;

  RTDMessage rtdMessage;

}

