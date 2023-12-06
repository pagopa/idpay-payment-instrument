package it.gov.pagopa.payment.instrument.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.gov.pagopa.common.web.dto.ErrorDTO;
import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.payment.instrument.config.ServiceExceptionConfig;
import it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants;
import it.gov.pagopa.payment.instrument.dto.*;
import it.gov.pagopa.payment.instrument.exception.custom.PaymentInstrumentNotFoundException;
import it.gov.pagopa.payment.instrument.exception.custom.UserNotAllowedException;
import it.gov.pagopa.payment.instrument.service.PaymentInstrumentDiscountService;
import it.gov.pagopa.payment.instrument.service.PaymentInstrumentService;
import it.gov.pagopa.payment.instrument.service.idpaycode.PaymentInstrumentCodeService;
import it.gov.pagopa.payment.instrument.test.fakers.GenerateCodeReqDTO;
import it.gov.pagopa.payment.instrument.test.fakers.InstrumentFromDiscountDTOFaker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants.ExceptionCode.*;
import static it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants.ExceptionMessage.ERROR_INSTRUMENT_ALREADY_ASSOCIATED_MSG;
import static it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants.ExceptionMessage.ERROR_INSTRUMENT_NOT_FOUND_MSG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(value = {
    PaymentInstrumentController.class}, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@Import(ServiceExceptionConfig.class)
class PaymentInstrumentControllerTest {

  private static final String BASE_URL = "http://localhost:8080/idpay/instrument";
  private static final String ENROLL_URL = "/enroll";
  private static final String CODE_ENROLL_URL = "/code/enroll";
  private static final String ENROLL_ISSUER_URL = "/hb/enroll";
  private static final String DEACTIVATE_URL = "/deactivate";
  private static final String DISABLE_ALL_URL = "/disableall";
  private static final String ROLLBACK_URL = "/rollback";
  private static final String USER_ID = "TEST_USER_ID";
  private static final String INITIATIVE_ID = "TEST_INITIATIVE_ID";
  private static final String HPAN = "TEST_HPAN";
  private static final String ID_WALLET = "ID_WALLET";
  private static final String INSTRUMENT_ID = "INSTRUMENT_ID";
  private static final String CHANNEL = "TEST_CHANNEL";
  private static final String BRAND_LOGO = "BRAND_LOGO";
  private static final String INSTRUMENT_TYPE = "TEST_INSTRUMENT_TYPE";

  private static final String GETHPAN_URL = "/" + INITIATIVE_ID + "/" + USER_ID;
  private static final EnrollmentBodyDTO ENROLLMENT_BODY_DTO = new EnrollmentBodyDTO(USER_ID,
      INITIATIVE_ID, ID_WALLET, CHANNEL, INSTRUMENT_TYPE);
  private static final EnrollmentBodyDTO ENROLLMENT_BODY_DTO_EMPTY = new EnrollmentBodyDTO("", "",
      "", "", "");
  private static final DeactivationBodyDTO DEACTIVATION_BODY_DTO = new DeactivationBodyDTO(USER_ID,
      INITIATIVE_ID, INSTRUMENT_ID);

  private static final BaseEnrollmentBodyDTO BASE_ENROLLMENT_BODY_DTO = new BaseEnrollmentBodyDTO(USER_ID,
          INITIATIVE_ID, CHANNEL, INSTRUMENT_TYPE);
  private static final BaseEnrollmentBodyDTO BASE_ENROLLMENT_BODY_DTO_EMPTY = new BaseEnrollmentBodyDTO("",
          "", "", "");
  private static final DeactivationBodyDTO DEACTIVATION_BODY_DTO_EMPTY = new DeactivationBodyDTO("",
      "", "");

  private static final HpanDTO HPAN_DTO_TEST = new HpanDTO(HPAN, CHANNEL, BRAND_LOGO, BRAND_LOGO, ID_WALLET,
      INSTRUMENT_ID, CHANNEL, PaymentInstrumentConstants.INSTRUMENT_TYPE_CARD, LocalDateTime.now());

  private static final HpanGetDTO HPANGETDTO = new HpanGetDTO();

  private static final String GETHPANISSUER_URL = "/" + INITIATIVE_ID + "/" + USER_ID + "/" + CHANNEL;
  private static final InstrumentIssuerDTO ENROLLMENT_ISSUER_BODY_DTO = new InstrumentIssuerDTO(INITIATIVE_ID, USER_ID, "HPAN", "ISSUER", PaymentInstrumentConstants.INSTRUMENT_TYPE_CARD,"", "", "");
  private static final InstrumentIssuerDTO ENROLLMENT_ISSUER_BODY_DTO_EMPTY = new InstrumentIssuerDTO(INITIATIVE_ID, USER_ID, "", "", "","", "", "");
  private static final String GET_INSTRUMENT_INITIATIVES_DETAIL = "/initiatives/" + ID_WALLET + "/" + USER_ID + "/detail";
  private static final String ENROLL_DISCOUNT_URL = "/discount/enroll";
  private static final String ENROLL_CODE_URL = "/generate-code/" + USER_ID;
  private static final String CODE_STATUS_URL = "/code/status/" + USER_ID;
  private static final String GET_SECOND_FACTOR_URL = "/code/secondFactor/" + USER_ID;

  private static final String VERIFY_PIN_BLOCK_URL = "/code/verify/" + USER_ID;
  @MockBean
  PaymentInstrumentService paymentInstrumentServiceMock;

  @MockBean
  PaymentInstrumentDiscountService paymentInstrumentDiscountService;

  @MockBean
  PaymentInstrumentCodeService paymentInstrumentCodeService;

  @Autowired
  protected MockMvc mvc;

  @Autowired
  ObjectMapper objectMapper;

  @Test
  void enroll_ok() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    mvc.perform(MockMvcRequestBuilders.put(BASE_URL + ENROLL_URL)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(objectMapper.writeValueAsString(ENROLLMENT_BODY_DTO))
            .accept(MediaType.APPLICATION_JSON_VALUE)).andExpect(MockMvcResultMatchers.status().isOk())
        .andReturn();

  }

  @Test
  void enroll_empty_body() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    MvcResult res = mvc.perform(MockMvcRequestBuilders.put(BASE_URL + ENROLL_URL)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(objectMapper.writeValueAsString(ENROLLMENT_BODY_DTO_EMPTY))
            .accept(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(MockMvcResultMatchers.status().isBadRequest()).andReturn();

    ErrorDTO error = objectMapper.readValue(res.getResponse().getContentAsString(), ErrorDTO.class);

    assertEquals(INVALID_REQUEST, error.getCode());
    assertTrue(error.getMessage().contains(PaymentInstrumentConstants.ERROR_MANDATORY_FIELD));
  }

  @Test
  void enroll_already_active() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    Mockito.doThrow(new UserNotAllowedException(ERROR_INSTRUMENT_ALREADY_ASSOCIATED_MSG))
        .when(paymentInstrumentServiceMock)
        .enrollInstrument(INITIATIVE_ID, USER_ID, ID_WALLET, CHANNEL, INSTRUMENT_TYPE);

    MvcResult res = mvc.perform(MockMvcRequestBuilders.put(BASE_URL + ENROLL_URL)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(objectMapper.writeValueAsString(ENROLLMENT_BODY_DTO))
            .accept(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(MockMvcResultMatchers.status().isForbidden()).andReturn();

    ErrorDTO errorResult = objectMapper.readValue(res.getResponse().getContentAsString(), ErrorDTO.class);

    assertEquals(new ErrorDTO(INSTRUMENT_ALREADY_ASSOCIATED,ERROR_INSTRUMENT_ALREADY_ASSOCIATED_MSG), errorResult);

  }

  @Test
  void deactivate_ok() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    mvc.perform(MockMvcRequestBuilders.delete(BASE_URL + DEACTIVATE_URL)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(objectMapper.writeValueAsString(DEACTIVATION_BODY_DTO))
            .accept(MediaType.APPLICATION_JSON_VALUE)).andExpect(MockMvcResultMatchers.status().isOk())
        .andReturn();

  }

  @Test
  void deactivate_empty_body() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    MvcResult res = mvc.perform(MockMvcRequestBuilders.delete(BASE_URL + DEACTIVATE_URL)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(objectMapper.writeValueAsString(DEACTIVATION_BODY_DTO_EMPTY))
            .accept(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(MockMvcResultMatchers.status().isBadRequest()).andReturn();

    ErrorDTO error = objectMapper.readValue(res.getResponse().getContentAsString(), ErrorDTO.class);

    assertEquals(INVALID_REQUEST, error.getCode());
    assertTrue(error.getMessage().contains(PaymentInstrumentConstants.ERROR_MANDATORY_FIELD));
  }

  @Test
  void deactivate_not_found() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    Mockito.doThrow(new PaymentInstrumentNotFoundException(ERROR_INSTRUMENT_NOT_FOUND_MSG))
        .when(paymentInstrumentServiceMock)
        .deactivateInstrument(INITIATIVE_ID, USER_ID, INSTRUMENT_ID);

    MvcResult res = mvc.perform(MockMvcRequestBuilders.delete(BASE_URL + DEACTIVATE_URL)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(objectMapper.writeValueAsString(DEACTIVATION_BODY_DTO))
            .accept(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(MockMvcResultMatchers.status().isNotFound()).andReturn();

    ErrorDTO error = objectMapper.readValue(res.getResponse().getContentAsString(), ErrorDTO.class);

    assertEquals(new ErrorDTO(INSTRUMENT_NOT_FOUND, ERROR_INSTRUMENT_NOT_FOUND_MSG), error);
  }

  @Test
  void deactivate_ko_serviceException() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    Mockito.doThrow(new ServiceException("DUMMY_EXCEPTION_CODE", "DUMMY_EXCEPTION_MESSAGE"))
            .when(paymentInstrumentServiceMock)
            .deactivateInstrument(INITIATIVE_ID, USER_ID, INSTRUMENT_ID);

    MvcResult res = mvc.perform(MockMvcRequestBuilders.delete(BASE_URL + DEACTIVATE_URL)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(objectMapper.writeValueAsString(DEACTIVATION_BODY_DTO))
                    .accept(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isInternalServerError()).andReturn();

    ErrorDTO error = objectMapper.readValue(res.getResponse().getContentAsString(), ErrorDTO.class);

    assertEquals(new ErrorDTO("DUMMY_EXCEPTION_CODE", "DUMMY_EXCEPTION_MESSAGE"), error);
  }

  @Test
  void getHpan_ok() throws Exception {
    List<HpanDTO> hpanDTOList = new ArrayList<>();
    hpanDTOList.add(HPAN_DTO_TEST);
    HPANGETDTO.setInstrumentList(hpanDTOList);

    Mockito.when(paymentInstrumentServiceMock.getHpan(INITIATIVE_ID, USER_ID))
        .thenReturn(HPANGETDTO);

    mvc.perform(
            MockMvcRequestBuilders.get(BASE_URL + GETHPAN_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andReturn();
  }
  
  @Test
  void disableAllInstrument_ok() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    UnsubscribeBodyDTO unsubscribeBodyDTO = new UnsubscribeBodyDTO(INITIATIVE_ID, USER_ID,
        LocalDateTime.now().toString());

    Mockito.doNothing().when(paymentInstrumentServiceMock)
        .deactivateAllInstruments(INITIATIVE_ID, USER_ID, LocalDateTime.now().toString());

    mvc.perform(
            MockMvcRequestBuilders.delete(BASE_URL + DISABLE_ALL_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(unsubscribeBodyDTO))
                .accept(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(MockMvcResultMatchers.status().isNoContent())
        .andReturn();
  }

  @Test
  void getHpanFromIssuer_ok() throws Exception {
    List<HpanDTO> hpanDTOList = new ArrayList<>();
    hpanDTOList.add(HPAN_DTO_TEST);
    HPANGETDTO.setInstrumentList(hpanDTOList);

    Mockito.when(paymentInstrumentServiceMock.getHpanFromIssuer(INITIATIVE_ID, USER_ID, CHANNEL))
        .thenReturn(HPANGETDTO);

    mvc.perform(
            MockMvcRequestBuilders.get(BASE_URL + GETHPANISSUER_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andReturn();
  }

  @Test
  void enroll_issuer_ok() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    mvc.perform(MockMvcRequestBuilders.put(BASE_URL + ENROLL_ISSUER_URL)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(objectMapper.writeValueAsString(ENROLLMENT_ISSUER_BODY_DTO))
            .accept(MediaType.APPLICATION_JSON_VALUE)).andExpect(MockMvcResultMatchers.status().isOk())
        .andReturn();

  }

  @Test
  void enroll_issuer_empty_body() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    MvcResult res = mvc.perform(MockMvcRequestBuilders.put(BASE_URL + ENROLL_ISSUER_URL)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(objectMapper.writeValueAsString(ENROLLMENT_ISSUER_BODY_DTO_EMPTY))
            .accept(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(MockMvcResultMatchers.status().isBadRequest()).andReturn();

    ErrorDTO error = objectMapper.readValue(res.getResponse().getContentAsString(), ErrorDTO.class);

    assertEquals(INVALID_REQUEST, error.getCode());
    assertTrue(error.getMessage().contains(PaymentInstrumentConstants.ERROR_MANDATORY_FIELD));
  }

  @Test
  void enroll_issuer_already_active() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    Mockito.doThrow(new UserNotAllowedException(ERROR_INSTRUMENT_ALREADY_ASSOCIATED_MSG))
        .when(paymentInstrumentServiceMock)
        .enrollFromIssuer(Mockito.any());

    MvcResult res = mvc.perform(MockMvcRequestBuilders.put(BASE_URL + ENROLL_ISSUER_URL)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(objectMapper.writeValueAsString(ENROLLMENT_ISSUER_BODY_DTO))
            .accept(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(MockMvcResultMatchers.status().isForbidden()).andReturn();

    ErrorDTO error = objectMapper.readValue(res.getResponse().getContentAsString(), ErrorDTO.class);

    assertEquals(new ErrorDTO(INSTRUMENT_ALREADY_ASSOCIATED, ERROR_INSTRUMENT_ALREADY_ASSOCIATED_MSG), error);
  }

  @Test
  void get_instrument_initiatives_detail() throws Exception {
    Mockito.when(paymentInstrumentServiceMock.getInstrumentInitiativesDetail(USER_ID, ID_WALLET, new ArrayList<>()))
            .thenReturn(new InstrumentDetailDTO());

    mvc.perform(
                    MockMvcRequestBuilders.get(BASE_URL + GET_INSTRUMENT_INITIATIVES_DETAIL)
                            .contentType(MediaType.APPLICATION_JSON_VALUE)
                            .accept(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();
  }

  @Test
  void enroll_discount_ok() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    mvc.perform(MockMvcRequestBuilders.put(BASE_URL + ENROLL_DISCOUNT_URL)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(objectMapper.writeValueAsString(InstrumentFromDiscountDTOFaker.mockInstance(1)))
            .accept(MediaType.APPLICATION_JSON_VALUE)).andExpect(MockMvcResultMatchers.status().isOk())
        .andReturn();

  }

  @Test
  void enroll_discount_empty_body() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    mvc.perform(MockMvcRequestBuilders.put(BASE_URL + ENROLL_DISCOUNT_URL)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .accept(MediaType.APPLICATION_JSON_VALUE)).andExpect(MockMvcResultMatchers.status().isInternalServerError())
        .andReturn();

  }
  @Test
  void rollback() throws Exception {
    Mockito.doNothing().when(paymentInstrumentServiceMock).rollback(INITIATIVE_ID, USER_ID);
    mvc.perform(
                    MockMvcRequestBuilders.put(BASE_URL + ROLLBACK_URL + "/" + INITIATIVE_ID + "/" + USER_ID)
                            .contentType(MediaType.APPLICATION_JSON_VALUE)
                            .accept(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isNoContent())
            .andReturn();
  }

  @Test
  void enroll_code_ok() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    mvc.perform(MockMvcRequestBuilders.put(BASE_URL + CODE_ENROLL_URL)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(objectMapper.writeValueAsString(BASE_ENROLLMENT_BODY_DTO))
                    .accept(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();

  }

  @Test
  void enroll_code_ko() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    MvcResult result = mvc.perform(MockMvcRequestBuilders.put(BASE_URL + CODE_ENROLL_URL)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(objectMapper.writeValueAsString(BASE_ENROLLMENT_BODY_DTO_EMPTY))
                    .accept(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andReturn();

    ErrorDTO error = objectMapper.readValue(result.getResponse().getContentAsString(), ErrorDTO.class);

    assertEquals(INVALID_REQUEST, error.getCode());
    assertTrue(error.getMessage().contains(PaymentInstrumentConstants.ERROR_MANDATORY_FIELD));

  }

  @Test
  void enroll_idpayCode_ok() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    mvc.perform(MockMvcRequestBuilders.post(BASE_URL + ENROLL_CODE_URL)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(objectMapper.writeValueAsString(GenerateCodeReqDTO.mockInstance(1, true)))
            .accept(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andReturn();
  }

  @Test
  void enroll_code_empty_body() throws Exception {
    mvc.perform(MockMvcRequestBuilders.post(BASE_URL + ENROLL_CODE_URL)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .accept(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andReturn();
  }

  @Test
  void idpayCode_status_ok() throws Exception {
    mvc.perform(MockMvcRequestBuilders.get(BASE_URL + CODE_STATUS_URL)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .accept(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andReturn();
  }

  @Test
  void getSecondFactor_ok() throws Exception {
    mvc.perform(MockMvcRequestBuilders.get(BASE_URL + GET_SECOND_FACTOR_URL)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .accept(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andReturn();
  }

  @Test
  void verify_pinBlock_ok() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    mvc.perform(MockMvcRequestBuilders.put(BASE_URL + VERIFY_PIN_BLOCK_URL)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(objectMapper.writeValueAsString(new PinBlockDTO("test", "test")))
            .accept(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andReturn();
  }
}