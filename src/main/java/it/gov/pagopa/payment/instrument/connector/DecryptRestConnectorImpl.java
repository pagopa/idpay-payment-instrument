package it.gov.pagopa.payment.instrument.connector;

import feign.FeignException;
import it.gov.pagopa.payment.instrument.dto.DecryptCfDTO;
import it.gov.pagopa.payment.instrument.exception.custom.PDVInvocationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import static it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants.ExceptionMessage.ERROR_INVOCATION_PDV_DECRYPT_MSG;

@Service
@Slf4j
public class DecryptRestConnectorImpl implements DecryptRestConnector{
  private final String apikey;
  private final DecryptRest decryptRest;

  public DecryptRestConnectorImpl(@Value("${api.key.decrypt}")String apikey,
      DecryptRest decryptRest) {
    this.apikey = apikey;
    this.decryptRest = decryptRest;
  }

  @Override
  public DecryptCfDTO getPiiByToken(String token) {
    DecryptCfDTO decryptCfDTO;

    try {
      decryptCfDTO = decryptRest.getPiiByToken(token, apikey);
    } catch (FeignException e) {
      log.error("[GET_PII_BY_TOKEN] PDV: something went wrong when invoking the PDV API.");
      throw new PDVInvocationException(ERROR_INVOCATION_PDV_DECRYPT_MSG);
    }
    return decryptCfDTO;
  }
}
