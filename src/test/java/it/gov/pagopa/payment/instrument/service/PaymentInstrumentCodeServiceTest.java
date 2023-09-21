package it.gov.pagopa.payment.instrument.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import feign.FeignException;
import feign.Request;
import feign.Request.HttpMethod;
import feign.RequestTemplate;
import it.gov.pagopa.payment.instrument.connector.WalletRestConnector;
import it.gov.pagopa.payment.instrument.dto.GenerateCodeRespDTO;
import it.gov.pagopa.payment.instrument.exception.PaymentInstrumentException;
import it.gov.pagopa.payment.instrument.repository.PaymentInstrumentCodeRepository;
import it.gov.pagopa.payment.instrument.test.fakers.GenerateCodeReqDTO;
import it.gov.pagopa.payment.instrument.utils.AuditUtilities;
import java.util.HashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class PaymentInstrumentCodeServiceTest {

  private PaymentInstrumentCodeService paymentInstrumentCodeService;

  @Mock
  PaymentInstrumentCodeRepository paymentInstrumentCodeRepository;

  @Mock
  WalletRestConnector walletRestConnector;

  @Mock
  AuditUtilities auditUtilities;

  @BeforeEach
  void setUp() {
    paymentInstrumentCodeService = new PaymentInstrumentCodeServiceImpl(paymentInstrumentCodeRepository,
        walletRestConnector, auditUtilities);
  }

  @Test
  void generateCode_initiativeId_not_empty(){
        GenerateCodeRespDTO generateCodeRespDTO =
        paymentInstrumentCodeService.generateCode("USERID", GenerateCodeReqDTO.mockInstance(1, true));

    assertions(generateCodeRespDTO);
  }

  @Test
  void generateCode_initiativeId_empty(){
    GenerateCodeRespDTO generateCodeRespDTO =
        paymentInstrumentCodeService.generateCode("USERID", GenerateCodeReqDTO.mockInstance(1, false));

    verify(walletRestConnector, never()).enrollInstrumentCode(any(), any());
    assertions(generateCodeRespDTO);
  }

  @Test
  void generateCode_enrollKo_notFound(){
    Request request =
        Request.create(
            HttpMethod.PUT, "url", new HashMap<>(), null, new RequestTemplate());
    Mockito.doThrow(new FeignException.NotFound("Initiative not found", request, new byte[0], null))
        .when(walletRestConnector).enrollInstrumentCode("INITIATIVEID1", "USERID");
    try {
      paymentInstrumentCodeService.generateCode("USERID", GenerateCodeReqDTO.mockInstance(1, true));
    }catch (PaymentInstrumentException e){
      assertEquals(HttpStatus.NOT_FOUND.value(), e.getCode());
      assertEquals("Resource not found while enrolling idpayCode on ms wallet", e.getMessage());
    }
  }

  @Test
  void generateCode_enrollKo_tooManyRequests(){
    Request request =
        Request.create(
            HttpMethod.PUT, "url", new HashMap<>(), null, new RequestTemplate());
    Mockito.doThrow(new FeignException.TooManyRequests("", request, new byte[0], null))
        .when(walletRestConnector).enrollInstrumentCode("INITIATIVEID1", "USERID");
    try {
      paymentInstrumentCodeService.generateCode("USERID", GenerateCodeReqDTO.mockInstance(1, true));
    }catch (PaymentInstrumentException e){
      assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(), e.getCode());
      assertEquals("Too many request on the ms wallet", e.getMessage());
    }
  }
  @Test
  void generateCode_enrollKo_internalServerError(){
    Request request =
        Request.create(
            HttpMethod.PUT, "url", new HashMap<>(), null, new RequestTemplate());
    Mockito.doThrow(new FeignException.InternalServerError("", request, new byte[0], null))
        .when(walletRestConnector).enrollInstrumentCode("INITIATIVEID1", "USERID");
    try {
      paymentInstrumentCodeService.generateCode("USERID", GenerateCodeReqDTO.mockInstance(1, true));
    }catch (PaymentInstrumentException e){
      assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getCode());
      assertEquals("An error occurred in the microservice wallet", e.getMessage());
    }
  }

  private void assertions(GenerateCodeRespDTO generateCodeRespDTO) {
    verify(paymentInstrumentCodeRepository, Mockito.times(1))
        .updateCode(anyString(), anyString(), any());
    assertNotNull(generateCodeRespDTO);
    assertEquals(5, generateCodeRespDTO.getIdpayCode().length());
  }

}
