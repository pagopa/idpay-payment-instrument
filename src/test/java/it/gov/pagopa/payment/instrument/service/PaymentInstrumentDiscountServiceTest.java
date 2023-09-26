package it.gov.pagopa.payment.instrument.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.gov.pagopa.payment.instrument.dto.BaseEnrollmentBodyDTO;
import it.gov.pagopa.payment.instrument.dto.InstrumentFromDiscountDTO;
import it.gov.pagopa.payment.instrument.dto.RuleEngineRequestDTO;
import it.gov.pagopa.payment.instrument.dto.mapper.BaseEnrollmentBodyDTO2PaymentInstrument;
import it.gov.pagopa.payment.instrument.dto.mapper.InstrumentFromDiscountDTO2PaymentInstrumentMapper;
import it.gov.pagopa.payment.instrument.dto.mapper.MessageMapper;
import it.gov.pagopa.payment.instrument.event.producer.ErrorProducer;
import it.gov.pagopa.payment.instrument.event.producer.RuleEngineProducer;
import it.gov.pagopa.payment.instrument.exception.PaymentInstrumentException;
import it.gov.pagopa.payment.instrument.model.PaymentInstrument;
import it.gov.pagopa.payment.instrument.repository.PaymentInstrumentRepository;
import it.gov.pagopa.payment.instrument.service.idpaycode.PaymentInstrumentCodeService;
import it.gov.pagopa.payment.instrument.test.fakers.BaseEnrollmentDTOFaker;
import it.gov.pagopa.payment.instrument.test.fakers.InstrumentFromDiscountDTOFaker;
import it.gov.pagopa.payment.instrument.test.fakers.PaymentInstrumentFaker;
import it.gov.pagopa.payment.instrument.utils.AuditUtilities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.support.MessageBuilder;

@ExtendWith(MockitoExtension.class)
class PaymentInstrumentDiscountServiceTest {

  @Mock
  private InstrumentFromDiscountDTO2PaymentInstrumentMapper instrumentFromDiscountDTO2PaymentInstrumentMapper;
  @Mock
  private BaseEnrollmentBodyDTO2PaymentInstrument baseEnrollmentBodyDTO2PaymentInstrument;
  @Mock
  private PaymentInstrumentRepository paymentInstrumentRepository;
  @Mock
  private MessageMapper messageMapper;
  @Mock
  private ErrorProducer errorProducer;
  @Mock
  private RuleEngineProducer ruleEngineProducer;
  @Mock
  private AuditUtilities auditUtilities;

  @Mock
  private PaymentInstrumentCodeService paymentInstrumentCodeService;

  PaymentInstrumentDiscountService paymentInstrumentDiscountService;

  @BeforeEach
  void setUp() {
    paymentInstrumentDiscountService = new PaymentInstrumentDiscountServiceImpl(
        instrumentFromDiscountDTO2PaymentInstrumentMapper, baseEnrollmentBodyDTO2PaymentInstrument, paymentInstrumentRepository,
        messageMapper, "ruleEngineServer", "ruleEngineTopic", errorProducer, ruleEngineProducer, auditUtilities,
        paymentInstrumentCodeService);
  }

  @Test
  void enrollDiscountInitiative() {
    PaymentInstrument paymentInstrument = PaymentInstrumentFaker.mockInstance(1);
    InstrumentFromDiscountDTO instrumentFromDiscountDTO = InstrumentFromDiscountDTOFaker.mockInstance(
        1);

    when(instrumentFromDiscountDTO2PaymentInstrumentMapper.apply(any())).thenReturn(
        paymentInstrument);
    when(messageMapper.apply(any())).thenReturn(
        MessageBuilder.withPayload(new RuleEngineRequestDTO(
        )).build());
    doNothing().when(ruleEngineProducer).sendInstruments(any());

    paymentInstrumentDiscountService.enrollDiscountInitiative(instrumentFromDiscountDTO);

    verify(paymentInstrumentRepository, times(1)).save(any());
    verify(errorProducer, times(0)).sendEvent(any());
  }

