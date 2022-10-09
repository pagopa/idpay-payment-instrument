package it.gov.pagopa.payment.instrument.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants;
import it.gov.pagopa.payment.instrument.dto.HpanDTO;
import it.gov.pagopa.payment.instrument.dto.HpanGetDTO;
import it.gov.pagopa.payment.instrument.dto.RTDOperationDTO;
import it.gov.pagopa.payment.instrument.dto.RuleEngineQueueDTO;
import it.gov.pagopa.payment.instrument.dto.mapper.MessageMapper;
import it.gov.pagopa.payment.instrument.event.ErrorProducer;
import it.gov.pagopa.payment.instrument.event.RTDProducer;
import it.gov.pagopa.payment.instrument.event.RuleEngineProducer;
import it.gov.pagopa.payment.instrument.exception.PaymentInstrumentException;
import it.gov.pagopa.payment.instrument.model.PaymentInstrument;
import it.gov.pagopa.payment.instrument.repository.PaymentInstrumentRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(value = {PaymentInstrumentService.class})
class PaymentInstrumentServiceTest {

  @MockBean
  PaymentInstrumentRepository paymentInstrumentRepositoryMock;
  @MockBean
  RuleEngineProducer producer;
  @MockBean
  RTDProducer rtdProducer;
  @MockBean
  MessageMapper messageMapper;
  @MockBean
  ErrorProducer errorProducer;
  @Autowired
  PaymentInstrumentService paymentInstrumentService;

  private static final String USER_ID = "TEST_USER_ID";
  private static final String USER_ID_FAIL = "TEST_USER_ID_FAIL";
  private static final String INITIATIVE_ID = "TEST_INITIATIVE_ID";
  private static final String INITIATIVE_ID_OTHER = "TEST_INITIATIVE_ID_OTHER";
  private static final String HPAN = "TEST_HPAN";
  private static final String CHANNEL = "TEST_CHANNEL";
  private static final LocalDateTime TEST_DATE = LocalDateTime.now();
  private static final int TEST_COUNT = 2;
  private static final PaymentInstrument TEST_INSTRUMENT = new PaymentInstrument(INITIATIVE_ID,
      USER_ID, HPAN, PaymentInstrumentConstants.STATUS_ACTIVE, CHANNEL, TEST_DATE);
  private static final PaymentInstrument TEST_INACTIVE_INSTRUMENT = new PaymentInstrument(
      INITIATIVE_ID,
      USER_ID, HPAN, PaymentInstrumentConstants.STATUS_INACTIVE, CHANNEL, TEST_DATE);

  static {
    TEST_INACTIVE_INSTRUMENT.setRequestDeactivationDate(TEST_DATE);
  }

  @Test
  void enrollInstrument_ok_empty() {
    Mockito.when(paymentInstrumentRepositoryMock.findByHpanAndStatus(HPAN,
        PaymentInstrumentConstants.STATUS_ACTIVE)).thenReturn(new ArrayList<>());

    Mockito.when(paymentInstrumentRepositoryMock.countByHpanAndStatus(HPAN,
        PaymentInstrumentConstants.STATUS_ACTIVE)).thenReturn(0);

    try {
      paymentInstrumentService.enrollInstrument(INITIATIVE_ID, USER_ID, HPAN, CHANNEL, TEST_DATE);
    } catch (PaymentInstrumentException e) {
      Assertions.fail();
    }
  }

  @Test
  void enrollInstrument_ok_other_initiative() {
    Mockito.when(paymentInstrumentRepositoryMock.findByHpanAndStatus(HPAN,
        PaymentInstrumentConstants.STATUS_ACTIVE)).thenReturn(List.of(TEST_INSTRUMENT));

    Mockito.when(paymentInstrumentRepositoryMock.countByHpanAndStatus(HPAN,
        PaymentInstrumentConstants.STATUS_ACTIVE)).thenReturn(1);

    try {
      paymentInstrumentService.enrollInstrument(INITIATIVE_ID_OTHER, USER_ID, HPAN, CHANNEL,
          TEST_DATE);
    } catch (PaymentInstrumentException e) {
      Assertions.fail();
    }
  }

