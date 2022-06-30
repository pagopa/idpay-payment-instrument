package it.gov.pagopa.payment.instrument.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants;
import it.gov.pagopa.payment.instrument.dto.DeactivationBodyDTO;
import it.gov.pagopa.payment.instrument.dto.EnrollmentBodyDTO;
import it.gov.pagopa.payment.instrument.dto.InstrumentResponseDTO;
import it.gov.pagopa.payment.instrument.dto.ErrorDTO;
import it.gov.pagopa.payment.instrument.exception.PaymentInstrumentException;
import it.gov.pagopa.payment.instrument.service.PaymentInstrumentService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(value = {
    PaymentInstrumentController.class}, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class PaymentInstrumentControllerTest {

  private static final String BASE_URL = "http://localhost:8080/idpay/instrument";
  private static final String ENROLL_URL = "/enroll/";
  private static final String DEACTIVATE_URL = "/deactivate/";
  private static final String USER_ID = "TEST_USER_ID";
  private static final String INITIATIVE_ID = "TEST_INITIATIVE_ID";
  private static final String HPAN = "TEST_HPAN";
  private static final String CHANNEL = "TEST_CHANNEL";
  private static final LocalDateTime TEST_DATE = LocalDateTime.now();
  private static final int TEST_COUNT = 2;
  private static final EnrollmentBodyDTO ENROLLMENT_BODY_DTO = new EnrollmentBodyDTO(USER_ID,
      INITIATIVE_ID, HPAN, CHANNEL, TEST_DATE);
  private static final EnrollmentBodyDTO ENROLLMENT_BODY_DTO_EMPTY = new EnrollmentBodyDTO("", "",
      "", "", TEST_DATE);
  private static final DeactivationBodyDTO DEACTIVATION_BODY_DTO = new DeactivationBodyDTO(USER_ID,
      INITIATIVE_ID, HPAN, TEST_DATE);
  private static final DeactivationBodyDTO DEACTIVATION_BODY_DTO_EMPTY = new DeactivationBodyDTO("",
      "", "", TEST_DATE);

  @MockBean
  PaymentInstrumentService paymentInstrumentServiceMock;

  @Autowired
  protected MockMvc mvc;

  @Test
  void enroll_ok() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    Mockito.doNothing().when(paymentInstrumentServiceMock)
        .enrollInstrument(INITIATIVE_ID, USER_ID, HPAN, CHANNEL, TEST_DATE);

    Mockito.when(paymentInstrumentServiceMock.countByInitiativeIdAndUserIdAndStatus(INITIATIVE_ID, USER_ID, PaymentInstrumentConstants.STATUS_ACTIVE))
        .thenReturn(TEST_COUNT);

    MvcResult res = mvc.perform(MockMvcRequestBuilders.put(BASE_URL + ENROLL_URL)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(objectMapper.writeValueAsString(ENROLLMENT_BODY_DTO))
            .accept(MediaType.APPLICATION_JSON_VALUE)).andExpect(MockMvcResultMatchers.status().isOk())
        .andReturn();

    InstrumentResponseDTO dto = objectMapper.readValue(res.getResponse().getContentAsString(),
        InstrumentResponseDTO.class);

    assertEquals(TEST_COUNT, dto.getNinstr());
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

    assertEquals(HttpStatus.BAD_REQUEST.value(), error.getCode());
    assertTrue(error.getMessage().contains(PaymentInstrumentConstants.ERROR_MANDATORY_FIELD));
  }

  @Test
  void enroll_already_active() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    Mockito.doThrow(new PaymentInstrumentException(HttpStatus.FORBIDDEN.value(),
            PaymentInstrumentConstants.ERROR_PAYMENT_INSTRUMENT_ALREADY_ACTIVE))
        .when(paymentInstrumentServiceMock)
        .enrollInstrument(INITIATIVE_ID, USER_ID, HPAN, CHANNEL, TEST_DATE);

    MvcResult res = mvc.perform(MockMvcRequestBuilders.put(BASE_URL + ENROLL_URL)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(objectMapper.writeValueAsString(ENROLLMENT_BODY_DTO))
            .accept(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(MockMvcResultMatchers.status().isForbidden()).andReturn();

    ErrorDTO error = objectMapper.readValue(res.getResponse().getContentAsString(), ErrorDTO.class);

    assertEquals(HttpStatus.FORBIDDEN.value(), error.getCode());
    assertEquals(PaymentInstrumentConstants.ERROR_PAYMENT_INSTRUMENT_ALREADY_ACTIVE,
        error.getMessage());
  }

  @Test
  void deactivate_ok() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    Mockito.doNothing().when(paymentInstrumentServiceMock)
        .deactivateInstrument(INITIATIVE_ID, USER_ID, HPAN, TEST_DATE);

    Mockito.when(paymentInstrumentServiceMock.countByInitiativeIdAndUserIdAndStatus(INITIATIVE_ID, USER_ID, PaymentInstrumentConstants.STATUS_ACTIVE))
        .thenReturn(TEST_COUNT);

    MvcResult res = mvc.perform(MockMvcRequestBuilders.delete(BASE_URL + DEACTIVATE_URL)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(objectMapper.writeValueAsString(DEACTIVATION_BODY_DTO))
            .accept(MediaType.APPLICATION_JSON_VALUE)).andExpect(MockMvcResultMatchers.status().isOk())
        .andReturn();

    InstrumentResponseDTO dto = objectMapper.readValue(res.getResponse().getContentAsString(),
        InstrumentResponseDTO.class);

    assertEquals(TEST_COUNT, dto.getNinstr());
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

    assertEquals(HttpStatus.BAD_REQUEST.value(), error.getCode());
    assertTrue(error.getMessage().contains(PaymentInstrumentConstants.ERROR_MANDATORY_FIELD));
  }

  @Test
  void deactivate_not_found() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    Mockito.doThrow(new PaymentInstrumentException(HttpStatus.NOT_FOUND.value(),
            PaymentInstrumentConstants.ERROR_PAYMENT_INSTRUMENT_NOT_FOUND))
        .when(paymentInstrumentServiceMock)
        .deactivateInstrument(INITIATIVE_ID, USER_ID, HPAN, TEST_DATE);

    MvcResult res = mvc.perform(MockMvcRequestBuilders.delete(BASE_URL + DEACTIVATE_URL)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(objectMapper.writeValueAsString(DEACTIVATION_BODY_DTO))
            .accept(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(MockMvcResultMatchers.status().isNotFound()).andReturn();

    ErrorDTO error = objectMapper.readValue(res.getResponse().getContentAsString(), ErrorDTO.class);

    assertEquals(HttpStatus.NOT_FOUND.value(), error.getCode());
    assertEquals(PaymentInstrumentConstants.ERROR_PAYMENT_INSTRUMENT_NOT_FOUND, error.getMessage());
  }
}