package it.gov.pagopa.payment.instrument.connector;

import it.gov.pagopa.payment.instrument.dto.pm.WalletV2ListResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PMRestClientConnectorImpl implements PMRestClientConnector {

  private final PMRestClient pmRestClient;
  private final String apimKey;
  private final String apimTrace;

  public PMRestClientConnectorImpl(PMRestClient pmRestClient,
      @Value("${rest-client.pm.apim-key}") String apimKey,
      @Value("${rest-client.pm.apim-trace}") String apimTrace) {
    this.pmRestClient = pmRestClient;
    this.apimKey = apimKey;
    this.apimTrace = apimTrace;
  }

  @Override
  public WalletV2ListResponse getWalletList(String userId) {
    return pmRestClient.getWalletList(apimKey, apimTrace, userId);
  }
}
