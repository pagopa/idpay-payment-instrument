package it.gov.pagopa.payment.instrument.connector;

import it.gov.pagopa.payment.instrument.dto.InstrumentAckDTO;
import it.gov.pagopa.payment.instrument.dto.WalletCallDTO;
import org.springframework.stereotype.Service;

@Service
public class WalletRestConnectorImpl implements WalletRestConnector {

  private final WalletRestClient walletRestClient;

  public WalletRestConnectorImpl(WalletRestClient paymentInstrumentRestClient) {
    this.walletRestClient = paymentInstrumentRestClient;
  }

  @Override
  public void updateWallet(WalletCallDTO body) {
    walletRestClient.updateWallet(body);
  }

  @Override
  public void processAck(InstrumentAckDTO body) {
    walletRestClient.processAck(body);
  }

}
