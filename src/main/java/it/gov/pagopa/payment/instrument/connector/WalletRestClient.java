package it.gov.pagopa.payment.instrument.connector;

import it.gov.pagopa.payment.instrument.dto.InstrumentAckDTO;
import it.gov.pagopa.payment.instrument.dto.WalletCallDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

@FeignClient(
    name = "${rest-client.wallet.serviceCode}",
    url = "${wallet.uri}")
public interface WalletRestClient {

  @PutMapping(
      value = "/idpay/wallet/updateWallet",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  void updateWallet(
      @RequestBody WalletCallDTO body);

  @PutMapping(
      value = "/idpay/wallet/acknowledge",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  void processAck(
      @RequestBody InstrumentAckDTO body);

  @PutMapping(
      value = "/idpay/wallet/{initiativeId}/{userId}/code/instruments",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  void enrollInstrumentCode(
      @PathVariable("initiativeId") String initiativeId,
      @PathVariable("userId") String userId);
}