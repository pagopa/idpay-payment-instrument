package it.gov.pagopa.payment.instrument.connector;

import it.gov.pagopa.payment.instrument.dto.pm.WalletV2ListResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;

@FeignClient(
    name = "${rest-client.pm.serviceCode}",
    url = "${rest-client.pm.baseUrl}")
public interface PMRestClient {

  @GetMapping(
      value = "/idpay/wallet/test/pmservice",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  WalletV2ListResponse getWalletList(@RequestHeader("Ocp-Apim-Subscription-Key") String apimKey, @RequestHeader("Ocp-Apim-Trace") String apimTrace,
      @RequestHeader("Fiscal-Code") String userId);
}
