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
  private static final String CIPHER_INSTANCE = "AES/GCM/NoPadding";
  private static final String IV = "IV_SAMPLE";

  @BeforeEach
  void setUp() {
    encryptCodeService = new EncryptCodeServiceImpl(CIPHER_INSTANCE, IV);
  }

  @Test
  void encryptIdpayCode(){
    String idpayCode = encryptCodeService.buildHashedPinBlock(
        "12345","0000FFFFFFFFFFFF", "salt");

    assertNotNull(idpayCode);
  }

  @Test
  void encryptIdpayCode_ko_code_length(){
    try{
      encryptCodeService.buildHashedPinBlock(
          "1234","0000FFFFFFFFFFFF", "salt");
      fail();
    }catch (PaymentInstrumentException e){
      assertEquals(HttpStatus.BAD_REQUEST.value(), e.getCode());
      assertEquals("Pin length is not valid", e.getMessage());
    }
  }

  @Test
  void encryptIdpayCode_ko_internalServerError() {
    try{
      encryptCodeService.buildHashedPinBlock(
          "12345","testError", "salt");
      fail();
    }catch (PaymentInstrumentException e){
      assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getCode());
      assertEquals("Something went wrong while creating pinBlock", e.getMessage());
    }
  }
}
