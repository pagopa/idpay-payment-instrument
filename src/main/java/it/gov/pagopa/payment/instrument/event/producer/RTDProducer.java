package it.gov.pagopa.payment.instrument.event.producer;

import it.gov.pagopa.payment.instrument.dto.rtd.RTDOperationDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

@Component
public class RTDProducer {
  private final String binderInstrument;
  private final StreamBridge streamBridge;

  public RTDProducer(@Value("${spring.cloud.stream.bindings.paymentInstrumentQueue-out-1.binder}") String binderInstrument,
                     StreamBridge streamBridge) {
    this.binderInstrument = binderInstrument;
    this.streamBridge = streamBridge;
  }

  public void sendInstrument(RTDOperationDTO rtdOperationDTO) {
    streamBridge.send("paymentInstrumentQueue-out-1", binderInstrument, rtdOperationDTO);
  }

}
