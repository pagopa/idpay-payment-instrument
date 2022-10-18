package it.gov.pagopa.payment.instrument.connector;

import it.gov.pagopa.payment.instrument.dto.pm.WalletV2ListResponse;
import org.springframework.web.bind.annotation.RequestHeader;

public interface PMRestClientConnector {
  WalletV2ListResponse getWalletList(@RequestHeader("Fiscal-Code") String userId);
}
