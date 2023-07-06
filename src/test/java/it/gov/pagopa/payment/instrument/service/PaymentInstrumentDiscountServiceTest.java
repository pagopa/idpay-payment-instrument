package it.gov.pagopa.payment.instrument.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.gov.pagopa.payment.instrument.dto.InstrumentFromDiscountDTO;
import it.gov.pagopa.payment.instrument.dto.RuleEngineRequestDTO;
import it.gov.pagopa.payment.instrument.dto.mapper.InstrumentFromDiscountDTO2PaymentInstrumentMapper;
import it.gov.pagopa.payment.instrument.dto.mapper.MessageMapper;
import it.gov.pagopa.payment.instrument.event.producer.ErrorProducer;
import it.gov.pagopa.payment.instrument.event.producer.RuleEngineProducer;
import it.gov.pagopa.payment.instrument.exception.PaymentInstrumentException;
import it.gov.pagopa.payment.instrument.model.PaymentInstrument;
import it.gov.pagopa.payment.instrument.repository.PaymentInstrumentRepository;
import it.gov.pagopa.payment.instrument.test.fakers.InstrumentFromDiscountDTOFaker;
import it.gov.pagopa.payment.instrument.test.fakers.PaymentInstrumentFaker;
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
  private PaymentInstrumentRepository paymentInstrumentRepository;
  @Mock
  private MessageMapper messageMapper;
  @Mock
  private ErrorProducer errorProducer;
  @Mock
  private RuleEngineProducer ruleEngineProducer;

  PaymentInstrumentDiscountService paymentInstrumentDiscountService;

  @BeforeEach
  void setUp() {
    paymentInstrumentDiscountService = new PaymentInstrumentDiscountServiceImpl(
        instrumentFromDiscountDTO2PaymentInstrumentMapper, paymentInstrumentRepository,
        messageMapper, "ruleEngineServer", "ruleEngineTopic", errorProducer, ruleEngineProducer);
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
}