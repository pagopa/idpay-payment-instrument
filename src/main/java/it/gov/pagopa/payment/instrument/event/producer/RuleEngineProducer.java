package it.gov.pagopa.payment.instrument.event.producer;

import it.gov.pagopa.payment.instrument.dto.RuleEngineRequestDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Component
public class RuleEngineProducer {
  private final String binderInstrument;
  private final StreamBridge streamBridge;

  public RuleEngineProducer(@Value("${spring.cloud.stream.bindings.paymentInstrumentQueue-out-0.binder}") String binderInstrument,
                            StreamBridge streamBridge) {
    this.binderInstrument = binderInstrument;
    this.streamBridge = streamBridge;
  }

  public void sendInstruments(Message<RuleEngineRequestDTO> ruleEngineQueueDTO){
    streamBridge.send("paymentInstrumentQueue-out-0", binderInstrument, ruleEngineQueueDTO);
  }

}
