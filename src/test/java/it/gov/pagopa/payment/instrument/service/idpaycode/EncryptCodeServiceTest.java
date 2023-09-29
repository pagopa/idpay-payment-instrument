package it.gov.pagopa.payment.instrument.service.idpaycode;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EncryptCodeServiceTest {

  private EncryptCodeService encryptCodeService;

  @BeforeEach
  void setUp() {
    encryptCodeService = new EncryptCodeServiceImpl();
  }

  @Test
  void encryptIdpayCode(){
    String idpayCode = encryptCodeService.encryptIdpayCode("code");

    assertNotNull(idpayCode);

  }

}