  @Test
  void enrollInstrument_ok_idemp() {
    Mockito.when(paymentInstrumentRepositoryMock.findByHpanAndStatus(HPAN,
        PaymentInstrumentConstants.STATUS_ACTIVE)).thenReturn(List.of(TEST_INSTRUMENT));

    try {
      paymentInstrumentService.enrollInstrument(INITIATIVE_ID, USER_ID, HPAN, CHANNEL, TEST_DATE);
    } catch (PaymentInstrumentException e) {
      Assertions.fail();
    }
  }

  @Test
  void enrollInstrument_ok_already_active() {
    Mockito.when(paymentInstrumentRepositoryMock.findByHpanAndStatus(HPAN,
        PaymentInstrumentConstants.STATUS_ACTIVE)).thenReturn(List.of(TEST_INSTRUMENT));

    try {
      paymentInstrumentService.enrollInstrument(INITIATIVE_ID, USER_ID_FAIL, HPAN, CHANNEL,
          TEST_DATE);
    } catch (PaymentInstrumentException e) {
      assertEquals(HttpStatus.FORBIDDEN.value(), e.getCode());
      assertEquals(PaymentInstrumentConstants.ERROR_PAYMENT_INSTRUMENT_ALREADY_ACTIVE,
          e.getMessage());
    }
  }
  @Test
  void deactiveInstrument_ko_rule_engine() {
    Mockito.when(
            paymentInstrumentRepositoryMock.findByInitiativeIdAndUserIdAndHpan(INITIATIVE_ID,
                USER_ID, HPAN))
        .thenReturn(List.of(TEST_INSTRUMENT, TEST_INACTIVE_INSTRUMENT));

    Mockito.when(paymentInstrumentRepositoryMock.countByHpanAndStatus(HPAN,
        PaymentInstrumentConstants.STATUS_ACTIVE)).thenReturn(0);

    Mockito.doThrow(new PaymentInstrumentException(400,"")).when(producer).sendInstruments(Mockito.any());

    try {
      paymentInstrumentService.deactivateInstrument(INITIATIVE_ID, USER_ID, HPAN, TEST_DATE);
      Assertions.fail();
    } catch (PaymentInstrumentException e) {
      assertEquals(HttpStatus.BAD_REQUEST.value(), e.getCode());
    }
  }

  @Test
  void deactivateInstrument_ok_to_rtd() {
    TEST_INSTRUMENT.setStatus(PaymentInstrumentConstants.STATUS_ACTIVE);
    TEST_INSTRUMENT.setRequestDeactivationDate(null);
    Mockito.when(
            paymentInstrumentRepositoryMock.findByInitiativeIdAndUserIdAndHpan(INITIATIVE_ID,
                USER_ID, HPAN))
        .thenReturn(List.of(TEST_INSTRUMENT, TEST_INACTIVE_INSTRUMENT));

    Mockito.when(paymentInstrumentRepositoryMock.countByHpanAndStatus(HPAN,
        PaymentInstrumentConstants.STATUS_ACTIVE)).thenReturn(0);

    Mockito.doAnswer(invocationOnMock -> {
      TEST_INSTRUMENT.setStatus(PaymentInstrumentConstants.STATUS_INACTIVE);
      TEST_INSTRUMENT.setRequestDeactivationDate(TEST_DATE);
      return null;
    }).when(paymentInstrumentRepositoryMock).save(Mockito.any(PaymentInstrument.class));

    try {
      paymentInstrumentService.deactivateInstrument(INITIATIVE_ID, USER_ID, HPAN, TEST_DATE);
    } catch (PaymentInstrumentException e) {
      Assertions.fail();
    }
    assertEquals(PaymentInstrumentConstants.STATUS_INACTIVE, TEST_INSTRUMENT.getStatus());
    assertNotNull(TEST_INSTRUMENT.getRequestDeactivationDate());
    assertEquals(TEST_DATE, TEST_INSTRUMENT.getRequestDeactivationDate());
  }

