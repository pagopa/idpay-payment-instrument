package it.gov.pagopa.payment.instrument.dto.rtd;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RTDMessage {

  String fiscalCode;

  String hpan;

  String htoken;

  String par;

  String application;

  OffsetDateTime timestamp;

  List<String> applications;

}

