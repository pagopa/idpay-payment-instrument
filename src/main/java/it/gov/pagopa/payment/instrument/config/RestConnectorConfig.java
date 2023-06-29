package it.gov.pagopa.payment.instrument.config;

import it.gov.pagopa.payment.instrument.connector.*;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(clients = {
        WalletRestClient.class,
        EncryptRest.class,
        PMRestClient.class,
        DecryptRest.class,
        RewardCalculatorRestClient.class
})
public class RestConnectorConfig {

}
