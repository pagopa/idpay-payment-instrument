package it.gov.pagopa.payment.instrument.event.producer;

import it.gov.pagopa.payment.instrument.dto.RuleEngineQueueDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Component
public class RuleEngineProducer {
  @Value("${spring.cloud.stream.bindings.paymentInstrumentQueue-out-0.binder}")
  private String binderInstrument;
  @Autowired
  StreamBridge streamBridge;

  public void sendInstruments(Message<RuleEngineQueueDTO> ruleEngineQueueDTO){
    streamBridge.send("paymentInstrumentQueue-out-0", binderInstrument, ruleEngineQueueDTO);
  }

}
