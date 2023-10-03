package it.gov.pagopa.payment.instrument.service.idpaycode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import it.gov.pagopa.payment.instrument.exception.PaymentInstrumentException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class EncryptCodeServiceTest {

  private EncryptCodeService encryptCodeService;

  @BeforeEach
  void setUp() {
    encryptCodeService = new EncryptCodeServiceImpl();
  }

  @Test
  void encryptIdpayCode(){
    String idpayCode = encryptCodeService.encryptIdpayCode(
        "12345","secondFactor", "salt");

    assertNotNull(idpayCode);
  }

  @Test
  void encryptIdpayCode_ko_code_length(){
    try{
      encryptCodeService.encryptIdpayCode(
          "1234","secondFactor", "salt");
      fail();
    }catch (PaymentInstrumentException e){
      assertEquals(HttpStatus.BAD_REQUEST.value(), e.getCode());
      assertEquals("Pin length it's not valid", e.getMessage());
    }
  }

  @Test
  void encryptIdpayCode_ko_internalServerError() {
    try{
      encryptCodeService.encryptIdpayCode(
          "12345","", "salt");
      fail();
    }catch (PaymentInstrumentException e){
      assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getCode());
      assertEquals("Something went wrong while creating pinBlock", e.getMessage());
    }
  }

  @Test
  void encryptIdpayCode_ko_sha256() {
    try{
      encryptCodeService.encryptIdpayCode(
          "12345","secondFactor", null);
      fail();
    }catch (PaymentInstrumentException e){
      assertEquals(HttpStatus.FORBIDDEN.value(), e.getCode());
      assertEquals("Something went wrong creating SHA256 digest", e.getMessage());
    }
  }

}
