package it.gov.pagopa.payment.instrument.connector;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.mongodb.assertions.Assertions;
import it.gov.pagopa.payment.instrument.config.RestConnectorConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.TestPropertySourceUtils;
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ContextConfiguration(
        initializers = RewardCalculatorRestClientTest.WireMockInitializer.class,
        classes = {
                RewardCalculatorConnectorImpl.class,
                RestConnectorConfig.class,
                FeignAutoConfiguration.class,
                HttpMessageConvertersAutoConfiguration.class
        })
@TestPropertySource(
        locations = "classpath:application.yml",
        properties = {"spring.application.name=idpay-reward-calculator-integration-rest"})
class RewardCalculatorRestClientTest {

    @Autowired
    RewardCalculatorConnector rewardCalculatorConnector;

    private static final String INITIATIVE_ID = "INITIATIVE_ID";
    private static final String USER_ID = "TEST_USER_ID";

    @Test
    void cancelInstruments() {

        try {
            rewardCalculatorConnector.cancelInstruments(USER_ID,INITIATIVE_ID);

        } catch (Exception e) {
            Assertions.fail();
        }
    }
    @Test
    void rollbackInstruments() {

        try {
            rewardCalculatorConnector.rollbackInstruments(USER_ID,INITIATIVE_ID);

        } catch (Exception e) {
            Assertions.fail();
        }
    }



    public static class WireMockInitializer
            implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            WireMockServer wireMockServer = new WireMockServer(new WireMockConfiguration().dynamicPort());
            wireMockServer.start();

            applicationContext.getBeanFactory().registerSingleton("wireMockServer", wireMockServer);

            applicationContext.addApplicationListener(
                    applicationEvent -> {
                        if (applicationEvent instanceof ContextClosedEvent) {
                            wireMockServer.stop();
                        }
                    });

            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                    applicationContext,
                    String.format(
                            "rest-client.reward.baseUrl=http://%s:%d",
                            wireMockServer.getOptions().bindAddress(), wireMockServer.port()));
        }
    }
}