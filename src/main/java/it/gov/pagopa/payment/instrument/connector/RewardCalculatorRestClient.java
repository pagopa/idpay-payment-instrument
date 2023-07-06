package it.gov.pagopa.payment.instrument.connector;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@FeignClient(
        name = "reward-calculator",
        url = "${rest-client.reward.baseUrl}")
public interface RewardCalculatorRestClient {
    @DeleteMapping(
            value = "/paymentinstrument/{userId}/{initiativeId}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    void disableUserInitiativeInstruments(
            @PathVariable("userId") String userId, @PathVariable("initiativeId") String initiativeId);

    @PutMapping(
            value = "/paymentinstrument/{userId}/{initiativeId}/reactivate",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    void enableUserInitiativeInstruments(
            @PathVariable("userId") String userId, @PathVariable("initiativeId") String initiativeId);
}
