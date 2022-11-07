package it.gov.pagopa.payment.instrument.dto.rtd;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
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

  String application;

  @JsonProperty("timestamp")
  @JsonAlias("deactivationDate")
  OffsetDateTime timestamp;

}

