package it.gov.pagopa.payment.instrument.event.producer;

import it.gov.pagopa.payment.instrument.dto.rtd.RTDOperationDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RTDProducer {

  @Value("${spring.cloud.stream.bindings.paymentInstrumentQueue-out-1.binder}")
  private String binderInstrument;
  @Autowired
  StreamBridge streamBridge;

  public void sendInstrument(RTDOperationDTO rtdOperationDTO) {
    streamBridge.send("paymentInstrumentQueue-out-1", binderInstrument, rtdOperationDTO);
  }

}
