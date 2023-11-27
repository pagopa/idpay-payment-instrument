package it.gov.pagopa.payment.instrument.service.idpaycode;

import static it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants.ExceptionCode.*;
import static it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants.ExceptionMessage.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import feign.Request;
import feign.Request.HttpMethod;
import feign.RequestTemplate;
import it.gov.pagopa.common.web.dto.ErrorDTO;
import it.gov.pagopa.payment.instrument.connector.WalletRestConnector;
import it.gov.pagopa.payment.instrument.dto.EncryptedDataBlock;
import it.gov.pagopa.payment.instrument.dto.GenerateCodeRespDTO;
import it.gov.pagopa.payment.instrument.dto.PinBlockDTO;
import it.gov.pagopa.payment.instrument.exception.custom.IDPayCodeNotFoundException;
import it.gov.pagopa.payment.instrument.exception.custom.TooManyRequestsException;
import it.gov.pagopa.payment.instrument.exception.custom.UserNotOnboardedException;
import it.gov.pagopa.payment.instrument.exception.custom.WalletInvocationException;
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
  void generateCode_enrollKo_notFound() throws JsonProcessingException {
    Mockito.when(
            idpayCodeEncryptionService.buildHashedDataBlock(anyString(), anyString(), anyString()))
        .thenReturn(PIN_BLOCK);

    Mockito.when(idpayCodeEncryptionService.encryptSHADataBlock(PIN_BLOCK))
        .thenReturn(ENCRYPTED_DATA_BLOCK);

    Mockito.doNothing().when(paymentInstrumentCodeRepository).deleteById(USERID);

    Request request =
        Request.create(
            HttpMethod.PUT, "url", new HashMap<>(), null, new RequestTemplate());

    Mockito.doThrow(new FeignException.NotFound("Initiative not found", request, new byte[0], null))
        .when(walletRestConnector).enrollInstrumentCode(INITIATIVE_ID, USERID);

    ErrorDTO errorDTOExpected = new ErrorDTO("WALLET_USER_NOT_ONBOARDED", "initiative not found");
    Mockito.when(utilities.exceptionErrorDTOConverter(Mockito.any())).thenReturn(errorDTOExpected);

    try {
      paymentInstrumentCodeService.generateCode(USERID, INITIATIVE_ID);
    } catch (UserNotOnboardedException e) {
      assertEquals(USER_NOT_ONBOARDED, e.getCode());
      assertEquals(String.format(ERROR_USER_NOT_ONBOARDED_MSG, INITIATIVE_ID), e.getMessage());
    }
    verify(paymentInstrumentCodeRepository, Mockito.times(1))
        .deleteById(USERID);
  }

  @Test
  void generateCode_enrollKo_tooManyRequests() throws JsonProcessingException {
    Mockito.when(
            idpayCodeEncryptionService.buildHashedDataBlock(anyString(), anyString(), anyString()))
        .thenReturn(PIN_BLOCK);

    Mockito.when(idpayCodeEncryptionService.encryptSHADataBlock(PIN_BLOCK))
        .thenReturn(ENCRYPTED_DATA_BLOCK);

    Mockito.doNothing().when(paymentInstrumentCodeRepository).deleteById(USERID);

    Request request =
        Request.create(
            HttpMethod.PUT, "url", new HashMap<>(), null, new RequestTemplate());
    Mockito.doThrow(new FeignException.TooManyRequests("", request, new byte[0], null))
        .when(walletRestConnector).enrollInstrumentCode(INITIATIVE_ID, USERID);

    ErrorDTO errorDTOExpected = new ErrorDTO("WALLET_TOO_MANY_REQUESTS", "too many request");
    Mockito.when(utilities.exceptionErrorDTOConverter(Mockito.any())).thenReturn(errorDTOExpected);
    try {
      paymentInstrumentCodeService.generateCode(USERID, INITIATIVE_ID);
    } catch (TooManyRequestsException e) {
      assertEquals(TOO_MANY_REQUESTS, e.getCode());
      assertEquals(ERROR_TOO_MANY_REQUESTS_WALLET_MSG, e.getMessage());
    }
    verify(paymentInstrumentCodeRepository, Mockito.times(1))
        .deleteById(USERID);
  }

  @Test
  void generateCode_enrollKo_internalServerError() {
    Mockito.when(
            idpayCodeEncryptionService.buildHashedDataBlock(anyString(), anyString(), anyString()))
        .thenReturn(PIN_BLOCK);

    Mockito.when(idpayCodeEncryptionService.encryptSHADataBlock(PIN_BLOCK))
        .thenReturn(ENCRYPTED_DATA_BLOCK);

    Mockito.doNothing().when(paymentInstrumentCodeRepository).deleteById(USERID);

    Request request =
        Request.create(
            HttpMethod.PUT, "url", new HashMap<>(), null, new RequestTemplate());
    Mockito.doThrow(new FeignException.InternalServerError("", request, new byte[0], null))
        .when(walletRestConnector).enrollInstrumentCode(INITIATIVE_ID, USERID);

    ErrorDTO errorDTOExpected = new ErrorDTO("WALLET_GENERIC_ERROR", "generic error");
    Mockito.when(utilities.exceptionErrorDTOConverter(Mockito.any())).thenReturn(errorDTOExpected);

    try {
      paymentInstrumentCodeService.generateCode(USERID, INITIATIVE_ID);
    } catch (WalletInvocationException e) {
      assertEquals(GENERIC_ERROR, e.getCode());
      assertEquals(ERROR_INVOCATION_WALLET_MSG, e.getMessage());
    }
    verify(paymentInstrumentCodeRepository, Mockito.times(1))
        .deleteById(USERID);
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
    } catch (IDPayCodeNotFoundException e) {
      assertEquals(IDPAYCODE_NOT_FOUND, e.getCode());
      assertEquals(ERROR_IDPAYCODE_NOT_FOUND_MSG, e.getMessage());
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
    } catch (IDPayCodeNotFoundException e) {
      assertEquals(IDPAYCODE_NOT_FOUND, e.getCode());
      assertEquals(ERROR_IDPAYCODE_NOT_FOUND_MSG, e.getMessage());
    }
  }

  private void assertions(GenerateCodeRespDTO generateCodeRespDTO) {
    verify(paymentInstrumentCodeRepository, Mockito.times(1))
        .updateCode(anyString(), anyString(), anyString(), anyString(), anyString(), any());
    assertNotNull(generateCodeRespDTO);
    assertEquals(5, generateCodeRespDTO.getIdpayCode().length());
  }

}
