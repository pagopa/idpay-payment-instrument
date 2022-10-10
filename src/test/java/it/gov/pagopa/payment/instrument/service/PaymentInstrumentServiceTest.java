package it.gov.pagopa.payment.instrument.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import it.gov.pagopa.payment.instrument.connector.PMRestClientConnector;
import it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants;
import it.gov.pagopa.payment.instrument.dto.HpanDTO;
import it.gov.pagopa.payment.instrument.dto.HpanGetDTO;
import it.gov.pagopa.payment.instrument.dto.RTDOperationDTO;
import it.gov.pagopa.payment.instrument.dto.mapper.MessageMapper;
import it.gov.pagopa.payment.instrument.dto.pm.BPayInfo;
import it.gov.pagopa.payment.instrument.dto.pm.BPayInfo.BPayPaymentInstrumentWallet;
import it.gov.pagopa.payment.instrument.dto.pm.CardInfo;
import it.gov.pagopa.payment.instrument.dto.pm.CardInfo.CardType;
import it.gov.pagopa.payment.instrument.dto.pm.PaymentMethodInfo;
import it.gov.pagopa.payment.instrument.dto.pm.SatispayInfo;
import it.gov.pagopa.payment.instrument.dto.pm.WalletType;
import it.gov.pagopa.payment.instrument.dto.pm.WalletV2;
import it.gov.pagopa.payment.instrument.dto.pm.WalletV2ListResponse;
import it.gov.pagopa.payment.instrument.event.ErrorProducer;
import it.gov.pagopa.payment.instrument.event.RTDProducer;
import it.gov.pagopa.payment.instrument.event.RuleEngineProducer;
import it.gov.pagopa.payment.instrument.exception.PaymentInstrumentException;
import it.gov.pagopa.payment.instrument.model.PaymentInstrument;
import it.gov.pagopa.payment.instrument.repository.PaymentInstrumentRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
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
  @MockBean
  PMRestClientConnector pmRestClientConnector;
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
  private static final String ID_WALLET = "ID_WALLET";
  private static final String INSTRUMENT_ID = "INSTRUMENT_ID";
  private static final String MASKED_PAN = "MASKED_PAN";
  private static final String BLURRED_NUMBER = "BLURRED_NUMBER";
  private static final String BRAND = "BRAND";
  private static final String EXPIRE_MONTH = "EXPIRE_MONTH";
  private static final String EXPIRE_YEAR = "EXPIRE_YEAR";
  private static final String HOLDER = "HOLDER";
  private static final List<String> HTOKEN_LIST = new ArrayList<>();
  private static final String ISSUER_ABI_CODE = "ISSUER_ABI_CODE";
  private static final String UUID = "UUID";
  private static final String BRAND_LOGO = "BAND_LOGO";
  private static final LocalDateTime CREATE_DATTE = LocalDateTime.now();
  private static final List<String> ENABLEABLE_FUNCTIONS = new ArrayList<>();
  private static final Boolean FAVOURITE = true;
  private static final String ONBOARDING_CHANNEL = "ONBOARDING_CHANNEL";
  private static final String BANK_NAME = "BANK_NAME";
  private static final String INSTITUTE_CODE = "INSTITUTE_CODE";
  private static final List<BPayPaymentInstrumentWallet> PAYMENT_INSTRUMENTS = null;
  private static final CardInfo CARD_INFO = new CardInfo(BLURRED_NUMBER, BRAND, BRAND_LOGO,
      EXPIRE_MONTH, EXPIRE_YEAR, HPAN, HOLDER, HTOKEN_LIST, ISSUER_ABI_CODE,
      CardType.PP);
  private static final SatispayInfo SATISPAY_INFO = new SatispayInfo(BRAND_LOGO, UUID);
  private static final BPayInfo BPAY_INFO = new BPayInfo(BANK_NAME, BRAND_LOGO, INSTITUTE_CODE,
      BLURRED_NUMBER,
      PAYMENT_INSTRUMENTS, UUID);
  private static final PaymentMethodInfo PAYMENT_METHOD_INFO = new PaymentMethodInfo(CARD_INFO,
      SATISPAY_INFO, BPAY_INFO);
  private static final LocalDateTime UPDATE_DATE = LocalDateTime.now();
  private static final WalletV2 WALLET_V2_CARD = new WalletV2(CREATE_DATTE, ENABLEABLE_FUNCTIONS,
      FAVOURITE, ID_WALLET, ONBOARDING_CHANNEL, UPDATE_DATE, WalletType.CARD, PAYMENT_METHOD_INFO);
  private static final WalletV2 WALLET_V2_SATISPAY = new WalletV2(CREATE_DATTE,
      ENABLEABLE_FUNCTIONS,
      FAVOURITE, ID_WALLET, ONBOARDING_CHANNEL, UPDATE_DATE, WalletType.SATISPAY,
      PAYMENT_METHOD_INFO);
  private static final WalletV2 WALLET_V2_BPAY = new WalletV2(CREATE_DATTE, ENABLEABLE_FUNCTIONS,
      FAVOURITE, ID_WALLET, ONBOARDING_CHANNEL, UPDATE_DATE, WalletType.BPAY, PAYMENT_METHOD_INFO);
  private static final WalletV2 WALLET_V2_TYPE_KO = new WalletV2(CREATE_DATTE, ENABLEABLE_FUNCTIONS,
      FAVOURITE, ID_WALLET, ONBOARDING_CHANNEL, UPDATE_DATE, null, PAYMENT_METHOD_INFO);

  private static final List<WalletV2> WALLET_V2_LIST_CARD = List.of(WALLET_V2_CARD);
  private static final List<WalletV2> WALLET_V2_LIST_SATISPAY = List.of(WALLET_V2_SATISPAY);
  private static final List<WalletV2> WALLET_V2_LIST_BPAY = List.of(WALLET_V2_BPAY);
  private static final List<WalletV2> WALLET_V2_LIST_TYPE_KO = List.of(WALLET_V2_TYPE_KO);
  private static final WalletV2ListResponse WALLET_V_2_LIST_RESPONSE_CARD = new WalletV2ListResponse(
      WALLET_V2_LIST_CARD);
  private static final WalletV2ListResponse WALLET_V_2_LIST_RESPONSE_SATISPAY = new WalletV2ListResponse(
      WALLET_V2_LIST_SATISPAY);
  private static final WalletV2ListResponse WALLET_V_2_LIST_RESPONSE_BPAY = new WalletV2ListResponse(
      WALLET_V2_LIST_BPAY);
  private static final WalletV2ListResponse WALLET_V_2_LIST_RESPONSE_TYPE_KO = new WalletV2ListResponse(
      WALLET_V2_LIST_TYPE_KO);


  private static final PaymentInstrument TEST_INSTRUMENT = new PaymentInstrument(INITIATIVE_ID,
      USER_ID, ID_WALLET, HPAN, MASKED_PAN, BRAND_LOGO, PaymentInstrumentConstants.STATUS_ACTIVE,
      CHANNEL, TEST_DATE);
  private static final PaymentInstrument TEST_INACTIVE_INSTRUMENT = new PaymentInstrument(
      INITIATIVE_ID,
      USER_ID, ID_WALLET, HPAN, MASKED_PAN, BRAND_LOGO, PaymentInstrumentConstants.STATUS_INACTIVE,
      CHANNEL, TEST_DATE);

  static {
    TEST_INACTIVE_INSTRUMENT.setRequestDeactivationDate(TEST_DATE);
  }

  @Test
  void enrollInstrument_ok_empty() {
    Mockito.when(paymentInstrumentRepositoryMock.findByIdWalletAndStatus(ID_WALLET,
        PaymentInstrumentConstants.STATUS_ACTIVE)).thenReturn(new ArrayList<>());

    Mockito.when(paymentInstrumentRepositoryMock.countByHpanAndStatus(HPAN,
        PaymentInstrumentConstants.STATUS_ACTIVE)).thenReturn(0);
    Mockito.when(
        pmRestClientConnector.getWalletList(USER_ID)).thenReturn(WALLET_V_2_LIST_RESPONSE_CARD);

    try {
      paymentInstrumentService.enrollInstrument(INITIATIVE_ID, USER_ID, ID_WALLET, CHANNEL,
          TEST_DATE);
    } catch (PaymentInstrumentException e) {
      fail();
    }
  }

  @Test
  void enrollInstrument_ok_idemp() {
    Mockito.when(paymentInstrumentRepositoryMock.findByIdWalletAndStatus(ID_WALLET,
        PaymentInstrumentConstants.STATUS_ACTIVE)).thenReturn(List.of(TEST_INSTRUMENT));

    Mockito.when(
        pmRestClientConnector.getWalletList(USER_ID)).thenReturn(WALLET_V_2_LIST_RESPONSE_CARD);

    try {
      paymentInstrumentService.enrollInstrument(INITIATIVE_ID, USER_ID, ID_WALLET, CHANNEL,
          TEST_DATE);
    } catch (PaymentInstrumentException e) {
      Assertions.fail();
    }
  }

  @Test
  void enrollInstrument_ok_satispay() {
    Mockito.when(paymentInstrumentRepositoryMock.findByIdWalletAndStatus(ID_WALLET,
        PaymentInstrumentConstants.STATUS_ACTIVE)).thenReturn(new ArrayList<>());

    Mockito.when(paymentInstrumentRepositoryMock.countByHpanAndStatus(HPAN,
        PaymentInstrumentConstants.STATUS_ACTIVE)).thenReturn(0);
    Mockito.when(
        pmRestClientConnector.getWalletList(USER_ID)).thenReturn(WALLET_V_2_LIST_RESPONSE_SATISPAY);

    try {
      paymentInstrumentService.enrollInstrument(INITIATIVE_ID, USER_ID, ID_WALLET, CHANNEL,
          TEST_DATE);
    } catch (PaymentInstrumentException e) {
      fail();
    }
  }

  @Test
  void enrollInstrument_ok_bpay() {
    Mockito.when(paymentInstrumentRepositoryMock.findByIdWalletAndStatus(ID_WALLET,
        PaymentInstrumentConstants.STATUS_ACTIVE)).thenReturn(new ArrayList<>());

    Mockito.when(paymentInstrumentRepositoryMock.countByHpanAndStatus(HPAN,
        PaymentInstrumentConstants.STATUS_ACTIVE)).thenReturn(0);
    Mockito.when(
        pmRestClientConnector.getWalletList(USER_ID)).thenReturn(WALLET_V_2_LIST_RESPONSE_BPAY);

    try {
      paymentInstrumentService.enrollInstrument(INITIATIVE_ID, USER_ID, ID_WALLET, CHANNEL,
          TEST_DATE);
    } catch (PaymentInstrumentException e) {
      fail();
    }
  }

  @Test
  void enrollInstrument_ok_other_initiative() {
    Mockito.when(paymentInstrumentRepositoryMock.findByIdWalletAndStatus(ID_WALLET,
        PaymentInstrumentConstants.STATUS_ACTIVE)).thenReturn(List.of(TEST_INSTRUMENT));

    Mockito.when(paymentInstrumentRepositoryMock.countByHpanAndStatus(HPAN,
        PaymentInstrumentConstants.STATUS_ACTIVE)).thenReturn(1);

    Mockito.when(
        pmRestClientConnector.getWalletList(USER_ID)).thenReturn(WALLET_V_2_LIST_RESPONSE_CARD);

    try {
      paymentInstrumentService.enrollInstrument(INITIATIVE_ID_OTHER, USER_ID, ID_WALLET, CHANNEL,
          TEST_DATE);
    } catch (PaymentInstrumentException e) {
      fail();
    }
    assertNotNull(TEST_INSTRUMENT.getActivationDate());
    assertEquals(ID_WALLET, TEST_INSTRUMENT.getIdWallet());
  }

  @Test
  void enrollInstrument_pm_ko() {
    Mockito.when(paymentInstrumentRepositoryMock.findByIdWalletAndStatus(ID_WALLET,
        PaymentInstrumentConstants.STATUS_ACTIVE)).thenReturn(new ArrayList<>());

    Mockito.when(paymentInstrumentRepositoryMock.countByHpanAndStatus(HPAN,
        PaymentInstrumentConstants.STATUS_ACTIVE)).thenReturn(0);

    Request request =
        Request.create(Request.HttpMethod.GET, "url", new HashMap<>(), null, new RequestTemplate());

    Mockito.doThrow(new FeignException.BadRequest("", request, new byte[0], null))
        .when(pmRestClientConnector).getWalletList(USER_ID);

    try {
      paymentInstrumentService.enrollInstrument(INITIATIVE_ID_OTHER, USER_ID, ID_WALLET, CHANNEL,
          TEST_DATE);
      Assertions.fail();
    } catch (PaymentInstrumentException e) {
      assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getCode());
    }
  }

  @Test
  void enrollInstrument_ok_already_active() {
    Mockito.when(paymentInstrumentRepositoryMock.findByIdWalletAndStatus(ID_WALLET,
        PaymentInstrumentConstants.STATUS_ACTIVE)).thenReturn(List.of(TEST_INSTRUMENT));

    Mockito.when(
        pmRestClientConnector.getWalletList(USER_ID)).thenReturn(WALLET_V_2_LIST_RESPONSE_CARD);

    try {
      paymentInstrumentService.enrollInstrument(INITIATIVE_ID, USER_ID_FAIL, ID_WALLET, CHANNEL,
          TEST_DATE);
    } catch (PaymentInstrumentException e) {
      assertEquals(HttpStatus.FORBIDDEN.value(), e.getCode());
      assertEquals(PaymentInstrumentConstants.ERROR_PAYMENT_INSTRUMENT_ALREADY_ACTIVE,
          e.getMessage());
    }
  }

  @Test
  void enrollInstrument_ko_rule_engine() {
    Mockito.when(paymentInstrumentRepositoryMock.findByIdWalletAndStatus(ID_WALLET,
        PaymentInstrumentConstants.STATUS_ACTIVE)).thenReturn(new ArrayList<>());

    Mockito.when(paymentInstrumentRepositoryMock.countByHpanAndStatus(HPAN,
        PaymentInstrumentConstants.STATUS_ACTIVE)).thenReturn(0);
    Mockito.when(
        pmRestClientConnector.getWalletList(USER_ID)).thenReturn(WALLET_V_2_LIST_RESPONSE_CARD);

    Mockito.doThrow(new PaymentInstrumentException(400, "")).when(producer)
        .sendInstruments(Mockito.any());

    try {
      paymentInstrumentService.enrollInstrument(INITIATIVE_ID, USER_ID, ID_WALLET, CHANNEL,
          TEST_DATE);
      Assertions.fail();
    } catch (PaymentInstrumentException e) {
      assertEquals(HttpStatus.BAD_REQUEST.value(), e.getCode());
    }
  }

  @Test
  void enrollInstrument_ok_queue_error() {
    Mockito.when(paymentInstrumentRepositoryMock.findByIdWalletAndStatus(ID_WALLET,
        PaymentInstrumentConstants.STATUS_ACTIVE)).thenReturn(new ArrayList<>());

    Mockito.when(paymentInstrumentRepositoryMock.countByHpanAndStatus(ID_WALLET,
        PaymentInstrumentConstants.STATUS_ACTIVE)).thenReturn(0);
    Mockito.when(
        pmRestClientConnector.getWalletList(USER_ID)).thenReturn(WALLET_V_2_LIST_RESPONSE_CARD);

    Mockito.doThrow(new PaymentInstrumentException(400, "")).when(rtdProducer)
        .sendInstrument(Mockito.any(
            RTDOperationDTO.class));

    try {
      paymentInstrumentService.enrollInstrument(INITIATIVE_ID, USER_ID, ID_WALLET, CHANNEL,
          TEST_DATE);
    } catch (PaymentInstrumentException e) {
      Assertions.fail();
    }
  }
  @Test
  void deactivateInstrument_ok_to_rtd() {
    TEST_INSTRUMENT.setStatus(PaymentInstrumentConstants.STATUS_ACTIVE);
    TEST_INSTRUMENT.setRequestDeactivationDate(null);
    Mockito.when(
            paymentInstrumentRepositoryMock.findByInitiativeIdAndUserIdAndId(INITIATIVE_ID,
                USER_ID, INSTRUMENT_ID))
        .thenReturn(List.of(TEST_INSTRUMENT, TEST_INACTIVE_INSTRUMENT));

    Mockito.when(paymentInstrumentRepositoryMock.countByHpanAndStatus(HPAN,
        PaymentInstrumentConstants.STATUS_ACTIVE)).thenReturn(0);

    Mockito.doAnswer(invocationOnMock -> {
      TEST_INSTRUMENT.setStatus(PaymentInstrumentConstants.STATUS_INACTIVE);
      TEST_INSTRUMENT.setRequestDeactivationDate(TEST_DATE);
      return null;
    }).when(paymentInstrumentRepositoryMock).save(Mockito.any(PaymentInstrument.class));

    try {
      paymentInstrumentService.deactivateInstrument(INITIATIVE_ID, USER_ID, INSTRUMENT_ID,
          TEST_DATE);
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
            paymentInstrumentRepositoryMock.findByInitiativeIdAndUserIdAndId(INITIATIVE_ID,
                USER_ID, INSTRUMENT_ID))
        .thenReturn(List.of(TEST_INSTRUMENT, TEST_INACTIVE_INSTRUMENT));

    Mockito.when(paymentInstrumentRepositoryMock.countByHpanAndStatus(HPAN,
        PaymentInstrumentConstants.STATUS_ACTIVE)).thenReturn(1);

    Mockito.doAnswer(invocationOnMock -> {
      TEST_INSTRUMENT.setStatus(PaymentInstrumentConstants.STATUS_INACTIVE);
      TEST_INSTRUMENT.setRequestDeactivationDate(TEST_DATE);
      return null;
    }).when(paymentInstrumentRepositoryMock).save(Mockito.any(PaymentInstrument.class));

    try {
      paymentInstrumentService.deactivateInstrument(INITIATIVE_ID, USER_ID, INSTRUMENT_ID,
          TEST_DATE);
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
            paymentInstrumentRepositoryMock.findByInitiativeIdAndUserIdAndId(INITIATIVE_ID,
                USER_ID, INSTRUMENT_ID))
        .thenReturn(List.of(TEST_INACTIVE_INSTRUMENT));

    try {
      paymentInstrumentService.deactivateInstrument(INITIATIVE_ID, USER_ID, INSTRUMENT_ID,
          TEST_DATE);
    } catch (PaymentInstrumentException e) {
      Assertions.fail();
    }
  }

  @Test
  void deactivateInstrument_not_found() {
    Mockito.when(
            paymentInstrumentRepositoryMock.findByInitiativeIdAndUserIdAndId(INITIATIVE_ID,
                USER_ID, INSTRUMENT_ID))
        .thenReturn(List.of());

    try {
      paymentInstrumentService.deactivateInstrument(INITIATIVE_ID, USER_ID, INSTRUMENT_ID,
          TEST_DATE);
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
  void disableAllPayInstrument_ok() {

    List<PaymentInstrument> paymentInstruments = new ArrayList<>();
    paymentInstruments.add(TEST_INSTRUMENT);

    Mockito.when(
            paymentInstrumentRepositoryMock.findByInitiativeIdAndUserIdAndStatus(INITIATIVE_ID, USER_ID,
                PaymentInstrumentConstants.STATUS_ACTIVE))
        .thenReturn(paymentInstruments);

    Mockito.doAnswer(
            invocationOnMock -> {
              TEST_INSTRUMENT.setStatus(PaymentInstrumentConstants.STATUS_INACTIVE);
              TEST_INSTRUMENT.setRequestDeactivationDate(TEST_DATE);
              return null;
            })
        .when(paymentInstrumentRepositoryMock).save(Mockito.any(PaymentInstrument.class));

    paymentInstrumentService.deactivateAllInstrument(INITIATIVE_ID, USER_ID,
        LocalDateTime.now().toString());
    assertNotNull(TEST_INSTRUMENT.getRequestDeactivationDate());
    assertEquals(PaymentInstrumentConstants.STATUS_INACTIVE, TEST_INSTRUMENT.getStatus());
  }

  @Test
  void disableAllPayInstrument_ko() {

    List<PaymentInstrument> paymentInstruments = new ArrayList<>();
    paymentInstruments.add(TEST_INSTRUMENT);

    Mockito.doThrow(new PaymentInstrumentException(400, "error")).when(producer).sendInstruments(
        ArgumentMatchers.any());

    Mockito.when(
            paymentInstrumentRepositoryMock.findByInitiativeIdAndUserIdAndStatus(INITIATIVE_ID, USER_ID,
                PaymentInstrumentConstants.STATUS_ACTIVE))
        .thenReturn(paymentInstruments);

    Mockito.doAnswer(
            invocationOnMock -> {
              TEST_INSTRUMENT.setStatus(PaymentInstrumentConstants.STATUS_INACTIVE);
              TEST_INSTRUMENT.setRequestDeactivationDate(TEST_DATE);
              return null;
            })
        .when(paymentInstrumentRepositoryMock).save(Mockito.any(PaymentInstrument.class));

    try {
      paymentInstrumentService.deactivateAllInstrument(INITIATIVE_ID, USER_ID,
          LocalDateTime.now().toString());
      fail();
    } catch (Exception e) {
      assertNull(TEST_INSTRUMENT.getRequestDeactivationDate());
      assertNotEquals(PaymentInstrumentConstants.STATUS_INACTIVE, TEST_INSTRUMENT.getStatus());
    }

  }

  @Test
  void rollback() {
    List<PaymentInstrument> paymentInstrumentList = new ArrayList<>();
    paymentInstrumentList.add(TEST_INSTRUMENT);
    paymentInstrumentService.rollbackInstruments(paymentInstrumentList);
    assertNull(TEST_INSTRUMENT.getRequestDeactivationDate());
    assertNotEquals(PaymentInstrumentConstants.STATUS_INACTIVE, TEST_INSTRUMENT.getStatus());
  }

}