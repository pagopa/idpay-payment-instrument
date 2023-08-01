package it.gov.pagopa.payment.instrument.event.consumer;

import it.gov.pagopa.payment.instrument.dto.QueueCommandOperationDTO;
import it.gov.pagopa.payment.instrument.service.PaymentInstrumentService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
public class CommandConsumer {
    @Bean
    public Consumer<QueueCommandOperationDTO> consumerCommands(PaymentInstrumentService paymentInstrumentService) {
        return paymentInstrumentService::processOperation;
    }
}
