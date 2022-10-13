package it.gov.pagopa.payment.instrument.connector;

import it.gov.pagopa.payment.instrument.dto.CFDTO;
import it.gov.pagopa.payment.instrument.dto.EncryptedCfDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;

@FeignClient(name = "${rest-client.encrypt.cf}", url = "${rest-client.encrypt.base-url}")
public interface EncryptRest {

  @PutMapping(value = "/tokens", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  EncryptedCfDTO upsertToken(@RequestBody CFDTO cfdto, @RequestHeader("x-api-key") String apikey);
}
