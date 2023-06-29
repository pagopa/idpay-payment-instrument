package it.gov.pagopa.payment.instrument.connector;

import org.springframework.web.bind.annotation.PathVariable;

public interface RewardCalculatorConnector {
   void cancelInstruments(@PathVariable String userId, @PathVariable String initiativeId);
   void rollbackInstruments(@PathVariable String userId, @PathVariable String initiativeId);
}
