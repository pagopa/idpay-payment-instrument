package it.gov.pagopa.payment.instrument.connector;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import it.gov.pagopa.payment.instrument.config.RestConnectorConfig;
import it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants;
import it.gov.pagopa.payment.instrument.dto.InstrumentAckDTO;
import it.gov.pagopa.payment.instrument.dto.WalletCallDTO;
import it.gov.pagopa.payment.instrument.dto.WalletDTO;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import it.gov.pagopa.payment.instrument.exception.custom.UserNotOnboardedException;
import it.gov.pagopa.payment.instrument.exception.custom.WalletInvocationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
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

import static it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants.ExceptionCode.GENERIC_ERROR;
import static it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants.ExceptionCode.USER_NOT_ONBOARDED;
import static it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants.ExceptionMessage.ERROR_INVOCATION_WALLET_MSG;
import static it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants.ExceptionMessage.ERROR_USER_NOT_ONBOARDED_MSG;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ContextConfiguration(
    initializers = WalletRestClientTest.WireMockInitializer.class,
    classes = {
        WalletRestConnectorImpl.class,
        RestConnectorConfig.class,
        FeignAutoConfiguration.class,
        HttpMessageConvertersAutoConfiguration.class
    })
@TestPropertySource(
    locations = "classpath:application.yml",
    properties = {"spring.application.name=idpay-initiative-integration"})
class WalletRestClientTest {

  @MockBean
  private WalletRestClient walletRestClient;
  private static final String INITIATIVE_ID = "INITIATIVE_ID";
  private static final String HPAN = "TEST_HPAN";
  private static final String MASKED_PAN = "MASKED_PAN";
  private static final String USER_ID = "TEST_USER_ID";
  private static final String BRAND_LOGO = "BAND_LOGO";

  private static final WalletCallDTO WALLET_CALL_DTO = new WalletCallDTO();
  private static final WalletDTO WALLET_DTO = new WalletDTO(INITIATIVE_ID, USER_ID, HPAN,
      BRAND_LOGO, BRAND_LOGO, MASKED_PAN);
  private static final List<WalletDTO> WALLET_DTO_LIST = new ArrayList<>();
  private static final InstrumentAckDTO INSTRUMENT_ACK_DTO = InstrumentAckDTO.builder()
      .initiativeId(INITIATIVE_ID)
      .userId(USER_ID)
      .channel("APP_IO")
      .maskedPan(MASKED_PAN)
      .brandLogo(BRAND_LOGO)
      .ninstr(0)
      .operationType(PaymentInstrumentConstants.OPERATION_ADD)
      .operationDate(LocalDateTime.now()).build();

  @Autowired
  private WalletRestConnector walletRestConnector;

  @Test
  void updateWallet() {

    WALLET_DTO_LIST.add(WALLET_DTO);
    WALLET_CALL_DTO.setWalletDTOlist(WALLET_DTO_LIST);
    try {
      walletRestConnector.updateWallet(WALLET_CALL_DTO);

    } catch (Exception e) {
      Assertions.fail();
    }
  }

  @Test
  void updateWallet_ko_feignException() {
    Mockito.doThrow(new FeignException
            .InternalServerError("", Request.create(Request.HttpMethod.PUT, "url", new HashMap<>(), null, new RequestTemplate()), new byte[0], null))
            .when(walletRestClient).updateWallet(WALLET_CALL_DTO);

    try {
      walletRestConnector.updateWallet(WALLET_CALL_DTO);
    } catch (WalletInvocationException e) {
      Assertions.assertEquals(GENERIC_ERROR, e.getCode());
      Assertions.assertEquals(ERROR_INVOCATION_WALLET_MSG, e.getMessage());
    }
  }
  @Test
  void processAck() {

    WALLET_DTO_LIST.add(WALLET_DTO);
    WALLET_CALL_DTO.setWalletDTOlist(WALLET_DTO_LIST);

    walletRestConnector.processAck(INSTRUMENT_ACK_DTO);

    Mockito.verify(walletRestClient, Mockito.times(1))
              .processAck(Mockito.any());
  }

  @Test
  void processAck_ko_wallet404() {

    WALLET_DTO_LIST.add(WALLET_DTO);
    WALLET_CALL_DTO.setWalletDTOlist(WALLET_DTO_LIST);

    Mockito.doThrow(new FeignException
            .NotFound("", Request.create(Request.HttpMethod.PUT, "url", new HashMap<>(), null, new RequestTemplate()), new byte[0], null))
            .when(walletRestClient).processAck(Mockito.any());
    try {
      walletRestConnector.processAck(INSTRUMENT_ACK_DTO);
    } catch (UserNotOnboardedException e) {
      Assertions.assertEquals(USER_NOT_ONBOARDED,e.getCode());
      Assertions.assertEquals(String.format(ERROR_USER_NOT_ONBOARDED_MSG,INSTRUMENT_ACK_DTO.getInitiativeId()), e.getMessage());
    }
  }

  @Test
  void processAck_ko_wallet500() {

    WALLET_DTO_LIST.add(WALLET_DTO);
    WALLET_CALL_DTO.setWalletDTOlist(WALLET_DTO_LIST);

    Mockito.doThrow(new FeignException
                    .InternalServerError("", Request.create(Request.HttpMethod.PUT, "url", new HashMap<>(), null, new RequestTemplate()), new byte[0], null))
            .when(walletRestClient).processAck(Mockito.any());
    try {
      walletRestConnector.processAck(INSTRUMENT_ACK_DTO);
    } catch (WalletInvocationException e) {
      Assertions.assertEquals(GENERIC_ERROR,e.getCode());
      Assertions.assertEquals(ERROR_INVOCATION_WALLET_MSG, e.getMessage());
    }
  }

  @Test
  void enrollInstrumentCode() {
    try {
      walletRestConnector.enrollInstrumentCode(INITIATIVE_ID, USER_ID);
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
              "wallet.uri=http://%s:%d",
              wireMockServer.getOptions().bindAddress(), wireMockServer.port()));
    }
  }
}
