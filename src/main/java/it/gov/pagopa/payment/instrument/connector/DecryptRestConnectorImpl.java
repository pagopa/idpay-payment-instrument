package it.gov.pagopa.payment.instrument.connector;

import it.gov.pagopa.payment.instrument.dto.DecryptCfDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
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
    return decryptRest.getPiiByToken(token, apikey);
  }
}
