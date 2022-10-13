package it.gov.pagopa.payment.instrument.connector;

import it.gov.pagopa.payment.instrument.dto.CFDTO;
import it.gov.pagopa.payment.instrument.dto.EncryptedCfDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

@Service
public interface EncryptRestConnector {

  EncryptedCfDTO upsertToken(@RequestBody CFDTO body);
}
