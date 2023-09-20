package it.gov.pagopa.payment.instrument.connector;

import it.gov.pagopa.payment.instrument.dto.InstrumentAckDTO;
import it.gov.pagopa.payment.instrument.dto.WalletCallDTO;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public interface WalletRestConnector {

  void updateWallet(@RequestBody WalletCallDTO body);
  void processAck(@RequestBody InstrumentAckDTO body);
  void enrollInstrumentCode(@PathVariable("initiativeId") String initiativeId, @PathVariable("userId") String userId);
}