  @Test
  void deactivateInstrument_ok_no_rtd() {
    TEST_INSTRUMENT.setStatus(PaymentInstrumentConstants.STATUS_ACTIVE);
    TEST_INSTRUMENT.setRequestDeactivationDate(null);
    Mockito.when(
            paymentInstrumentRepositoryMock.findByInitiativeIdAndUserIdAndHpan(INITIATIVE_ID,
                USER_ID, HPAN))
        .thenReturn(List.of(TEST_INSTRUMENT, TEST_INACTIVE_INSTRUMENT));

    Mockito.when(paymentInstrumentRepositoryMock.countByHpanAndStatus(HPAN,
        PaymentInstrumentConstants.STATUS_ACTIVE)).thenReturn(1);

    Mockito.doAnswer(invocationOnMock -> {
      TEST_INSTRUMENT.setStatus(PaymentInstrumentConstants.STATUS_INACTIVE);
      TEST_INSTRUMENT.setRequestDeactivationDate(TEST_DATE);
      return null;
    }).when(paymentInstrumentRepositoryMock).save(Mockito.any(PaymentInstrument.class));

    try {
      paymentInstrumentService.deactivateInstrument(INITIATIVE_ID, USER_ID, HPAN, TEST_DATE);
    } catch (PaymentInstrumentException e) {
      Assertions.fail();
    }
    assertEquals(PaymentInstrumentConstants.STATUS_INACTIVE, TEST_INSTRUMENT.getStatus());
    assertNotNull(TEST_INSTRUMENT.getRequestDeactivationDate());
    assertEquals(TEST_DATE, TEST_INSTRUMENT.getRequestDeactivationDate());
  }

  @Test
  void deactivateInstrument_ok_idemp() {
    Mockito.when(
            paymentInstrumentRepositoryMock.findByInitiativeIdAndUserIdAndHpan(INITIATIVE_ID,
                USER_ID, HPAN))
        .thenReturn(List.of(TEST_INACTIVE_INSTRUMENT));

    try {
      paymentInstrumentService.deactivateInstrument(INITIATIVE_ID, USER_ID, HPAN, TEST_DATE);
    } catch (PaymentInstrumentException e) {
      Assertions.fail();
    }
  }

  @Test
  void enrollInstrument_ko_rule_engine() {
    Mockito.when(paymentInstrumentRepositoryMock.findByHpanAndStatus(HPAN,
        PaymentInstrumentConstants.STATUS_ACTIVE)).thenReturn(new ArrayList<>());

    Mockito.when(paymentInstrumentRepositoryMock.countByHpanAndStatus(HPAN,
        PaymentInstrumentConstants.STATUS_ACTIVE)).thenReturn(0);

    Mockito.doThrow(new PaymentInstrumentException(400,"")).when(producer).sendInstruments(Mockito.any());

    try {
      paymentInstrumentService.enrollInstrument(INITIATIVE_ID, USER_ID, HPAN, CHANNEL, TEST_DATE);
      Assertions.fail();
    } catch (PaymentInstrumentException e) {
      assertEquals(HttpStatus.BAD_REQUEST.value(), e.getCode());
    }
  }

