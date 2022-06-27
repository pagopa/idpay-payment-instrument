package it.gov.pagopa.payment.instrument.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class PaymentInstrumentException extends RuntimeException {

  private final int code;

  private final String message;

}
