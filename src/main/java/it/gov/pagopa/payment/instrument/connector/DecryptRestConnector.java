package it.gov.pagopa.payment.instrument.connector;

import it.gov.pagopa.payment.instrument.dto.DecryptCfDTO;
import org.springframework.stereotype.Service;

@Service
public interface DecryptRestConnector {

  DecryptCfDTO getPiiByToken(String token);
}
