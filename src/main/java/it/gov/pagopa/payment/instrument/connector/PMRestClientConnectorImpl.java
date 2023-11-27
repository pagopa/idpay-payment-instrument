package it.gov.pagopa.payment.instrument.connector;

import feign.FeignException;
import it.gov.pagopa.payment.instrument.dto.pm.WalletV2ListResponse;
import it.gov.pagopa.payment.instrument.exception.custom.PMInvocationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import static it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants.ExceptionMessage.ERROR_INVOCATION_PM_MSG;

@Service
@Slf4j
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
    WalletV2ListResponse walletV2ListResponse = null;
    try{
      walletV2ListResponse = pmRestClient.getWalletList(apimKey, apimTrace, userId);
    }catch(FeignException e){
      log.error("[GET_WALLET_LIST] PM: something went wrong when invoking the PM API.");
      throw new PMInvocationException(ERROR_INVOCATION_PM_MSG);
    }
    return walletV2ListResponse;
  }
}
