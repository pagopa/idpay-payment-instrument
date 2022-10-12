package it.gov.pagopa.payment.instrument.connector;

import org.springframework.web.bind.annotation.RequestHeader;

public interface PMRestClientConnector {
  String getWalletList(@RequestHeader("Fiscal-Code") String userId);
}
