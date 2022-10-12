package it.gov.pagopa.payment.instrument.config;

import it.gov.pagopa.payment.instrument.connector.EncryptRest;
import it.gov.pagopa.payment.instrument.connector.WalletRestClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(clients = {WalletRestClient.class, EncryptRest.class})
public class RestConnectorConfig {

}
