package it.gov.pagopa.payment.instrument.connector;

import feign.FeignException;
import it.gov.pagopa.payment.instrument.dto.CFDTO;
import it.gov.pagopa.payment.instrument.dto.EncryptedCfDTO;
import it.gov.pagopa.payment.instrument.exception.custom.PDVInvocationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import static it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants.ExceptionMessage.ERROR_INVOCATION_PDV_DECRYPT_MSG;
import static it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants.ExceptionMessage.ERROR_INVOCATION_PDV_ENCRYPT_MSG;

@Service
@Slf4j
public class EncryptRestConnectorImpl implements EncryptRestConnector {
  private final String apikey;
  private final EncryptRest encryptRest;

  public EncryptRestConnectorImpl(@Value("${api.key.encrypt}")String apikey,
      EncryptRest encryptRest) {
    this.apikey = apikey;
    this.encryptRest = encryptRest;
  }

  @Override
  public EncryptedCfDTO upsertToken(CFDTO cfdto) {
    EncryptedCfDTO encryptedCfDTO;
    try {
      encryptedCfDTO = encryptRest.upsertToken(cfdto,apikey);
    } catch (FeignException e) {
      log.error("[GET_TOKEN_BY_PII] PDV: something went wrong when invoking the PDV API.");
      throw new PDVInvocationException(ERROR_INVOCATION_PDV_ENCRYPT_MSG);
    }
    return encryptedCfDTO;
  }
}
