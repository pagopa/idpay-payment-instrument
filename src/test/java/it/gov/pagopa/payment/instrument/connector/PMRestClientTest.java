//package it.gov.pagopa.payment.instrument.connector;
//
//import com.github.tomakehurst.wiremock.WireMockServer;
//import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
//import it.gov.pagopa.payment.instrument.config.RestConnectorConfig;
//import it.gov.pagopa.payment.instrument.dto.pm.WalletV2ListResponse;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
//import org.springframework.cloud.openfeign.FeignAutoConfiguration;
//import org.springframework.context.ApplicationContextInitializer;
//import org.springframework.context.ConfigurableApplicationContext;
//import org.springframework.context.event.ContextClosedEvent;
//import org.springframework.test.context.ContextConfiguration;
//import org.springframework.test.context.TestPropertySource;
//import org.springframework.test.context.junit.jupiter.SpringExtension;
//import org.springframework.test.context.support.TestPropertySourceUtils;
//
//@ExtendWith(SpringExtension.class)
//@SpringBootTest(webEnvironment = WebEnvironment.NONE)
//@ContextConfiguration(
//    initializers = PMRestClientTest.WireMockInitializer.class,
//    classes = {
//        PMRestClientConnectorImpl.class,
//        RestConnectorConfig.class,
//        FeignAutoConfiguration.class,
//        HttpMessageConvertersAutoConfiguration.class
//    })
//@TestPropertySource(
//    locations = "classpath:application.yml",
//    properties = "spring.application.name=idpay-service-pm")
//public class PMRestClientTest {
//    private static final String FISCAL_CODE = "GMMMRA79L13H703E";
//    private static final String APIMKEY = "TEST_APIM_KEY";
//    private static final String APIMTRACE = "TEST_APIM_TRACE";
//
//    @Autowired
//    private PMRestClient pmRestClient;
//    @Autowired
//    private PMRestClientConnector restConnector;
//
//    @Test
//    void getWalletInfo() {
//
//        final WalletV2ListResponse actualResponse = restConnector.getWalletList(FISCAL_CODE);
//
////        assertNotNull(actualResponse);
//    }
//
//    public static class WireMockInitializer
//        implements ApplicationContextInitializer<ConfigurableApplicationContext> {
//
//        @Override
//        public void initialize(ConfigurableApplicationContext applicationContext) {
//            WireMockServer wireMockServer = new WireMockServer(new WireMockConfiguration().dynamicPort());
//            wireMockServer.start();
//
//            applicationContext.getBeanFactory().registerSingleton("wireMockServer", wireMockServer);
//
//            applicationContext.addApplicationListener(
//                applicationEvent -> {
//                    if (applicationEvent instanceof ContextClosedEvent) {
//                        wireMockServer.stop();
//                    }
//                });
//
//            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
//                applicationContext,
//                String.format(
//                    "rest-client.pm.baseUrl=http://%s:%d",
//                    wireMockServer.getOptions().bindAddress(), wireMockServer.port()));
//        }
//    }
//
//}
