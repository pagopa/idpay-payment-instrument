package it.gov.pagopa.payment.instrument.connector;

import it.gov.pagopa.payment.instrument.dto.WalletCallDTO;
import org.springframework.web.bind.annotation.RequestBody;

public interface WalletRestConnector {

  void updateWallet(@RequestBody WalletCallDTO body);
}
