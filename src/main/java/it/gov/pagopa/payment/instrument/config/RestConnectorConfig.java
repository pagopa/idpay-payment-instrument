package it.gov.pagopa.payment.instrument.config;

import it.gov.pagopa.payment.instrument.connector.PMRestClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(clients = PMRestClient.class)
public class RestConnectorConfig {

}