  @Test
  void enrollInstrument_ok_queue_error() {
    Mockito.when(paymentInstrumentRepositoryMock.findByHpanAndStatus(HPAN,
        PaymentInstrumentConstants.STATUS_ACTIVE)).thenReturn(new ArrayList<>());

    Mockito.when(paymentInstrumentRepositoryMock.countByHpanAndStatus(HPAN,
        PaymentInstrumentConstants.STATUS_ACTIVE)).thenReturn(0);

    Mockito.doThrow(new PaymentInstrumentException(400,"")).when(rtdProducer).sendInstrument(Mockito.any(
        RTDOperationDTO.class));

    try {
      paymentInstrumentService.enrollInstrument(INITIATIVE_ID, USER_ID, HPAN, CHANNEL, TEST_DATE);
    } catch (PaymentInstrumentException e) {
      Assertions.fail();
    }
  }

  @Test
  void deactivateInstrument_not_found() {
    Mockito.when(
            paymentInstrumentRepositoryMock.findByInitiativeIdAndUserIdAndHpan(INITIATIVE_ID,
                USER_ID, HPAN))
        .thenReturn(List.of());

    try {
      paymentInstrumentService.deactivateInstrument(INITIATIVE_ID, USER_ID, HPAN, TEST_DATE);
    } catch (PaymentInstrumentException e) {
      assertEquals(HttpStatus.NOT_FOUND.value(), e.getCode());
      assertEquals(PaymentInstrumentConstants.ERROR_PAYMENT_INSTRUMENT_NOT_FOUND, e.getMessage());
    }
  }

  @Test
  void countByInitiativeIdAndUserId_ok() {
    Mockito.when(
            paymentInstrumentRepositoryMock.countByInitiativeIdAndUserIdAndStatus(INITIATIVE_ID,
                USER_ID, PaymentInstrumentConstants.STATUS_ACTIVE))
        .thenReturn(TEST_COUNT);

    int actual = paymentInstrumentService.countByInitiativeIdAndUserIdAndStatus(INITIATIVE_ID,
        USER_ID, PaymentInstrumentConstants.STATUS_ACTIVE);

    assertEquals(TEST_COUNT, actual);
  }

