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
import it.gov.pagopa.payment.instrument.dto.GeneratedCodeDTO;
import it.gov.pagopa.payment.instrument.exception.PaymentInstrumentException;
import it.gov.pagopa.payment.instrument.repository.PaymentInstrumentCodeRepository;
import it.gov.pagopa.payment.instrument.test.fakers.GenerateCodeDTOFaker;
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
        GeneratedCodeDTO generatedCodeDTO =
        paymentInstrumentCodeService.generateCode("USERID", GenerateCodeDTOFaker.mockInstance(1, true));

    assertions(generatedCodeDTO);
  }

  @Test
  void generateCode_initiativeId_empty(){
    GeneratedCodeDTO generatedCodeDTO =
        paymentInstrumentCodeService.generateCode("USERID", GenerateCodeDTOFaker.mockInstance(1, false));

    verify(walletRestConnector, never()).enrollInstrumentCode(any(), any());

    assertions(generatedCodeDTO);
  }

  @Test
  void generateCode_enrollKo(){
    Request request =
        Request.create(
            HttpMethod.PUT, "url", new HashMap<>(), null, new RequestTemplate());
    Mockito.doThrow(new FeignException.BadRequest("", request, new byte[0], null))
        .when(walletRestConnector).enrollInstrumentCode("INITIATIVEID1", "USERID");
    try {
      paymentInstrumentCodeService.generateCode("USERID", GenerateCodeDTOFaker.mockInstance(1, true));
    }catch (PaymentInstrumentException e){
      assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getCode());
      assertEquals("An error occurred while enrolling code", e.getMessage());
    }

    verify(paymentInstrumentCodeRepository, Mockito.times(1))
        .updateCode(anyString(), anyString(), any());
  }

  private void assertions(GeneratedCodeDTO generatedCodeDTO) {
    verify(paymentInstrumentCodeRepository, Mockito.times(1))
        .updateCode(anyString(), anyString(), any());
    assertNotNull(generatedCodeDTO);
    assertEquals(5, generatedCodeDTO.getIdpayCode().length());
  }

}
