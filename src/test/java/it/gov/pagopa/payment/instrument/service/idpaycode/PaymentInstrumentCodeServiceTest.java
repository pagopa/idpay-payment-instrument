package it.gov.pagopa.payment.instrument.service.idpaycode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import feign.FeignException;
import feign.Request;
import feign.Request.HttpMethod;
import feign.RequestTemplate;
import it.gov.pagopa.payment.instrument.connector.WalletRestConnector;
import it.gov.pagopa.payment.instrument.dto.EncryptedDataBlock;
import it.gov.pagopa.payment.instrument.dto.GenerateCodeRespDTO;
import it.gov.pagopa.payment.instrument.dto.PinBlockDTO;
import it.gov.pagopa.payment.instrument.exception.PaymentInstrumentException;
import it.gov.pagopa.payment.instrument.model.PaymentInstrumentCode;
import it.gov.pagopa.payment.instrument.repository.PaymentInstrumentCodeRepository;
import it.gov.pagopa.payment.instrument.utils.AuditUtilities;
import it.gov.pagopa.payment.instrument.utils.Utilities;
import java.util.HashMap;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class PaymentInstrumentCodeServiceTest {

  public static final String INITIATIVE_ID = "INITIATIVE_ID";
  public static final String USERID = "USERID";
  public static final String SALT = "salt";
  public static final String IDPAY_CODE = "idpayCode";
  public static final String KEY_ID = "https://KEYVAULTNAME.vault.azure.net/keys/KEYNAME/KEYID";
  public static final EncryptedDataBlock ENCRYPTED_DATA_BLOCK = new EncryptedDataBlock(IDPAY_CODE,
      KEY_ID);
  public static final String SECOND_FACTOR = "SECOND_FACTOR";
  public static final PaymentInstrumentCode PAYMENT_INSTRUMENT_CODE = PaymentInstrumentCode.builder()
      .userId(USERID)
      .secondFactor(SECOND_FACTOR)
      .salt(SALT)
      .idpayCode(IDPAY_CODE)
      .keyId(KEY_ID)
      .build();
  public static final String SHA256_CODE = "sha256";
  public static final String PIN_BLOCK = "12345";
  public static final String ENCRYPTED_KEY = "test-key";
  private PaymentInstrumentCodeService paymentInstrumentCodeService;
  @Mock
  PaymentInstrumentCodeRepository paymentInstrumentCodeRepository;
  @Mock
  WalletRestConnector walletRestConnector;
  @Mock
  AuditUtilities auditUtilities;
  @Mock
  IdpayCodeEncryptionService idpayCodeEncryptionService;
  @Mock
  Utilities utilities;

  @BeforeEach
  void setUp() {
    paymentInstrumentCodeService = new PaymentInstrumentCodeServiceImpl(
        paymentInstrumentCodeRepository,
        walletRestConnector, auditUtilities, idpayCodeEncryptionService, utilities);
  }

  @Test
  void generateCode_initiativeId_not_empty() {
    Mockito.when(
            idpayCodeEncryptionService.buildHashedDataBlock(anyString(), anyString(), anyString()))
        .thenReturn(PIN_BLOCK);

    Mockito.when(idpayCodeEncryptionService.encryptSHADataBlock(PIN_BLOCK))
        .thenReturn(ENCRYPTED_DATA_BLOCK);

    GenerateCodeRespDTO generateCodeRespDTO =
        paymentInstrumentCodeService.generateCode(USERID, INITIATIVE_ID);

    assertions(generateCodeRespDTO);
  }

  @Test
  void generateCode_initiativeId_empty() {
    Mockito.when(
            idpayCodeEncryptionService.buildHashedDataBlock(anyString(), anyString(), anyString()))
        .thenReturn(PIN_BLOCK);

    Mockito.when(idpayCodeEncryptionService.encryptSHADataBlock(PIN_BLOCK))
        .thenReturn(ENCRYPTED_DATA_BLOCK);

    GenerateCodeRespDTO generateCodeRespDTO =
        paymentInstrumentCodeService.generateCode(USERID, "");

    verify(walletRestConnector, never()).enrollInstrumentCode(any(), any());
    assertions(generateCodeRespDTO);
  }

  @Test
  void generateCode_initiativeId_null() {
    Mockito.when(
            idpayCodeEncryptionService.buildHashedDataBlock(anyString(), anyString(), anyString()))
        .thenReturn(PIN_BLOCK);

    Mockito.when(idpayCodeEncryptionService.encryptSHADataBlock(PIN_BLOCK))
        .thenReturn(ENCRYPTED_DATA_BLOCK);

    GenerateCodeRespDTO generateCodeRespDTO =
        paymentInstrumentCodeService.generateCode(USERID, null);

    verify(walletRestConnector, never()).enrollInstrumentCode(any(), any());
    assertions(generateCodeRespDTO);
  }

  @Test
  void generateCode_enrollKo_notFound() {
    Mockito.when(
            idpayCodeEncryptionService.buildHashedDataBlock(anyString(), anyString(), anyString()))
        .thenReturn(PIN_BLOCK);

    Mockito.when(idpayCodeEncryptionService.encryptSHADataBlock(PIN_BLOCK))
        .thenReturn(ENCRYPTED_DATA_BLOCK);

    Mockito.when(paymentInstrumentCodeRepository.deleteInstrument(USERID))
        .thenReturn(new PaymentInstrumentCode());

    Request request =
        Request.create(
            HttpMethod.PUT, "url", new HashMap<>(), null, new RequestTemplate());
    Mockito.doThrow(new FeignException.NotFound("Initiative not found", request, new byte[0], null))
        .when(walletRestConnector).enrollInstrumentCode(INITIATIVE_ID, USERID);

    try {
      paymentInstrumentCodeService.generateCode(USERID, INITIATIVE_ID);
    } catch (PaymentInstrumentException e) {
      assertEquals(HttpStatus.NOT_FOUND.value(), e.getCode());
    }
    verify(paymentInstrumentCodeRepository, Mockito.times(1))
        .deleteInstrument(USERID);
  }

  @Test
  void generateCode_enrollKo_tooManyRequests() {
    Mockito.when(
            idpayCodeEncryptionService.buildHashedDataBlock(anyString(), anyString(), anyString()))
        .thenReturn(PIN_BLOCK);

    Mockito.when(idpayCodeEncryptionService.encryptSHADataBlock(PIN_BLOCK))
        .thenReturn(ENCRYPTED_DATA_BLOCK);

    Mockito.when(paymentInstrumentCodeRepository.deleteInstrument(USERID))
        .thenReturn(new PaymentInstrumentCode());

    Request request =
        Request.create(
            HttpMethod.PUT, "url", new HashMap<>(), null, new RequestTemplate());
    Mockito.doThrow(new FeignException.TooManyRequests("", request, new byte[0], null))
        .when(walletRestConnector).enrollInstrumentCode(INITIATIVE_ID, USERID);
    try {
      paymentInstrumentCodeService.generateCode(USERID, INITIATIVE_ID);
    } catch (PaymentInstrumentException e) {
      assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(), e.getCode());
    }
    verify(paymentInstrumentCodeRepository, Mockito.times(1))
        .deleteInstrument(USERID);
  }

  @Test
  void generateCode_enrollKo_badRequest() {
    Mockito.when(
            idpayCodeEncryptionService.buildHashedDataBlock(anyString(), anyString(), anyString()))
        .thenReturn(PIN_BLOCK);

    Mockito.when(idpayCodeEncryptionService.encryptSHADataBlock(PIN_BLOCK))
        .thenReturn(ENCRYPTED_DATA_BLOCK);

    Mockito.when(paymentInstrumentCodeRepository.deleteInstrument(USERID))
        .thenReturn(new PaymentInstrumentCode());

    Request request =
        Request.create(
            HttpMethod.PUT, "url", new HashMap<>(), null, new RequestTemplate());
    Mockito.doThrow(new FeignException.BadRequest("", request, new byte[0], null))
        .when(walletRestConnector).enrollInstrumentCode(INITIATIVE_ID, USERID);
    try {
      paymentInstrumentCodeService.generateCode(USERID, INITIATIVE_ID);
    } catch (PaymentInstrumentException e) {
      assertEquals(HttpStatus.BAD_REQUEST.value(), e.getCode());
    }
    verify(paymentInstrumentCodeRepository, Mockito.times(1))
        .deleteInstrument(USERID);
  }

  @Test
  void generateCode_enrollKo_internalServerError() {
    Mockito.when(
            idpayCodeEncryptionService.buildHashedDataBlock(anyString(), anyString(), anyString()))
        .thenReturn(PIN_BLOCK);

    Mockito.when(idpayCodeEncryptionService.encryptSHADataBlock(PIN_BLOCK))
        .thenReturn(ENCRYPTED_DATA_BLOCK);

    Mockito.when(paymentInstrumentCodeRepository.deleteInstrument(USERID))
        .thenReturn(new PaymentInstrumentCode());

    Request request =
        Request.create(
            HttpMethod.PUT, "url", new HashMap<>(), null, new RequestTemplate());
    Mockito.doThrow(new FeignException.InternalServerError("", request, new byte[0], null))
        .when(walletRestConnector).enrollInstrumentCode(INITIATIVE_ID, USERID);
    try {
      paymentInstrumentCodeService.generateCode(USERID, INITIATIVE_ID);
    } catch (PaymentInstrumentException e) {
      assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getCode());
      assertEquals("An error occurred in the microservice wallet", e.getMessage());
    }
    verify(paymentInstrumentCodeRepository, Mockito.times(1))
        .deleteInstrument(USERID);
  }

  @Test
  void codeStatus_true() {
    Mockito.when(paymentInstrumentCodeRepository.findById(USERID))
        .thenReturn(Optional.of(PAYMENT_INSTRUMENT_CODE));

    boolean isIdPayCodeEnabled = paymentInstrumentCodeService.codeStatus(USERID);

    assertTrue(isIdPayCodeEnabled);
  }

  @Test
  void codeStatus_false_code_null(){
    final PaymentInstrumentCode paymentInstrumentCode = PaymentInstrumentCode.builder()
        .userId(USERID)
        .build();
    Mockito.when(paymentInstrumentCodeRepository.findById(USERID)).thenReturn(Optional.of(paymentInstrumentCode));

    boolean isIdPayCodeEnabled = paymentInstrumentCodeService.codeStatus(USERID);

    assertFalse(isIdPayCodeEnabled);
  }

  @Test
  void codeStatus_false() {
    Mockito.when(paymentInstrumentCodeRepository.findById(USERID))
        .thenReturn(Optional.ofNullable(any(PaymentInstrumentCode.class)));

    boolean isIdPayCodeEnabled = paymentInstrumentCodeService.codeStatus(USERID);

    assertFalse(isIdPayCodeEnabled);
  }

  @Test
  void getSecondFactor_ok() {
    Mockito.when(paymentInstrumentCodeRepository.findById(USERID))
        .thenReturn(Optional.of(PAYMENT_INSTRUMENT_CODE));

    String secondFactor = paymentInstrumentCodeService.getSecondFactor(USERID);

    assertEquals(PAYMENT_INSTRUMENT_CODE.getSecondFactor(), secondFactor);
  }

  @Test
  void getSecondFactor_ko() {
    Mockito.when(paymentInstrumentCodeRepository.findById(USERID))
        .thenReturn(Optional.ofNullable(any(PaymentInstrumentCode.class)));

    try {
      paymentInstrumentCodeService.getSecondFactor(USERID);
    } catch (PaymentInstrumentException e) {
      assertEquals(HttpStatus.NOT_FOUND.value(), e.getCode());
      assertEquals("There is not a idpaycode for the userId: " + USERID, e.getMessage());
    }
  }

  @Test
  void verifyPinBlock() {
    Mockito.when(paymentInstrumentCodeRepository.findById(USERID))
        .thenReturn(Optional.of(PAYMENT_INSTRUMENT_CODE));

    Mockito.when(idpayCodeEncryptionService.hashSHADecryptedDataBlock(
            USERID, new PinBlockDTO(PIN_BLOCK, ENCRYPTED_KEY), SALT))
        .thenReturn(SHA256_CODE);

    Mockito.when(idpayCodeEncryptionService.decryptIdpayCode(ENCRYPTED_DATA_BLOCK))
        .thenReturn(SHA256_CODE);

    boolean pinBlockVerified = paymentInstrumentCodeService.verifyPinBlock(
        USERID, new PinBlockDTO(PIN_BLOCK, ENCRYPTED_KEY));

    assertTrue(pinBlockVerified);
  }

  @Test
  void verifyPinBlock_false() {
    Mockito.when(paymentInstrumentCodeRepository.findById(USERID))
        .thenReturn(Optional.of(PAYMENT_INSTRUMENT_CODE));

    Mockito.when(idpayCodeEncryptionService.hashSHADecryptedDataBlock(
            USERID, new PinBlockDTO(PIN_BLOCK, ENCRYPTED_KEY), SALT))
        .thenReturn(SHA256_CODE);

    Mockito.when(idpayCodeEncryptionService.decryptIdpayCode(ENCRYPTED_DATA_BLOCK))
        .thenReturn("SHA256_CODE_INCORRECT");

    boolean pinBlockVerified = paymentInstrumentCodeService.verifyPinBlock(
        USERID, new PinBlockDTO(PIN_BLOCK, ENCRYPTED_KEY));

    assertFalse(pinBlockVerified);
  }

  @Test
  void verifyPinBlock_ko_notFound() {
    Mockito.when(paymentInstrumentCodeRepository.findById(USERID))
        .thenReturn(Optional.ofNullable(any(PaymentInstrumentCode.class)));

    try {
      paymentInstrumentCodeService.verifyPinBlock(USERID, new PinBlockDTO(PIN_BLOCK, ENCRYPTED_KEY));
    } catch (PaymentInstrumentException e) {
      assertEquals(HttpStatus.NOT_FOUND.value(), e.getCode());
      assertEquals("Instrument not found", e.getMessage());
    }
  }

  private void assertions(GenerateCodeRespDTO generateCodeRespDTO) {
    verify(paymentInstrumentCodeRepository, Mockito.times(1))
        .updateCode(anyString(), anyString(), anyString(), anyString(), anyString(), any());
    assertNotNull(generateCodeRespDTO);
    assertEquals(5, generateCodeRespDTO.getIdpayCode().length());
  }

}
