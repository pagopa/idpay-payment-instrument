package it.gov.pagopa.payment.instrument.connector;

import feign.FeignException;
import it.gov.pagopa.payment.instrument.dto.InstrumentAckDTO;
import it.gov.pagopa.payment.instrument.dto.WalletCallDTO;
import it.gov.pagopa.payment.instrument.exception.custom.UserNotOnboardedException;
import it.gov.pagopa.payment.instrument.exception.custom.WalletInvocationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants.ExceptionMessage.ERROR_INVOCATION_WALLET_MSG;
import static it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants.ExceptionMessage.ERROR_USER_NOT_ONBOARDED_MSG;

@Service
@Slf4j
public class WalletRestConnectorImpl implements WalletRestConnector {

  private final WalletRestClient walletRestClient;

  public WalletRestConnectorImpl(WalletRestClient paymentInstrumentRestClient) {
    this.walletRestClient = paymentInstrumentRestClient;
  }

  @Override
  public void updateWallet(WalletCallDTO body) {
    try{
      walletRestClient.updateWallet(body);
    } catch (FeignException e) {
      log.error("[UPDATE_WALLET] An error occurred while invoking the wallet microservice");
      throw new WalletInvocationException(ERROR_INVOCATION_WALLET_MSG,true,e);
    }
  }

  @Override
  public void processAck(InstrumentAckDTO body) {
    try {
      walletRestClient.processAck(body);
    }catch (FeignException e){
      if(e.status() == 404){
        log.error("[PROCESS_ACK] The user {} is not onboarded on initiative {}", body.getUserId(), body.getInitiativeId());
        throw new UserNotOnboardedException(String.format(ERROR_USER_NOT_ONBOARDED_MSG,body.getInitiativeId()),true,e);
      }
      log.error("[PROCESS_ACK] An error occurred while invoking the wallet microservice");
      throw new WalletInvocationException(ERROR_INVOCATION_WALLET_MSG,true,e);
    }

  }

  @Override
  public void enrollInstrumentCode(String initiativeId, String userId) {
    walletRestClient.enrollInstrumentCode(initiativeId, userId);
  }

}