  @Test
  void enrollDiscountInitiativeError() {
    PaymentInstrument paymentInstrument = PaymentInstrumentFaker.mockInstance(1);
    InstrumentFromDiscountDTO instrumentFromDiscountDTO = InstrumentFromDiscountDTOFaker.mockInstance(
        1);

    when(instrumentFromDiscountDTO2PaymentInstrumentMapper.apply(any())).thenReturn(
        paymentInstrument);
    doThrow(new PaymentInstrumentException(HttpStatus.BAD_REQUEST.value(), "")).when(
        ruleEngineProducer).sendInstruments(any());
    when(messageMapper.apply(any())).thenReturn(
        MessageBuilder.withPayload(new RuleEngineRequestDTO(
        )).build());

    paymentInstrumentDiscountService.enrollDiscountInitiative(instrumentFromDiscountDTO);

    verify(paymentInstrumentRepository, times(1)).save(any());
    verify(errorProducer, times(1)).sendEvent(any());
  }

  @Test
  void enrollInstrumentCode() {
    // Given
    BaseEnrollmentBodyDTO enrollmentRequest = BaseEnrollmentDTOFaker.mockInstance(1);

    PaymentInstrument paymentInstrument = PaymentInstrumentFaker.mockInstance(1);

    when(paymentInstrumentCodeService.codeStatus(paymentInstrument.getUserId())).thenReturn(true);

    when(baseEnrollmentBodyDTO2PaymentInstrument.apply(any(), anyString()))
            .thenReturn(paymentInstrument);

    when(messageMapper.apply(any())).thenReturn(
            MessageBuilder.withPayload(new RuleEngineRequestDTO(
            )).build());

    doNothing().when(ruleEngineProducer).sendInstruments(any());

    // When
    paymentInstrumentDiscountService.enrollInstrumentCode(enrollmentRequest);

    // Then
    verify(paymentInstrumentRepository, times(1)).save(any());
    verify(errorProducer, times(0)).sendEvent(any());

  }

  @Test
  void enrollInstrumentCodeError() {
    // Given
    BaseEnrollmentBodyDTO enrollmentRequest = BaseEnrollmentDTOFaker.mockInstance(1);

    PaymentInstrument paymentInstrument = PaymentInstrumentFaker.mockInstance(1);

    when(paymentInstrumentCodeService.codeStatus(paymentInstrument.getUserId())).thenReturn(true);

    when(baseEnrollmentBodyDTO2PaymentInstrument.apply(any(), anyString()))
            .thenReturn(paymentInstrument);

    doThrow(new PaymentInstrumentException(HttpStatus.BAD_REQUEST.value(), "")).when(
            ruleEngineProducer).sendInstruments(any());

    when(messageMapper.apply(any())).thenReturn(
            MessageBuilder.withPayload(new RuleEngineRequestDTO(
            )).build());

    paymentInstrumentDiscountService.enrollInstrumentCode(enrollmentRequest);

    verify(paymentInstrumentRepository, times(1)).save(any());
    verify(errorProducer, times(1)).sendEvent(any());
  }

  @Test
  void enrollInstrumentCode_codeStatus_false() {
    BaseEnrollmentBodyDTO enrollmentRequest = BaseEnrollmentDTOFaker.mockInstance(1);

    PaymentInstrument paymentInstrument = PaymentInstrumentFaker.mockInstance(1);

    when(paymentInstrumentCodeService.codeStatus(paymentInstrument.getUserId())).thenReturn(false);

    try{
      paymentInstrumentDiscountService.enrollInstrumentCode(enrollmentRequest);
      fail();
    }catch (PaymentInstrumentException e){
      assertEquals(403, e.getCode());
      assertEquals("IdpayCode must be generated", e.getMessage());
    }
  }
}