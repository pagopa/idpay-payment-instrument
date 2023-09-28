package it.gov.pagopa.payment.instrument.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CheckEnrollmentDTO {
  @JsonProperty("isIdPayCodeEnabled")
  private boolean isIdPayCodeEnabled;

}
