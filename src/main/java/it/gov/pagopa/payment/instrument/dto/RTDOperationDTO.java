package it.gov.pagopa.payment.instrument.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RTDOperationDTO {

  private String userId;

  private String initiativeId;

  private String operationType;

  private String hpan;

  private String iban;

  private String email;

  private String channel;

  private LocalDateTime operationDate;

  private String application;
}
