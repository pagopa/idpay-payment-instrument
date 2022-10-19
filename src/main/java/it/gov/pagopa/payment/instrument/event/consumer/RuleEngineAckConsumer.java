package it.gov.pagopa.payment.instrument.event.consumer;

import it.gov.pagopa.payment.instrument.dto.RuleEngineAckDTO;
import it.gov.pagopa.payment.instrument.service.PaymentInstrumentService;
import java.util.function.Consumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RuleEngineAckConsumer {

  @Bean
  public Consumer<RuleEngineAckDTO> ackConsumer(PaymentInstrumentService paymentInstrumentService){
    return paymentInstrumentService::processAck;
  }


}
