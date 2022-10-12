package it.gov.pagopa.payment.instrument.event.consumer;

import it.gov.pagopa.payment.instrument.dto.DeactivationPMBodyDTO;
import it.gov.pagopa.payment.instrument.service.PaymentInstrumentService;
import java.util.function.Consumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RtdConsumer {
  @Bean
  public Consumer<DeactivationPMBodyDTO> consumerIban(PaymentInstrumentService paymentInstrumentService){
    return paymentInstrumentService::deactivateInstrumentPM;
  }

}
