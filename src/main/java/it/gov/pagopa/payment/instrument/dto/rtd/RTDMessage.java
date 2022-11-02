package it.gov.pagopa.payment.instrument.dto.rtd;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RTDMessage {

  String fiscalCode;

  String hpan;

  String htoken;

  String par;

  OffsetDateTime timestamp;

  OffsetDateTime deactivationDate;

}

