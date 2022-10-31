package it.gov.pagopa.payment.instrument.connector;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import feign.FeignException;
import it.gov.pagopa.payment.instrument.config.RestConnectorConfig;
import it.gov.pagopa.payment.instrument.dto.pm.WalletV2ListResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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
    initializers = PMRestClientTest.WireMockInitializer.class,
    classes = {
        PMRestClientConnectorImpl.class,
        RestConnectorConfig.class,
        FeignAutoConfiguration.class,
        HttpMessageConvertersAutoConfiguration.class
    })
@TestPropertySource(
    locations = "classpath:application.yml",
    properties = {"spring.application.name=service.pm"})
class PMRestClientTest {

  private static final String USER_ID = "TEST_USER_ID";

  @Autowired
  private PMRestClient pmRestClient;
  @Autowired
  private PMRestClientConnector pmRestClientConnector;
  @MockBean
  ObjectMapper objectMapper;

  @Test
  void getWalletList() {
    try {
      final WalletV2ListResponse actual = pmRestClientConnector.getWalletList(USER_ID);
      assertNotNull(actual);
    } catch (FeignException e) {
      fail();
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
              "rest-client.pm.baseUrl=http://%s:%d",
              wireMockServer.getOptions().bindAddress(), wireMockServer.port()));
    }
  }
}
