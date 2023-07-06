package it.gov.pagopa.payment.instrument.connector;

import org.springframework.web.bind.annotation.PathVariable;

public interface RewardCalculatorConnector {
   void disableUserInitiativeInstruments(@PathVariable String userId, @PathVariable String initiativeId);
   void enableUserInitiativeInstruments(@PathVariable String userId, @PathVariable String initiativeId);
}
