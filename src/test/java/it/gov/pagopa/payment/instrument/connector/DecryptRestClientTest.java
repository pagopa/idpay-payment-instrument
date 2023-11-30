package it.gov.pagopa.payment.instrument.connector;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import it.gov.pagopa.payment.instrument.config.RestConnectorConfig;
import it.gov.pagopa.payment.instrument.dto.DecryptCfDTO;
import it.gov.pagopa.payment.instrument.exception.custom.PDVInvocationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.TestPropertySourceUtils;

import java.util.HashMap;

import static it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants.ExceptionCode.GENERIC_ERROR;
import static it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants.ExceptionMessage.ERROR_INVOCATION_PDV_DECRYPT_MSG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ContextConfiguration(
    initializers = DecryptRestClientTest.WireMockInitializer.class,
    classes = {
        DecryptRestConnectorImpl.class,
        RestConnectorConfig.class,
        FeignAutoConfiguration.class,
        HttpMessageConvertersAutoConfiguration.class
    })
@TestPropertySource(
    locations = "classpath:application.yml",
    properties = {"spring.application.name=pdv-ms-tokenizer-test"})
class DecryptRestClientTest {
  private static final String FISCAL_CODE = "AAAAAA00A00A000A";
  private static final String TOKEN = "TEST_TOKEN";

  @SpyBean
  private DecryptRest decryptRest;

  @Autowired
  private DecryptRestConnector restConnector;

  @Test
  void getPii() {

    final DecryptCfDTO actualResponse = restConnector.getPiiByToken(TOKEN);

    assertNotNull(actualResponse);
    assertEquals(FISCAL_CODE, actualResponse.getPii());
  }

  @Test
  void getPiiToken_ko_FeingException() {
    Mockito.doThrow(new FeignException
                    .InternalServerError("", Request.create(Request.HttpMethod.PUT, "url", new HashMap<>(), null, new RequestTemplate()), new byte[0], null))
            .when(decryptRest).getPiiByToken(Mockito.any(), Mockito.any());
    try {
      restConnector.getPiiByToken(TOKEN);
    } catch (PDVInvocationException e) {
      Assertions.assertEquals(GENERIC_ERROR,e.getCode());
      Assertions.assertEquals(ERROR_INVOCATION_PDV_DECRYPT_MSG, e.getMessage());
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
              "rest-client.decrypt.baseUrl=http://%s:%d",
              wireMockServer.getOptions().bindAddress(), wireMockServer.port()));
    }
  }

}