  @Test
  void getHpan_ok() {
    List<PaymentInstrument> paymentInstruments = new ArrayList<>();
    paymentInstruments.add(TEST_INSTRUMENT);

    Mockito.when(
            paymentInstrumentRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
        .thenReturn(paymentInstruments);
    try {
      HpanGetDTO hpanGetDTO = paymentInstrumentService.gethpan(INITIATIVE_ID, USER_ID);
      HpanDTO actual = hpanGetDTO.getHpanList().get(0);
      assertEquals(TEST_INSTRUMENT.getHpan(), actual.getHpan());
      assertEquals(TEST_INSTRUMENT.getChannel(), actual.getChannel());
      assertFalse(hpanGetDTO.getHpanList().isEmpty());
    } catch (PaymentInstrumentException e) {
      Assertions.fail();
    }

  }

  @Test
  void getHpan_ko() {
    List<PaymentInstrument> paymentInstruments = new ArrayList<>();

    Mockito.when(
            paymentInstrumentRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
        .thenReturn(paymentInstruments);
    try {
      paymentInstrumentService.gethpan(INITIATIVE_ID, USER_ID);
      Assertions.fail();
    } catch (PaymentInstrumentException e) {
      assertEquals(HttpStatus.NOT_FOUND.value(), e.getCode());
      assertEquals(PaymentInstrumentConstants.ERROR_INITIATIVE_USER, e.getMessage());
    }

  }

  @Test
  void disableAllPayInstrument_ok(){

    List<PaymentInstrument> paymentInstruments = new ArrayList<>();
    paymentInstruments.add(TEST_INSTRUMENT);

    Mockito.when(paymentInstrumentRepositoryMock.findByInitiativeIdAndUserIdAndStatus(INITIATIVE_ID, USER_ID, PaymentInstrumentConstants.STATUS_ACTIVE))
        .thenReturn(paymentInstruments);

    Mockito.doAnswer(
            invocationOnMock -> {
              TEST_INSTRUMENT.setStatus(PaymentInstrumentConstants.STATUS_INACTIVE);
              TEST_INSTRUMENT.setRequestDeactivationDate(TEST_DATE);
              return null;
            })
        .when(paymentInstrumentRepositoryMock).save(Mockito.any(PaymentInstrument.class));

    paymentInstrumentService.deactivateAllInstrument(INITIATIVE_ID, USER_ID, LocalDateTime.now().toString());
    assertNotNull(TEST_INSTRUMENT.getRequestDeactivationDate());
    assertEquals(PaymentInstrumentConstants.STATUS_INACTIVE, TEST_INSTRUMENT.getStatus());
  }

  @Test
  void disableAllPayInstrument_ok_queue_error(){

    List<PaymentInstrument> paymentInstruments = new ArrayList<>();
    paymentInstruments.add(TEST_INSTRUMENT);

    Mockito.when(paymentInstrumentRepositoryMock.findByInitiativeIdAndUserIdAndStatus(INITIATIVE_ID, USER_ID, PaymentInstrumentConstants.STATUS_ACTIVE))
        .thenReturn(paymentInstruments);

    Mockito.doThrow(new PaymentInstrumentException(400,"")).when(rtdProducer).sendInstrument(Mockito.any(
        RTDOperationDTO.class));

    Mockito.doAnswer(
            invocationOnMock -> {
              TEST_INSTRUMENT.setStatus(PaymentInstrumentConstants.STATUS_INACTIVE);
              TEST_INSTRUMENT.setRequestDeactivationDate(TEST_DATE);
              return null;
            })
        .when(paymentInstrumentRepositoryMock).save(Mockito.any(PaymentInstrument.class));

    paymentInstrumentService.deactivateAllInstrument(INITIATIVE_ID, USER_ID, LocalDateTime.now().toString());
    assertNotNull(TEST_INSTRUMENT.getRequestDeactivationDate());
    assertEquals(PaymentInstrumentConstants.STATUS_INACTIVE, TEST_INSTRUMENT.getStatus());
  }

  @Test
  void disableAllPayInstrument_ko(){

    List<PaymentInstrument> paymentInstruments = new ArrayList<>();
    paymentInstruments.add(TEST_INSTRUMENT);



    Mockito.doThrow(new PaymentInstrumentException(400, "error")).when(producer).sendInstruments(
        ArgumentMatchers.<Message<RuleEngineQueueDTO>>any());

    Mockito.when(paymentInstrumentRepositoryMock.findByInitiativeIdAndUserIdAndStatus(INITIATIVE_ID, USER_ID, PaymentInstrumentConstants.STATUS_ACTIVE))
        .thenReturn(paymentInstruments);

    Mockito.doAnswer(
            invocationOnMock -> {
              TEST_INSTRUMENT.setStatus(PaymentInstrumentConstants.STATUS_INACTIVE);
              TEST_INSTRUMENT.setRequestDeactivationDate(TEST_DATE);
              return null;
            })
        .when(paymentInstrumentRepositoryMock).save(Mockito.any(PaymentInstrument.class));

    try{
      paymentInstrumentService.deactivateAllInstrument(INITIATIVE_ID, USER_ID, LocalDateTime.now().toString());
      fail();
    }catch (Exception e){
      assertNull(TEST_INSTRUMENT.getRequestDeactivationDate());
      assertNotEquals(PaymentInstrumentConstants.STATUS_INACTIVE, TEST_INSTRUMENT.getStatus());
    }

  }

  @Test
  void rollback(){
    List<PaymentInstrument> paymentInstrumentList = new ArrayList<>();
    paymentInstrumentList.add(TEST_INSTRUMENT);
    paymentInstrumentService.rollbackInstruments(paymentInstrumentList);
    assertNull(TEST_INSTRUMENT.getRequestDeactivationDate());
    assertNotEquals(PaymentInstrumentConstants.STATUS_INACTIVE, TEST_INSTRUMENT.getStatus());
  }

}
