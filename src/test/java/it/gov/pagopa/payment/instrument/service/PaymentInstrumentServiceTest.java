package it.gov.pagopa.payment.instrument.service;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import it.gov.pagopa.payment.instrument.connector.DecryptRestConnector;
import it.gov.pagopa.payment.instrument.connector.EncryptRestConnector;
import it.gov.pagopa.payment.instrument.connector.PMRestClientConnector;
import it.gov.pagopa.payment.instrument.connector.WalletRestConnector;
import it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants;
import it.gov.pagopa.payment.instrument.dto.*;
import it.gov.pagopa.payment.instrument.dto.mapper.AckMapper;
import it.gov.pagopa.payment.instrument.dto.mapper.MessageMapper;
import it.gov.pagopa.payment.instrument.dto.pm.PaymentMethodInfo;
import it.gov.pagopa.payment.instrument.dto.pm.PaymentMethodInfo.BPayPaymentInstrumentWallet;
import it.gov.pagopa.payment.instrument.dto.pm.PaymentMethodInfoList;
import it.gov.pagopa.payment.instrument.dto.pm.WalletV2;
import it.gov.pagopa.payment.instrument.dto.pm.WalletV2ListResponse;
import it.gov.pagopa.payment.instrument.dto.rtd.RTDEnrollAckDTO;
import it.gov.pagopa.payment.instrument.dto.rtd.RTDMessage;
import it.gov.pagopa.payment.instrument.dto.rtd.RTDOperationDTO;
import it.gov.pagopa.payment.instrument.dto.rtd.RTDRevokeCardDTO;
import it.gov.pagopa.payment.instrument.event.producer.ErrorProducer;
import it.gov.pagopa.payment.instrument.event.producer.RTDProducer;
import it.gov.pagopa.payment.instrument.event.producer.RuleEngineProducer;
import it.gov.pagopa.payment.instrument.exception.PaymentInstrumentException;
import it.gov.pagopa.payment.instrument.model.PaymentInstrument;
import it.gov.pagopa.payment.instrument.repository.PaymentInstrumentRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@ContextConfiguration(classes = PaymentInstrumentServiceImpl.class)
class PaymentInstrumentServiceTest {
    
    @MockBean
    PaymentInstrumentRepository paymentInstrumentRepositoryMock;
    @MockBean
    RuleEngineProducer producer;
    @MockBean
    RTDProducer rtdProducer;
    @MockBean
    ErrorProducer errorProducer;
    @MockBean
    PMRestClientConnector pmRestClientConnector;
    @MockBean
    EncryptRestConnector encryptRestConnector;
    @MockBean
    WalletRestConnector walletRestConnector;
    @MockBean
    DecryptRestConnector decryptRestConnector;
    @Autowired
    PaymentInstrumentService paymentInstrumentService;
    @MockBean
    MessageMapper messageMapper;
    @MockBean
    AckMapper ackMapper;
    private static final String USER_ID = "TEST_USER_ID";
    private static final String USER_ID_FAIL = "TEST_USER_ID_FAIL";
    private static final String INITIATIVE_ID = "TEST_INITIATIVE_ID";
    private static final String INITIATIVE_ID_OTHER = "TEST_INITIATIVE_ID_OTHER";
    private static final String HPAN = "TEST_HPAN";
    private static final Boolean CONSENT = true;
    private static final String CHANNEL = "TEST_CHANNEL";
    private static final String DELETE_CHANNEL = "TEST_DELETE_CHANNEL";
    private static final LocalDateTime TEST_ACTIVATION_DATE = LocalDateTime.now();
    private static final LocalDateTime TEST_DEACTIVATION_DATE = LocalDateTime.now();
    private static final LocalDateTime TEST_RULE_ENGINE_ACKDATE = LocalDateTime.now();
    private static final LocalDateTime TEST_DATE = LocalDateTime.now();
    private static final LocalDateTime TEST_TIMESTAMP = TEST_DATE.minusHours(4);
    private static final String ID_WALLET = "ID_WALLET";
    private static final String ID_WALLET_KO = "ID_WALLET_KO";
    private static final String INSTRUMENT_ID = "INSTRUMENT_ID";
    private static final String MASKED_PAN = "MASKED_PAN";
    private static final String BLURRED_NUMBER = "BLURRED_NUMBER";
    private static final String BRAND = "BRAND";
    private static final String EXPIRE_MONTH = "EXPIRE_MONTH";
    private static final String EXPIRE_YEAR = "EXPIRE_YEAR";
    private static final String HOLDER = "HOLDER";
    private static final String ISSUER_ABI_CODE = "ISSUER_ABI_CODE";
    private static final String UUID = "UUID";
    private static final String BRAND_LOGO = "BAND_LOGO";
    private static final String BPD = "BPD";
    private static final String CREATE_DATE = "LocalDateTime.now()";
    private static final List<String> ENABLEABLE_FUNCTIONS = List.of(BPD);
    private static final List<String> ENABLEABLE_FUNCTIONS_KO = List.of("TEST_KO");
    private static final Boolean FAVOURITE = true;
    private static final String ONBOARDING_CHANNEL = "ONBOARDING_CHANNEL";
    private static final String BANK_NAME = "BANK_NAME";
    private static final String INSTITUTE_CODE = "INSTITUTE_CODE";
    private static final List<BPayPaymentInstrumentWallet> PAYMENT_INSTRUMENTS = null;
    private static final PaymentMethodInfo PAYMENT_METHOD_INFO = new PaymentMethodInfo(BLURRED_NUMBER,
            BRAND, BRAND_LOGO, EXPIRE_MONTH, EXPIRE_YEAR, HPAN, HOLDER, ISSUER_ABI_CODE,
            BANK_NAME, INSTITUTE_CODE, BLURRED_NUMBER, PAYMENT_INSTRUMENTS,
            UUID, UUID);
    private static final String UPDATE_DATE = "LocalDateTime.now()";
    private static final WalletV2 WALLET_V2_CARD = new WalletV2(CREATE_DATE, ENABLEABLE_FUNCTIONS,
            FAVOURITE, ID_WALLET, ONBOARDING_CHANNEL, UPDATE_DATE, "CARD", PAYMENT_METHOD_INFO);
    private static final WalletV2 WALLET_V2_PBD_KO = new WalletV2(CREATE_DATE,
            ENABLEABLE_FUNCTIONS_KO,
            FAVOURITE, ID_WALLET, ONBOARDING_CHANNEL, UPDATE_DATE, "CARD", PAYMENT_METHOD_INFO);
    private static final WalletV2 WALLET_V2_SATISPAY = new WalletV2(CREATE_DATE,
            
            ENABLEABLE_FUNCTIONS,
            FAVOURITE, ID_WALLET, ONBOARDING_CHANNEL, UPDATE_DATE, PaymentInstrumentConstants.SATISPAY,
            PAYMENT_METHOD_INFO);
    private static final WalletV2 WALLET_V2_BPAY = new WalletV2(CREATE_DATE, ENABLEABLE_FUNCTIONS,
            FAVOURITE, ID_WALLET, ONBOARDING_CHANNEL, UPDATE_DATE, PaymentInstrumentConstants.BPAY,
            PAYMENT_METHOD_INFO);
    private static final RTDMessage RTD_MESSAGE = new RTDMessage(USER_ID, HPAN, HPAN, HPAN, "ID_PAY",
            OffsetDateTime.now(), null);
    private static final RTDMessage RTD_MESSAGE_NOT_IDPAY = new RTDMessage(USER_ID, HPAN, HPAN, HPAN,
            "TEST",
            OffsetDateTime.now(), null);
    private static final List<WalletV2> WALLET_V2_LIST_CARD = List.of(WALLET_V2_CARD);
    private static final List<WalletV2> WALLET_V2_LIST_PBD_KO = List.of(WALLET_V2_PBD_KO);
    private static final List<WalletV2> WALLET_V2_LIST_SATISPAY = List.of(WALLET_V2_SATISPAY);
    private static final List<WalletV2> WALLET_V2_LIST_BPAY = List.of(WALLET_V2_BPAY);
    private static final WalletV2ListResponse WALLET_V_2_LIST_RESPONSE_CARD = new WalletV2ListResponse(
            WALLET_V2_LIST_CARD);
    private static final WalletV2ListResponse WALLET_V_2_LIST_RESPONSE_PBD_KO = new WalletV2ListResponse(
            WALLET_V2_LIST_PBD_KO);
    private static final WalletV2ListResponse WALLET_V_2_LIST_RESPONSE_SATISPAY = new WalletV2ListResponse(
            WALLET_V2_LIST_SATISPAY);
    private static final WalletV2ListResponse WALLET_V_2_LIST_RESPONSE_BPAY = new WalletV2ListResponse(
            WALLET_V2_LIST_BPAY);
    private static final DecryptCfDTO DECRYPT_CF_DTO = new DecryptCfDTO(USER_ID);
    private static final PaymentInstrument TEST_INSTRUMENT = PaymentInstrument.builder()
            .id(INSTRUMENT_ID)
            .initiativeId(INITIATIVE_ID)
            .userId(USER_ID)
            .idWallet(ID_WALLET)
            .hpan(HPAN)
            .maskedPan(MASKED_PAN)
            .brandLogo(BRAND_LOGO)
            .consent(CONSENT)
            .status(PaymentInstrumentConstants.STATUS_ACTIVE)
            .channel(CHANNEL)
            .deleteChannel(DELETE_CHANNEL)
            .activationDate(TEST_ACTIVATION_DATE)
            .deactivationDate(TEST_DEACTIVATION_DATE)
            .rtdAckDate(TEST_RULE_ENGINE_ACKDATE)
            .build();
    
    private static final PaymentInstrument TEST_PENDING_ENROLLMENT_INSTRUMENT = PaymentInstrument.builder()
            .id(INSTRUMENT_ID)
            .initiativeId(INITIATIVE_ID)
            .userId(USER_ID)
            .idWallet(ID_WALLET)
            .hpan(HPAN)
            .maskedPan(MASKED_PAN)
            .brandLogo(BRAND_LOGO)
            .consent(CONSENT)
            .status(PaymentInstrumentConstants.STATUS_PENDING_RE)
            .channel(CHANNEL)
            .deleteChannel(DELETE_CHANNEL)
            .activationDate(TEST_ACTIVATION_DATE)
            .deactivationDate(TEST_DEACTIVATION_DATE)
            .rtdAckDate(TEST_RULE_ENGINE_ACKDATE)
            .build();
    
    private static final PaymentInstrument TEST_PENDING_DEACTIVATION_INSTRUMENT = PaymentInstrument.builder()
            .id(INSTRUMENT_ID)
            .initiativeId(INITIATIVE_ID)
            .userId(USER_ID)
            .idWallet(ID_WALLET)
            .hpan(HPAN)
            .maskedPan(MASKED_PAN)
            .brandLogo(BRAND_LOGO)
            .consent(CONSENT)
            .status(PaymentInstrumentConstants.STATUS_PENDING_DEACTIVATION_REQUEST)
            .channel(CHANNEL)
            .deleteChannel(DELETE_CHANNEL)
            .activationDate(TEST_ACTIVATION_DATE)
            .deactivationDate(TEST_DEACTIVATION_DATE)
            .rtdAckDate(TEST_RULE_ENGINE_ACKDATE)
            .build();
    
    private static final PaymentInstrument TEST_INACTIVE_INSTRUMENT = PaymentInstrument.builder()
            .id(INSTRUMENT_ID)
            .initiativeId(INITIATIVE_ID)
            .userId(USER_ID)
            .idWallet(ID_WALLET)
            .hpan(HPAN)
            .maskedPan(MASKED_PAN)
            .brandLogo(BRAND_LOGO)
            .consent(CONSENT)
            .status(PaymentInstrumentConstants.STATUS_INACTIVE)
            .channel(CHANNEL)
            .deleteChannel(DELETE_CHANNEL)
            .activationDate(TEST_ACTIVATION_DATE)
            .deactivationDate(TEST_DEACTIVATION_DATE)
            .rtdAckDate(TEST_RULE_ENGINE_ACKDATE)
            .build();
    
    private static final PaymentInstrument TEST_INSTRUMENT_PENDING_RTD = PaymentInstrument.builder()
            .id(INSTRUMENT_ID)
            .initiativeId(INITIATIVE_ID)
            .userId(USER_ID)
            .idWallet(ID_WALLET)
            .hpan(HPAN)
            .maskedPan(MASKED_PAN)
            .brandLogo(BRAND_LOGO)
            .consent(CONSENT)
            .status(PaymentInstrumentConstants.STATUS_PENDING_RTD)
            .channel(CHANNEL)
            .deleteChannel(DELETE_CHANNEL)
            .activationDate(TEST_ACTIVATION_DATE)
            .deactivationDate(TEST_DEACTIVATION_DATE)
            .rtdAckDate(TEST_RULE_ENGINE_ACKDATE)
            .build();
    
    private static final PaymentInstrument TEST_ENROLLMENT_FAILED = PaymentInstrument.builder()
            .id(INSTRUMENT_ID)
            .initiativeId(INITIATIVE_ID)
            .userId(USER_ID)
            .idWallet(ID_WALLET)
            .hpan(HPAN)
            .maskedPan(MASKED_PAN)
            .brandLogo(BRAND_LOGO)
            .consent(CONSENT)
            .status(PaymentInstrumentConstants.STATUS_ENROLLMENT_FAILED)
            .channel(CHANNEL)
            .deleteChannel(DELETE_CHANNEL)
            .activationDate(TEST_ACTIVATION_DATE)
            .deactivationDate(TEST_DEACTIVATION_DATE)
            .rtdAckDate(TEST_RULE_ENGINE_ACKDATE)
            .updateDate(TEST_DATE)
            .build();

    private static final InstrumentAckDTO TEST_INSTRUMENT_ACK_DTO = InstrumentAckDTO.builder()
            .initiativeId(INITIATIVE_ID)
            .userId(USER_ID)
            .maskedPan(MASKED_PAN)
            .brandLogo(BRAND_LOGO)
            .channel(CHANNEL)
            .build();
    
    @Test
    void enrollInstrument_ok_empty() {
        Mockito.when(paymentInstrumentRepositoryMock.findByHpanAndStatusNotContaining(HPAN,
                PaymentInstrumentConstants.STATUS_INACTIVE)).thenReturn(new ArrayList<>());
        
        Mockito.when(decryptRestConnector.getPiiByToken(USER_ID)).thenReturn(DECRYPT_CF_DTO);
        Mockito.when(
                pmRestClientConnector.getWalletList(USER_ID)).thenReturn(WALLET_V_2_LIST_RESPONSE_CARD);
        
        try {
            paymentInstrumentService.enrollInstrument(INITIATIVE_ID, USER_ID, ID_WALLET, CHANNEL
            );
        } catch (PaymentInstrumentException e) {
            fail();
        }
    }
    
    @Test
    void enrollInstrument_ko_consent() {
        Mockito.when(paymentInstrumentRepositoryMock.findByHpanAndStatusNotContaining(HPAN,
                PaymentInstrumentConstants.STATUS_INACTIVE)).thenReturn(new ArrayList<>());
        
        Mockito.when(decryptRestConnector.getPiiByToken(USER_ID)).thenReturn(DECRYPT_CF_DTO);
        Mockito.when(
                pmRestClientConnector.getWalletList(USER_ID)).thenReturn(WALLET_V_2_LIST_RESPONSE_PBD_KO);
        
        try {
            paymentInstrumentService.enrollInstrument(INITIATIVE_ID, USER_ID, ID_WALLET, CHANNEL);
            fail();
        } catch (PaymentInstrumentException e) {
            assertEquals(HttpStatus.NOT_FOUND.value(), e.getCode());
        }
    }
    
    @Test
    void enrollInstrument_ok_idemp() {
        Mockito.when(paymentInstrumentRepositoryMock.findByHpanAndStatusNotContaining(HPAN,
                PaymentInstrumentConstants.STATUS_INACTIVE)).thenReturn(List.of(TEST_INSTRUMENT));
        Mockito.when(decryptRestConnector.getPiiByToken(USER_ID)).thenReturn(DECRYPT_CF_DTO);
        Mockito.when(
                pmRestClientConnector.getWalletList(USER_ID)).thenReturn(WALLET_V_2_LIST_RESPONSE_CARD);
        try {
            paymentInstrumentService.enrollInstrument(INITIATIVE_ID, USER_ID, ID_WALLET, CHANNEL
            );
        } catch (PaymentInstrumentException e) {
            Assertions.fail();
        }
    }
    
    @Test
    void enrollInstrument_ok_satispay() {
        Mockito.when(paymentInstrumentRepositoryMock.findByHpanAndStatusNotContaining(HPAN,
                PaymentInstrumentConstants.STATUS_INACTIVE)).thenReturn(new ArrayList<>());
        
        Mockito.when(decryptRestConnector.getPiiByToken(USER_ID)).thenReturn(DECRYPT_CF_DTO);
        Mockito.when(
                pmRestClientConnector.getWalletList(USER_ID)).thenReturn(WALLET_V_2_LIST_RESPONSE_SATISPAY);
        
        try {
            paymentInstrumentService.enrollInstrument(INITIATIVE_ID, USER_ID, ID_WALLET, CHANNEL
            );
        } catch (PaymentInstrumentException e) {
            fail();
        }
    }
    
    @Test
    void enrollInstrument_ok_bpay() {
        Mockito.when(paymentInstrumentRepositoryMock.findByHpanAndStatusNotContaining(HPAN,
                PaymentInstrumentConstants.STATUS_INACTIVE)).thenReturn(new ArrayList<>());
        
        Mockito.when(decryptRestConnector.getPiiByToken(USER_ID)).thenReturn(DECRYPT_CF_DTO);
        Mockito.when(
                pmRestClientConnector.getWalletList(USER_ID)).thenReturn(WALLET_V_2_LIST_RESPONSE_BPAY);
        
        try {
            paymentInstrumentService.enrollInstrument(INITIATIVE_ID, USER_ID, ID_WALLET, CHANNEL
            );
        } catch (PaymentInstrumentException e) {
            fail();
        }
    }
    
    @Test
    void enrollInstrument_ok_other_initiative() {
        Mockito.when(paymentInstrumentRepositoryMock.findByHpanAndStatusNotContaining(HPAN,
                PaymentInstrumentConstants.STATUS_INACTIVE)).thenReturn(List.of(TEST_INSTRUMENT));
        
        Mockito.when(decryptRestConnector.getPiiByToken(USER_ID)).thenReturn(DECRYPT_CF_DTO);
        Mockito.when(
                pmRestClientConnector.getWalletList(USER_ID)).thenReturn(WALLET_V_2_LIST_RESPONSE_CARD);
        
        try {
            paymentInstrumentService.enrollInstrument(INITIATIVE_ID_OTHER, USER_ID, ID_WALLET, CHANNEL
            );
        } catch (PaymentInstrumentException e) {
            fail();
        }
        assertEquals(ID_WALLET, TEST_INSTRUMENT.getIdWallet());
    }
    
    @Test
    void enrollInstrument_pm_ko() {
        Mockito.when(paymentInstrumentRepositoryMock.findByHpanAndStatusNotContaining(HPAN,
                PaymentInstrumentConstants.STATUS_INACTIVE)).thenReturn(new ArrayList<>());
        
        Mockito.when(paymentInstrumentRepositoryMock.countByHpanAndStatusIn(HPAN,
                List.of(PaymentInstrumentConstants.STATUS_ACTIVE,
                        PaymentInstrumentConstants.STATUS_PENDING_DEACTIVATION_REQUEST))).thenReturn(0);
        Mockito.when(decryptRestConnector.getPiiByToken(USER_ID)).thenReturn(DECRYPT_CF_DTO);
        Mockito.when(
                pmRestClientConnector.getWalletList(USER_ID)).thenReturn(WALLET_V_2_LIST_RESPONSE_CARD);
        
        try {
            paymentInstrumentService.enrollInstrument(INITIATIVE_ID_OTHER, USER_ID, ID_WALLET_KO, CHANNEL
            );
            Assertions.fail();
        } catch (PaymentInstrumentException e) {
            assertEquals(HttpStatus.NOT_FOUND.value(), e.getCode());
        }
    }
    
    @Test
    void enrollInstrumentFailed_ok() {
        Mockito.when(paymentInstrumentRepositoryMock.findByHpanAndStatusNotContaining(HPAN,
                PaymentInstrumentConstants.STATUS_INACTIVE)).thenReturn(List.of(TEST_ENROLLMENT_FAILED));
        when(paymentInstrumentRepositoryMock.save(any())).thenReturn(mock(PaymentInstrument.class));
        doNothing().when(rtdProducer).sendInstrument(any());
        Mockito.when(decryptRestConnector.getPiiByToken(USER_ID)).thenReturn(DECRYPT_CF_DTO);
        Mockito.when(
                pmRestClientConnector.getWalletList(USER_ID)).thenReturn(WALLET_V_2_LIST_RESPONSE_CARD);
        paymentInstrumentService.enrollInstrument(INITIATIVE_ID, USER_ID, ID_WALLET, CHANNEL);
        verify(rtdProducer).sendInstrument(any());
    }
    
    @Test
    void enrollInstrumentFailed_ko() {
        Mockito.when(paymentInstrumentRepositoryMock.findByHpanAndStatusNotContaining(HPAN,
                PaymentInstrumentConstants.STATUS_INACTIVE)).thenReturn(List.of(TEST_ENROLLMENT_FAILED));
        doThrow(new PaymentInstrumentException(HttpStatus.BAD_REQUEST.value(), ""))
                .when(rtdProducer).sendInstrument(any());
        Mockito.when(decryptRestConnector.getPiiByToken(USER_ID)).thenReturn(DECRYPT_CF_DTO);
        Mockito.when(
                pmRestClientConnector.getWalletList(USER_ID)).thenReturn(WALLET_V_2_LIST_RESPONSE_CARD);
        doNothing().when(paymentInstrumentRepositoryMock).delete(any());
        try {
            paymentInstrumentService.enrollInstrument(INITIATIVE_ID, USER_ID, ID_WALLET, CHANNEL);
        } catch (PaymentInstrumentException e) {
            assertEquals(HttpStatus.BAD_REQUEST.value(), e.getCode());
        }
    }
    
    @Test
    void idWallet_ko() {
        Mockito.when(paymentInstrumentRepositoryMock.findByHpanAndStatusNotContaining(HPAN,
                PaymentInstrumentConstants.STATUS_INACTIVE)).thenReturn(new ArrayList<>());
        
        Mockito.when(decryptRestConnector.getPiiByToken(USER_ID)).thenReturn(DECRYPT_CF_DTO);
        
        Request request =
                Request.create(Request.HttpMethod.GET, "url", new HashMap<>(), null, new RequestTemplate());
        
        Mockito.doThrow(new FeignException.BadRequest("", request, new byte[0], null))
                .when(pmRestClientConnector).getWalletList(USER_ID);
        
        try {
            paymentInstrumentService.enrollInstrument(INITIATIVE_ID_OTHER, USER_ID, ID_WALLET, CHANNEL
            );
            Assertions.fail();
        } catch (PaymentInstrumentException e) {
            assertEquals(HttpStatus.BAD_REQUEST.value(), e.getCode());
        }
    }
    
    @Test
    void enrollInstrument_ko_already_active() {
        Mockito.when(paymentInstrumentRepositoryMock.findByHpanAndStatusNotContaining(HPAN,
                PaymentInstrumentConstants.STATUS_INACTIVE)).thenReturn(List.of(TEST_INSTRUMENT));
        
        Mockito.when(decryptRestConnector.getPiiByToken(USER_ID_FAIL)).thenReturn(DECRYPT_CF_DTO);
        
        Mockito.when(
                pmRestClientConnector.getWalletList(USER_ID)).thenReturn(
                WALLET_V_2_LIST_RESPONSE_CARD);
        
        try {
            paymentInstrumentService.enrollInstrument(INITIATIVE_ID, USER_ID_FAIL, ID_WALLET, CHANNEL
            );
        } catch (PaymentInstrumentException e) {
            assertEquals(HttpStatus.FORBIDDEN.value(), e.getCode());
            assertEquals(PaymentInstrumentConstants.ERROR_PAYMENT_INSTRUMENT_ALREADY_ACTIVE,
                    e.getMessage());
        }
    }
    
    @Test
    void deactiveInstrument_ko_rule_engine() {
        Mockito.when(
                        paymentInstrumentRepositoryMock.findByInitiativeIdAndUserIdAndId(INITIATIVE_ID,
                                USER_ID, INSTRUMENT_ID))
                .thenReturn(Optional.of(TEST_INSTRUMENT));
        
        Mockito.when(paymentInstrumentRepositoryMock.countByHpanAndStatusIn(HPAN,
                List.of(PaymentInstrumentConstants.STATUS_ACTIVE,
                        PaymentInstrumentConstants.STATUS_PENDING_DEACTIVATION_REQUEST))).thenReturn(0);
        
        Mockito.doThrow(new PaymentInstrumentException(400, "")).when(producer)
                .sendInstruments(Mockito.any());
        
        try {
            paymentInstrumentService.deactivateInstrument(INITIATIVE_ID, USER_ID, INSTRUMENT_ID
            );
            Assertions.fail();
        } catch (PaymentInstrumentException e) {
            assertEquals(HttpStatus.BAD_REQUEST.value(), e.getCode());
        }
    }
    
    @Test
    void enrollInstrument_ko_RTD() {
        Mockito.when(paymentInstrumentRepositoryMock.findByIdWalletAndStatus(ID_WALLET,
                PaymentInstrumentConstants.STATUS_ACTIVE)).thenReturn(new ArrayList<>());
        
        Mockito.when(paymentInstrumentRepositoryMock.countByHpanAndStatusIn(HPAN,
                List.of(PaymentInstrumentConstants.STATUS_ACTIVE,
                        PaymentInstrumentConstants.STATUS_PENDING_DEACTIVATION_REQUEST))).thenReturn(0);
        Mockito.when(decryptRestConnector.getPiiByToken(USER_ID)).thenReturn(DECRYPT_CF_DTO);
        
        Mockito.when(
                pmRestClientConnector.getWalletList(USER_ID)).thenReturn(WALLET_V_2_LIST_RESPONSE_CARD);
        
        Mockito.doThrow(new PaymentInstrumentException(400, "")).when(rtdProducer)
                .sendInstrument(Mockito.any());
        
        Mockito.doNothing().when(paymentInstrumentRepositoryMock).delete(Mockito.any());
        
        try {
            paymentInstrumentService.enrollInstrument(INITIATIVE_ID, USER_ID, ID_WALLET, CHANNEL
            );
            Assertions.fail();
        } catch (PaymentInstrumentException e) {
            assertEquals(HttpStatus.BAD_REQUEST.value(), e.getCode());
        }
    }
    
    @Test
    void processAck_enroll_ok() {
        final RuleEngineAckDTO dto = new RuleEngineAckDTO(INITIATIVE_ID, USER_ID,
                PaymentInstrumentConstants.OPERATION_ADD, List.of(HPAN), List.of(), LocalDateTime.now());
        
        Mockito.when(ackMapper.ackToWallet(Mockito.any(),Mockito.anyString(),Mockito.anyString(),Mockito.anyString(),Mockito.anyInt())).thenReturn(TEST_INSTRUMENT_ACK_DTO);
        Mockito.when(
                        paymentInstrumentRepositoryMock.findByInitiativeIdAndUserIdAndHpanAndStatus(INITIATIVE_ID,
                                USER_ID, HPAN, PaymentInstrumentConstants.STATUS_PENDING_RE))
                .thenReturn(Optional.of(TEST_PENDING_ENROLLMENT_INSTRUMENT));
        
        paymentInstrumentService.processAck(dto);
        
        assertEquals(dto.getTimestamp(), TEST_PENDING_ENROLLMENT_INSTRUMENT.getActivationDate());
        assertEquals(PaymentInstrumentConstants.STATUS_ACTIVE,
                TEST_PENDING_ENROLLMENT_INSTRUMENT.getStatus());
        Mockito.verify(paymentInstrumentRepositoryMock, Mockito.times(1))
                .save(Mockito.any(PaymentInstrument.class));
    }
    
    @Test
    void processAck_enroll_ok_duplicate() {
        
        final RuleEngineAckDTO dto = new RuleEngineAckDTO(INITIATIVE_ID, USER_ID,
                PaymentInstrumentConstants.OPERATION_ADD, List.of(HPAN), List.of(), LocalDateTime.now());
        
        Mockito.when(
                        paymentInstrumentRepositoryMock.findByInitiativeIdAndUserIdAndHpanAndStatus(INITIATIVE_ID,
                                USER_ID, HPAN, PaymentInstrumentConstants.STATUS_PENDING_ENROLLMENT_REQUEST))
                .thenReturn(Optional.empty());
        
        paymentInstrumentService.processAck(dto);
        
        Mockito.verify(paymentInstrumentRepositoryMock, Mockito.times(0))
                .save(Mockito.any(PaymentInstrument.class));
    }
    
    @Test
    void processAck_enroll_ko() {
        Mockito.when(ackMapper.ackToWallet(Mockito.any(),Mockito.anyString(),Mockito.anyString(),Mockito.anyString(),Mockito.anyInt())).thenReturn(TEST_INSTRUMENT_ACK_DTO);
        final RuleEngineAckDTO dto = new RuleEngineAckDTO(INITIATIVE_ID, USER_ID,
                PaymentInstrumentConstants.OPERATION_ADD, List.of(), List.of(HPAN), LocalDateTime.now());
        

        Mockito.when(
                        paymentInstrumentRepositoryMock.findByInitiativeIdAndUserIdAndHpanAndStatus(INITIATIVE_ID,
                                USER_ID, HPAN, PaymentInstrumentConstants.STATUS_PENDING_RE))
                .thenReturn(Optional.of(TEST_PENDING_ENROLLMENT_INSTRUMENT));
        
        paymentInstrumentService.processAck(dto);
        
        assertEquals(PaymentInstrumentConstants.STATUS_ENROLLMENT_FAILED_KO_RE,
                TEST_PENDING_ENROLLMENT_INSTRUMENT.getStatus());
        Mockito.verify(paymentInstrumentRepositoryMock, Mockito.times(1))
                .save(Mockito.any(PaymentInstrument.class));
    }
    
    @Test
    void save_hpan_ko_decrypt() {
        Mockito.when(paymentInstrumentRepositoryMock.findByIdWalletAndStatus(ID_WALLET,
                PaymentInstrumentConstants.STATUS_ACTIVE)).thenReturn(List.of(TEST_INSTRUMENT));
        
        Request request =
                Request.create(
                        Request.HttpMethod.GET, "url", new HashMap<>(), null, new RequestTemplate());
        Mockito.doThrow(new FeignException.BadRequest("", request, new byte[0], null))
                .when(decryptRestConnector).getPiiByToken(USER_ID);
        
        try {
            paymentInstrumentService.enrollInstrument(INITIATIVE_ID, USER_ID, ID_WALLET, CHANNEL
            );
        } catch (PaymentInstrumentException e) {
            assertEquals(HttpStatus.BAD_REQUEST.value(), e.getCode());
        }
    }
    
    @Test
    void deactivateInstrument_ok() {
        TEST_INSTRUMENT.setStatus(PaymentInstrumentConstants.STATUS_ACTIVE);
        TEST_INSTRUMENT.setDeactivationDate(null);
        Mockito.when(
                        paymentInstrumentRepositoryMock.findByInitiativeIdAndUserIdAndId(INITIATIVE_ID,
                                USER_ID, INSTRUMENT_ID))
                .thenReturn(Optional.of(TEST_INSTRUMENT));
        
        try {
            paymentInstrumentService.deactivateInstrument(INITIATIVE_ID, USER_ID, INSTRUMENT_ID
            );
        } catch (PaymentInstrumentException e) {
            Assertions.fail();
        }
        assertEquals(PaymentInstrumentConstants.STATUS_PENDING_DEACTIVATION_REQUEST,
                TEST_INSTRUMENT.getStatus());
    }
    
    @Test
    void deactivateInstrument_ok_idemp() {
        Mockito.when(
                        paymentInstrumentRepositoryMock.findByInitiativeIdAndUserIdAndId(INITIATIVE_ID,
                                USER_ID, INSTRUMENT_ID))
                .thenReturn(Optional.of(TEST_INACTIVE_INSTRUMENT));
        
        try {
            paymentInstrumentService.deactivateInstrument(INITIATIVE_ID, USER_ID, INSTRUMENT_ID
            );
        } catch (PaymentInstrumentException e) {
            Assertions.fail();
        }
    }
    
    @Test
    void deactivateInstrument_not_found() {
        Mockito.when(
                        paymentInstrumentRepositoryMock.findByInitiativeIdAndUserIdAndId(INITIATIVE_ID,
                                USER_ID, INSTRUMENT_ID))
                .thenReturn(Optional.empty());
        
        try {
            paymentInstrumentService.deactivateInstrument(INITIATIVE_ID, USER_ID, INSTRUMENT_ID
            );
        } catch (PaymentInstrumentException e) {
            assertEquals(HttpStatus.NOT_FOUND.value(), e.getCode());
            assertEquals(PaymentInstrumentConstants.ERROR_PAYMENT_INSTRUMENT_NOT_FOUND, e.getMessage());
        }
    }
    
    @Test
    void getHpan_status_active_ok() {
        final PaymentInstrument INSTRUMENT = PaymentInstrument.builder()
                .initiativeId(INITIATIVE_ID)
                .userId(USER_ID)
                .idWallet(ID_WALLET)
                .hpan(HPAN)
                .maskedPan(MASKED_PAN)
                .brandLogo(BRAND_LOGO)
                .status(PaymentInstrumentConstants.STATUS_ACTIVE)
                .channel(CHANNEL)
                .build();
        List<PaymentInstrument> paymentInstruments = List.of(INSTRUMENT);
        
        Mockito.when(
                        paymentInstrumentRepositoryMock.findByInitiativeIdAndUserIdAndStatusIn(
                                INITIATIVE_ID,
                                USER_ID, List.of(PaymentInstrumentConstants.STATUS_ACTIVE,
                                                PaymentInstrumentConstants.STATUS_PENDING_RTD,
                                                PaymentInstrumentConstants.STATUS_PENDING_RE,
                                                PaymentInstrumentConstants.STATUS_PENDING_DEACTIVATION_REQUEST)))
                .thenReturn(paymentInstruments);
        try {
            HpanGetDTO hpanGetDTO = paymentInstrumentService.getHpan(INITIATIVE_ID, USER_ID);
            assertFalse(hpanGetDTO.getInstrumentList().isEmpty());
        } catch (PaymentInstrumentException e) {
            Assertions.fail();
        }
    }
    
    @Test
    void getHpan_status_pending_enrollment_ok() {
        final PaymentInstrument INSTRUMENT = PaymentInstrument.builder()
                .initiativeId(INITIATIVE_ID)
                .userId(USER_ID)
                .idWallet(ID_WALLET)
                .hpan(HPAN)
                .maskedPan(MASKED_PAN)
                .brandLogo(BRAND_LOGO)
                .status(PaymentInstrumentConstants.STATUS_PENDING_RE)
                .channel(CHANNEL)
                .build();
        List<PaymentInstrument> paymentInstruments = List.of(INSTRUMENT);
        
        Mockito.when(
                        paymentInstrumentRepositoryMock.findByInitiativeIdAndUserIdAndStatusIn(
                                INITIATIVE_ID,
                                USER_ID,  List.of(PaymentInstrumentConstants.STATUS_ACTIVE,
                                        PaymentInstrumentConstants.STATUS_PENDING_RTD,
                                        PaymentInstrumentConstants.STATUS_PENDING_RE,
                                        PaymentInstrumentConstants.STATUS_PENDING_DEACTIVATION_REQUEST)))
                .thenReturn(paymentInstruments);
        try {
            HpanGetDTO hpanGetDTO = paymentInstrumentService.getHpan(INITIATIVE_ID, USER_ID);
            HpanDTO actual = hpanGetDTO.getInstrumentList().get(0);
            assertEquals(INSTRUMENT.getId(), actual.getInstrumentId());
            assertEquals(INSTRUMENT.getChannel(), actual.getChannel());
            assertEquals(INSTRUMENT.getMaskedPan(), actual.getMaskedPan());
            assertEquals(PaymentInstrumentConstants.STATUS_PENDING_ENROLLMENT_REQUEST, actual.getStatus());
            assertFalse(hpanGetDTO.getInstrumentList().isEmpty());
        } catch (PaymentInstrumentException e) {
            Assertions.fail();
        }
    }
    
    @Test
    void getHpan_status_pending_deactivation_ok() {
        final PaymentInstrument INSTRUMENT = PaymentInstrument.builder()
                .initiativeId(INITIATIVE_ID)
                .userId(USER_ID)
                .idWallet(ID_WALLET)
                .hpan(HPAN)
                .maskedPan(MASKED_PAN)
                .brandLogo(BRAND_LOGO)
                .status(PaymentInstrumentConstants.STATUS_PENDING_DEACTIVATION_REQUEST)
                .channel(CHANNEL)
                .build();
        List<PaymentInstrument> paymentInstruments = List.of(INSTRUMENT);
        
        Mockito.when(
                        paymentInstrumentRepositoryMock.findByInitiativeIdAndUserIdAndStatusIn(
                                INITIATIVE_ID,
                                USER_ID, List.of(PaymentInstrumentConstants.STATUS_ACTIVE,
                                                PaymentInstrumentConstants.STATUS_PENDING_RTD,
                                                PaymentInstrumentConstants.STATUS_PENDING_RE,
                                                PaymentInstrumentConstants.STATUS_PENDING_DEACTIVATION_REQUEST)))
                .thenReturn(paymentInstruments);
        try {
            HpanGetDTO hpanGetDTO = paymentInstrumentService.getHpan(INITIATIVE_ID, USER_ID);
            HpanDTO actual = hpanGetDTO.getInstrumentList().get(0);
            assertEquals(INSTRUMENT.getId(), actual.getInstrumentId());
            assertEquals(INSTRUMENT.getChannel(), actual.getChannel());
            assertEquals(INSTRUMENT.getMaskedPan(), actual.getMaskedPan());
            assertEquals(INSTRUMENT.getBrandLogo(), actual.getBrandLogo());
            assertFalse(hpanGetDTO.getInstrumentList().isEmpty());
        } catch (PaymentInstrumentException e) {
            Assertions.fail();
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
                            TEST_INSTRUMENT.setDeactivationDate(TEST_DATE);
                            return null;
                        })
                .when(paymentInstrumentRepositoryMock).save(Mockito.any(PaymentInstrument.class));
        
        paymentInstrumentService.deactivateAllInstruments(INITIATIVE_ID, USER_ID,
                LocalDateTime.now().toString());
        assertNotNull(TEST_INSTRUMENT.getDeactivationDate());
        assertEquals(PaymentInstrumentConstants.STATUS_INACTIVE, TEST_INSTRUMENT.getStatus());
    }
    
    @Test
    void disableAllPayInstrument_ok_queue_error() {
        
        List<PaymentInstrument> paymentInstruments = new ArrayList<>();
        paymentInstruments.add(TEST_INSTRUMENT);
        
        Mockito.when(
                        paymentInstrumentRepositoryMock.findByInitiativeIdAndUserIdAndStatus(INITIATIVE_ID, USER_ID,
                                PaymentInstrumentConstants.STATUS_ACTIVE))
                .thenReturn(paymentInstruments);
        
        Mockito.doThrow(new PaymentInstrumentException(400, "")).when(rtdProducer)
                .sendInstrument(Mockito.any(
                        RTDOperationDTO.class));
        
        Mockito.doAnswer(
                        invocationOnMock -> {
                            TEST_INSTRUMENT.setStatus(PaymentInstrumentConstants.STATUS_INACTIVE);
                            TEST_INSTRUMENT.setDeactivationDate(TEST_DATE);
                            return null;
                        })
                .when(paymentInstrumentRepositoryMock).save(Mockito.any(PaymentInstrument.class));
        
        paymentInstrumentService.deactivateAllInstruments(INITIATIVE_ID, USER_ID,
                LocalDateTime.now().toString());
        assertNotNull(TEST_INSTRUMENT.getDeactivationDate());
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
                            TEST_INSTRUMENT.setDeactivationDate(TEST_DATE);
                            return null;
                        })
                .when(paymentInstrumentRepositoryMock).save(Mockito.any(PaymentInstrument.class));
        
        try {
            paymentInstrumentService.deactivateAllInstruments(INITIATIVE_ID, USER_ID,
                    LocalDateTime.now().toString());
            fail();
        } catch (Exception e) {
            assertNull(TEST_INSTRUMENT.getDeactivationDate());
            assertNotEquals(PaymentInstrumentConstants.STATUS_INACTIVE, TEST_INSTRUMENT.getStatus());
        }
        
    }
    
    @Test
    void deactivateInstrument_PM() {
        TEST_INSTRUMENT.setStatus(PaymentInstrumentConstants.STATUS_ACTIVE);
        TEST_INSTRUMENT.setDeactivationDate(null);
        EncryptedCfDTO encryptedCfDTO = new EncryptedCfDTO(USER_ID);
        
        Mockito.when(
                        paymentInstrumentRepositoryMock.findByInitiativeIdAndUserIdAndStatusNotContaining(HPAN,
                                USER_ID,
                                PaymentInstrumentConstants.STATUS_INACTIVE))
                .thenReturn(List.of(TEST_INSTRUMENT, TEST_INACTIVE_INSTRUMENT));
        
        Mockito.when(encryptRestConnector.upsertToken(Mockito.any(CFDTO.class)))
                .thenReturn(encryptedCfDTO);
        
        Mockito.doNothing().when(walletRestConnector).updateWallet(Mockito.any(WalletCallDTO.class));
        
        Mockito.when(paymentInstrumentRepositoryMock.countByHpanAndStatusIn(HPAN,
                List.of(PaymentInstrumentConstants.STATUS_ACTIVE,
                        PaymentInstrumentConstants.STATUS_PENDING_DEACTIVATION_REQUEST))).thenReturn(0);
        
        Mockito.doAnswer(invocationOnMock -> {
            TEST_INSTRUMENT.setStatus(PaymentInstrumentConstants.STATUS_INACTIVE);
            TEST_INSTRUMENT.setDeactivationDate(TEST_DATE);
            return null;
        }).when(paymentInstrumentRepositoryMock).save(Mockito.any(PaymentInstrument.class));
        
        try {
            RTDRevokeCardDTO RTDRevokeCardDTO = new RTDRevokeCardDTO("RevokeCard", RTD_MESSAGE);
            paymentInstrumentService.processRtdMessage(RTDRevokeCardDTO);
        } catch (PaymentInstrumentException e) {
            Assertions.fail();
        }
        assertEquals(PaymentInstrumentConstants.STATUS_INACTIVE, TEST_INSTRUMENT.getStatus());
        assertNotNull(TEST_INSTRUMENT.getDeactivationDate());
        assertEquals(TEST_DATE, TEST_INSTRUMENT.getDeactivationDate());
    }
    
    @Test
    void deactivateInstrument_PM_pending_enrollment() {
        TEST_INSTRUMENT.setStatus(PaymentInstrumentConstants.STATUS_PENDING_ENROLLMENT_REQUEST);
        TEST_INSTRUMENT.setDeactivationDate(null);
        EncryptedCfDTO encryptedCfDTO = new EncryptedCfDTO(USER_ID);
        
        Mockito.when(
                        paymentInstrumentRepositoryMock.findByInitiativeIdAndUserIdAndStatusNotContaining(HPAN,
                                USER_ID,
                                PaymentInstrumentConstants.STATUS_INACTIVE))
                .thenReturn(List.of(TEST_INSTRUMENT, TEST_INACTIVE_INSTRUMENT));
        
        Mockito.when(encryptRestConnector.upsertToken(Mockito.any(CFDTO.class)))
                .thenReturn(encryptedCfDTO);
        
        Mockito.when(paymentInstrumentRepositoryMock.countByHpanAndStatusIn(HPAN,
                List.of(PaymentInstrumentConstants.STATUS_ACTIVE,
                        PaymentInstrumentConstants.STATUS_PENDING_DEACTIVATION_REQUEST))).thenReturn(0);
        
        Mockito.doAnswer(invocationOnMock -> {
            TEST_INSTRUMENT.setStatus(PaymentInstrumentConstants.STATUS_INACTIVE);
            TEST_INSTRUMENT.setDeactivationDate(TEST_DATE);
            return null;
        }).when(paymentInstrumentRepositoryMock).save(Mockito.any(PaymentInstrument.class));
        
        try {
            RTDRevokeCardDTO RTDRevokeCardDTO = new RTDRevokeCardDTO("RevokeCard", RTD_MESSAGE);
            paymentInstrumentService.processRtdMessage(RTDRevokeCardDTO);
        } catch (PaymentInstrumentException e) {
            Assertions.fail();
        }
        assertEquals(PaymentInstrumentConstants.STATUS_INACTIVE, TEST_INSTRUMENT.getStatus());
        assertNotNull(TEST_INSTRUMENT.getDeactivationDate());
        assertEquals(TEST_DATE, TEST_INSTRUMENT.getDeactivationDate());
    }
    
    @Test
    void deactivateInstrument_PM_pending_deactivation() {
        TEST_INSTRUMENT.setStatus(PaymentInstrumentConstants.STATUS_PENDING_DEACTIVATION_REQUEST);
        TEST_INSTRUMENT.setDeactivationDate(null);
        EncryptedCfDTO encryptedCfDTO = new EncryptedCfDTO(USER_ID);
        
        Mockito.when(
                        paymentInstrumentRepositoryMock.findByInitiativeIdAndUserIdAndStatusNotContaining(HPAN,
                                USER_ID,
                                PaymentInstrumentConstants.STATUS_INACTIVE))
                .thenReturn(List.of(TEST_INSTRUMENT, TEST_INACTIVE_INSTRUMENT));
        
        Mockito.when(encryptRestConnector.upsertToken(Mockito.any(CFDTO.class)))
                .thenReturn(encryptedCfDTO);
        
        Mockito.when(paymentInstrumentRepositoryMock.countByHpanAndStatusIn(HPAN,
                List.of(PaymentInstrumentConstants.STATUS_ACTIVE,
                        PaymentInstrumentConstants.STATUS_PENDING_DEACTIVATION_REQUEST))).thenReturn(0);
        
        Mockito.doAnswer(invocationOnMock -> {
            TEST_INSTRUMENT.setStatus(PaymentInstrumentConstants.STATUS_INACTIVE);
            TEST_INSTRUMENT.setDeactivationDate(TEST_DATE);
            return null;
        }).when(paymentInstrumentRepositoryMock).save(Mockito.any(PaymentInstrument.class));
        
        try {
            RTDRevokeCardDTO RTDRevokeCardDTO = new RTDRevokeCardDTO("RevokeCard", RTD_MESSAGE);
            paymentInstrumentService.processRtdMessage(RTDRevokeCardDTO);
        } catch (PaymentInstrumentException e) {
            Assertions.fail();
        }
        assertEquals(PaymentInstrumentConstants.STATUS_INACTIVE, TEST_INSTRUMENT.getStatus());
        assertNotNull(TEST_INSTRUMENT.getDeactivationDate());
        assertEquals(TEST_DATE, TEST_INSTRUMENT.getDeactivationDate());
    }
    
    @Test
    void deactivateInstrument_PM_KO_PDV() {
        Mockito.doThrow(new PaymentInstrumentException(404, "")).when(encryptRestConnector)
                .upsertToken(Mockito.any());
        RTDRevokeCardDTO RTDRevokeCardDTO = new RTDRevokeCardDTO("RevokeCard", RTD_MESSAGE);
        paymentInstrumentService.processRtdMessage(RTDRevokeCardDTO);
        
        Mockito.verify(walletRestConnector, Mockito.times(0)).updateWallet(Mockito.any());
        
    }
    
    @Test
    void deactivateInstrument_PM_queue_error_rule_engine() {
        TEST_INSTRUMENT.setStatus(PaymentInstrumentConstants.STATUS_ACTIVE);
        TEST_INSTRUMENT.setDeactivationDate(null);
        EncryptedCfDTO encryptedCfDTO = new EncryptedCfDTO(USER_ID);
        
        Mockito.when(
                        paymentInstrumentRepositoryMock.findByInitiativeIdAndUserIdAndStatusNotContaining(HPAN,
                                USER_ID,
                                PaymentInstrumentConstants.STATUS_INACTIVE))
                .thenReturn(List.of(TEST_INSTRUMENT, TEST_INACTIVE_INSTRUMENT));
        
        Mockito.when(encryptRestConnector.upsertToken(Mockito.any(CFDTO.class)))
                .thenReturn(encryptedCfDTO);
        
        Mockito.doNothing().when(walletRestConnector).updateWallet(Mockito.any(WalletCallDTO.class));
        
        Mockito.doThrow(new PaymentInstrumentException(400, "")).when(producer)
                .sendInstruments(Mockito.any());
        
        Mockito.when(paymentInstrumentRepositoryMock.countByHpanAndStatusIn(HPAN,
                List.of(PaymentInstrumentConstants.STATUS_ACTIVE,
                        PaymentInstrumentConstants.STATUS_PENDING_DEACTIVATION_REQUEST))).thenReturn(0);
        
        Mockito.when(messageMapper.apply(Mockito.any(RuleEngineQueueDTO.class))).thenReturn(
                MessageBuilder.withPayload(new RuleEngineQueueDTO()).build());
        
        Mockito.doAnswer(invocationOnMock -> {
            TEST_INSTRUMENT.setStatus(PaymentInstrumentConstants.STATUS_INACTIVE);
            TEST_INSTRUMENT.setDeactivationDate(TEST_DATE);
            TEST_INSTRUMENT.setDeactivationDate(TEST_DATE);
            return null;
        }).when(paymentInstrumentRepositoryMock).save(Mockito.any(PaymentInstrument.class));
        
        try {
            RTDRevokeCardDTO RTDRevokeCardDTO = new RTDRevokeCardDTO("RevokeCard", RTD_MESSAGE);
            paymentInstrumentService.processRtdMessage(RTDRevokeCardDTO);
        } catch (PaymentInstrumentException e) {
            Assertions.fail();
        }
        assertEquals(PaymentInstrumentConstants.STATUS_INACTIVE, TEST_INSTRUMENT.getStatus());
        assertNotNull(TEST_INSTRUMENT.getDeactivationDate());
        assertEquals(TEST_DATE, TEST_INSTRUMENT.getDeactivationDate());
        assertEquals(PaymentInstrumentConstants.PM, TEST_INSTRUMENT.getDeleteChannel());
    }
    
    @Test
    void rollback() {
        List<PaymentInstrument> paymentInstrumentList = new ArrayList<>();
        paymentInstrumentList.add(TEST_INSTRUMENT);
        paymentInstrumentService.rollbackInstruments(paymentInstrumentList);
        assertNull(TEST_INSTRUMENT.getDeactivationDate());
        assertNotEquals(PaymentInstrumentConstants.STATUS_INACTIVE, TEST_INSTRUMENT.getStatus());
    }
    
    @Test
    void deactivateInstrument_PM_KO_NotFound() {
        TEST_INSTRUMENT.setStatus(PaymentInstrumentConstants.STATUS_ACTIVE);
        TEST_INSTRUMENT.setDeactivationDate(null);
        EncryptedCfDTO encryptedCfDTO = new EncryptedCfDTO(USER_ID);
        Mockito.when(
                        paymentInstrumentRepositoryMock.findByHpanAndUserIdAndStatus(HPAN, USER_ID,
                                PaymentInstrumentConstants.STATUS_ACTIVE))
                .thenReturn(List.of());
        Mockito.when(encryptRestConnector.upsertToken(Mockito.any(CFDTO.class)))
                .thenReturn(encryptedCfDTO);
        RTDRevokeCardDTO RTDRevokeCardDTO = new RTDRevokeCardDTO("RevokeCard", RTD_MESSAGE);
        paymentInstrumentService.processRtdMessage(RTDRevokeCardDTO);
        
        Mockito.verify(walletRestConnector, Mockito.times(0)).updateWallet(Mockito.any());
    }
    
    @Test
    void processAck_deactivation_ok() {
        final RuleEngineAckDTO dto = new RuleEngineAckDTO(INITIATIVE_ID, USER_ID,
                PaymentInstrumentConstants.OPERATION_DELETE, List.of(HPAN), List.of(), LocalDateTime.now());
        
        Mockito.when(
                        paymentInstrumentRepositoryMock.findByInitiativeIdAndUserIdAndHpanAndStatus(INITIATIVE_ID,
                                USER_ID, HPAN, PaymentInstrumentConstants.STATUS_PENDING_DEACTIVATION_REQUEST))
                .thenReturn(Optional.of(TEST_PENDING_DEACTIVATION_INSTRUMENT));
        
        Mockito.when(paymentInstrumentRepositoryMock.countByHpanAndStatusIn(HPAN,
                List.of(PaymentInstrumentConstants.STATUS_ACTIVE,
                        PaymentInstrumentConstants.STATUS_PENDING_DEACTIVATION_REQUEST))).thenReturn(0);
        
        paymentInstrumentService.processAck(dto);
        
        assertEquals(dto.getTimestamp(), TEST_PENDING_DEACTIVATION_INSTRUMENT.getDeactivationDate());
        assertEquals(PaymentInstrumentConstants.STATUS_INACTIVE,
                TEST_PENDING_DEACTIVATION_INSTRUMENT.getStatus());
        Mockito.verify(paymentInstrumentRepositoryMock, Mockito.times(1))
                .save(Mockito.any(PaymentInstrument.class));
    }
    
    @Test
    void processAck_deactivation_ok_no_rtd() {
        final RuleEngineAckDTO dto = new RuleEngineAckDTO(INITIATIVE_ID, USER_ID,
                PaymentInstrumentConstants.OPERATION_DELETE, List.of(HPAN), List.of(), LocalDateTime.now());
        
        Mockito.when(
                        paymentInstrumentRepositoryMock.findByInitiativeIdAndUserIdAndHpanAndStatus(INITIATIVE_ID,
                                USER_ID, HPAN, PaymentInstrumentConstants.STATUS_PENDING_DEACTIVATION_REQUEST))
                .thenReturn(Optional.of(TEST_PENDING_DEACTIVATION_INSTRUMENT));
        
        Mockito.when(paymentInstrumentRepositoryMock.countByHpanAndStatusIn(HPAN,
                List.of(PaymentInstrumentConstants.STATUS_ACTIVE,
                        PaymentInstrumentConstants.STATUS_PENDING_DEACTIVATION_REQUEST))).thenReturn(1);
        
        paymentInstrumentService.processAck(dto);
        
        assertEquals(dto.getTimestamp(), TEST_PENDING_DEACTIVATION_INSTRUMENT.getDeactivationDate());
        assertEquals(PaymentInstrumentConstants.STATUS_INACTIVE,
                TEST_PENDING_DEACTIVATION_INSTRUMENT.getStatus());
        Mockito.verify(paymentInstrumentRepositoryMock, Mockito.times(1))
                .save(Mockito.any(PaymentInstrument.class));
    }
    
    @Test
    void processAck_deactivation_ok_duplicate() {
        
        final RuleEngineAckDTO dto = new RuleEngineAckDTO(INITIATIVE_ID, USER_ID,
                PaymentInstrumentConstants.OPERATION_DELETE, List.of(HPAN), List.of(), LocalDateTime.now());
        
        Mockito.when(
                        paymentInstrumentRepositoryMock.findByInitiativeIdAndUserIdAndHpanAndStatus(INITIATIVE_ID,
                                USER_ID, HPAN, PaymentInstrumentConstants.STATUS_PENDING_DEACTIVATION_REQUEST))
                .thenReturn(Optional.empty());
        
        paymentInstrumentService.processAck(dto);
        
        Mockito.verify(paymentInstrumentRepositoryMock, Mockito.times(0))
                .save(Mockito.any(PaymentInstrument.class));
    }
    
    @Test
    void processAck_deactivation_ko() {
        final RuleEngineAckDTO dto = new RuleEngineAckDTO(INITIATIVE_ID, USER_ID,
                PaymentInstrumentConstants.OPERATION_DELETE, List.of(), List.of(HPAN), LocalDateTime.now());
        
        Mockito.when(
                        paymentInstrumentRepositoryMock.findByInitiativeIdAndUserIdAndHpanAndStatus(INITIATIVE_ID,
                                USER_ID, HPAN, PaymentInstrumentConstants.STATUS_PENDING_DEACTIVATION_REQUEST))
                .thenReturn(Optional.of(TEST_PENDING_DEACTIVATION_INSTRUMENT));
        
        paymentInstrumentService.processAck(dto);
        
        assertEquals(PaymentInstrumentConstants.STATUS_ACTIVE,
                TEST_PENDING_DEACTIVATION_INSTRUMENT.getStatus());
        Mockito.verify(paymentInstrumentRepositoryMock, Mockito.times(1))
                .save(Mockito.any(PaymentInstrument.class));
    }
    
    @Test
    void saveAckFromRTD() {
        final RTDEnrollAckDTO dto = new RTDEnrollAckDTO("EnrollAck", INITIATIVE_ID, RTD_MESSAGE);
        
        Mockito.when(paymentInstrumentRepositoryMock.findByHpanAndStatus(HPAN,
                PaymentInstrumentConstants.STATUS_ACTIVE)).thenReturn(List.of(TEST_INSTRUMENT));
        
        paymentInstrumentService.processRtdMessage(dto);
        
        assertNotNull(TEST_INSTRUMENT.getActivationDate());
    }
    
    @Test
    void saveAckFromRTD_not_idpay_message() {
        final RTDEnrollAckDTO dto = new RTDEnrollAckDTO("EnrollAck", INITIATIVE_ID, RTD_MESSAGE_NOT_IDPAY);
        
        Mockito.when(paymentInstrumentRepositoryMock.findByHpanAndStatus(HPAN,
                PaymentInstrumentConstants.STATUS_ACTIVE)).thenReturn(List.of(TEST_INSTRUMENT));
        
        paymentInstrumentService.processRtdMessage(dto);
        
        Mockito.verify(paymentInstrumentRepositoryMock, Mockito.times(0)).saveAll(Mockito.anyList());
    }
    
    @Test
    void saveAckFromRTD_missingActivationDate() {
        final RTDEnrollAckDTO dto = new RTDEnrollAckDTO("EnrollAck", INITIATIVE_ID, RTD_MESSAGE);
        
        Mockito.when(paymentInstrumentRepositoryMock.findByInitiativeIdAndHpanAndStatus(INITIATIVE_ID, HPAN,
                PaymentInstrumentConstants.STATUS_PENDING_RTD)).thenReturn(TEST_INSTRUMENT);
        Mockito.when(paymentInstrumentRepositoryMock.countByInitiativeIdAndUserIdAndStatusIn(INITIATIVE_ID, USER_ID,
                List.of(PaymentInstrumentConstants.STATUS_ACTIVE,
                        PaymentInstrumentConstants.STATUS_PENDING_DEACTIVATION_REQUEST))).thenReturn(4);
        Mockito.doNothing().when(walletRestConnector).processAck(Mockito.any());
        paymentInstrumentService.processRtdMessage(dto);
        assertNotNull(TEST_INSTRUMENT.getActivationDate());
    }
    
    @Test
    void saveAckFromRTD_ko_sendToRuleEngine() {
        final RTDEnrollAckDTO dto = new RTDEnrollAckDTO("EnrollAck", INITIATIVE_ID, RTD_MESSAGE);
        
        Mockito.when(paymentInstrumentRepositoryMock.findByInitiativeIdAndHpanAndStatus(INITIATIVE_ID, HPAN,
                PaymentInstrumentConstants.STATUS_PENDING_RTD)).thenReturn(TEST_INSTRUMENT_PENDING_RTD);
        
        Mockito.doThrow(new PaymentInstrumentException(HttpStatus.BAD_REQUEST.value(), "")).when(producer).sendInstruments(Mockito.any());
        paymentInstrumentService.processRtdMessage(dto);
        assertEquals(PaymentInstrumentConstants.STATUS_PENDING_RTD, TEST_INSTRUMENT_PENDING_RTD.getStatus());
    }
    
    @Test
    void checkPendingTimeLimit_ok() {
        PaymentInstrument paymentInstrument = TEST_ENROLLMENT_FAILED;
        paymentInstrument.setUpdateDate(TEST_TIMESTAMP);
        List<PaymentInstrument> paymentInstrumentList = new ArrayList<>();
        paymentInstrumentList.add(paymentInstrument);
        when(paymentInstrumentRepositoryMock.findByStatusRegex(any())).thenReturn(paymentInstrumentList);
        Mockito.when(paymentInstrumentRepositoryMock.save(any())).thenReturn(mock(PaymentInstrument.class));
        doNothing().when(rtdProducer).sendInstrument(any());
        
        paymentInstrumentService.getHpan(INITIATIVE_ID, USER_ID);
        
        assertEquals(PaymentInstrumentConstants.STATUS_ENROLLMENT_FAILED, paymentInstrument.getStatus());
    }
    
    @Test
    void checkPendingTimeLimit_lessThanFourHours() {
        PaymentInstrument paymentInstrument = TEST_ENROLLMENT_FAILED;
        paymentInstrument.setUpdateDate(TEST_DATE);
        
        paymentInstrumentService.getHpan(INITIATIVE_ID, USER_ID);
        
        assertNotEquals(TEST_TIMESTAMP, paymentInstrument.getUpdateDate());
    }
    
    @Test
    void checkPendingTimeLimit_ok_activeInstrument_isNotEmpty() {
        PaymentInstrument paymentInstrument = TEST_ENROLLMENT_FAILED;
        paymentInstrument.setUpdateDate(TEST_TIMESTAMP);
        List<PaymentInstrument> paymentInstrumentList = new ArrayList<>();
        paymentInstrumentList.add(paymentInstrument);
        when(paymentInstrumentRepositoryMock.findByStatusRegex(any())).thenReturn(paymentInstrumentList);
        Mockito.when(paymentInstrumentRepositoryMock.save(any())).thenReturn(mock(PaymentInstrument.class));
        doNothing().when(rtdProducer).sendInstrument(any());
        
        paymentInstrumentService.getHpan(INITIATIVE_ID, USER_ID);
        
        assertEquals(PaymentInstrumentConstants.STATUS_ENROLLMENT_FAILED, paymentInstrument.getStatus());
    }
    
    @Test
    void checkPendingTimeLimit_ok_activeInstrument_isEmpty() {
        PaymentInstrument paymentInstrument = TEST_ENROLLMENT_FAILED;
        paymentInstrument.setUpdateDate(TEST_TIMESTAMP);
        List<PaymentInstrument> paymentInstrumentList = new ArrayList<>();
        when(paymentInstrumentRepositoryMock.findByStatusRegex(any())).thenReturn(paymentInstrumentList);
        Mockito.when(paymentInstrumentRepositoryMock.save(any())).thenReturn(mock(PaymentInstrument.class));
        
        paymentInstrumentService.getHpan(INITIATIVE_ID, USER_ID);
        
        assertEquals(PaymentInstrumentConstants.STATUS_ENROLLMENT_FAILED, paymentInstrument.getStatus());
    }
    
    @Test
    void checkPendingTimeLimit_ok_PENDING_RE() {
        PaymentInstrument paymentInstrument = TEST_ENROLLMENT_FAILED;
        paymentInstrument.setUpdateDate(TEST_TIMESTAMP);
        paymentInstrument.setStatus(PaymentInstrumentConstants.STATUS_PENDING_RE);
        List<PaymentInstrument> paymentInstrumentList = new ArrayList<>();
        paymentInstrumentList.add(paymentInstrument);
        when(paymentInstrumentRepositoryMock.findByStatusRegex(any())).thenReturn(paymentInstrumentList);
        Mockito.when(paymentInstrumentRepositoryMock.save(any())).thenReturn(mock(PaymentInstrument.class));
        doNothing().when(rtdProducer).sendInstrument(any());
        PaymentMethodInfoList infoList = new PaymentMethodInfoList();
        infoList.setHpan(HPAN);
        infoList.setMaskedPan(MASKED_PAN);
        infoList.setBrandLogo(BRAND_LOGO);
        infoList.setConsent(true);
        doNothing().when(producer).sendInstruments(any());
        
        paymentInstrumentService.getHpan(INITIATIVE_ID, USER_ID);
        
        assertEquals(PaymentInstrumentConstants.STATUS_ENROLLMENT_FAILED, paymentInstrument.getStatus());
    }
    
    @Test
    void getHpanFromIssuer_status_active_ok() {
        final PaymentInstrument INSTRUMENT = PaymentInstrument.builder()
                .initiativeId(INITIATIVE_ID)
                .userId(USER_ID)
                .idWallet(ID_WALLET)
                .hpan(HPAN)
                .maskedPan(MASKED_PAN)
                .brandLogo(BRAND_LOGO)
                .status(PaymentInstrumentConstants.STATUS_ACTIVE)
                .channel(CHANNEL)
                .build();
        List<PaymentInstrument> paymentInstruments = List.of(INSTRUMENT);
        
        Mockito.when(
                        paymentInstrumentRepositoryMock.findByInitiativeIdAndUserIdAndChannelAndStatusNotContaining(
                                INITIATIVE_ID,
                                USER_ID, CHANNEL, PaymentInstrumentConstants.STATUS_INACTIVE))
                .thenReturn(paymentInstruments);
        try {
            HpanGetDTO hpanGetDTO = paymentInstrumentService.getHpanFromIssuer(INITIATIVE_ID, USER_ID,
                    CHANNEL);
            assertFalse(hpanGetDTO.getInstrumentList().isEmpty());
        } catch (PaymentInstrumentException e) {
            Assertions.fail();
        }
    }
    
    @Test
    void getHpanFromIssuer_status_pending_enrollment_ok() {
        final PaymentInstrument INSTRUMENT = PaymentInstrument.builder()
                .initiativeId(INITIATIVE_ID)
                .userId(USER_ID)
                .idWallet(ID_WALLET)
                .hpan(HPAN)
                .maskedPan(MASKED_PAN)
                .brandLogo(BRAND_LOGO)
                .status(PaymentInstrumentConstants.STATUS_PENDING_ENROLLMENT_REQUEST)
                .channel(CHANNEL)
                .build();
        List<PaymentInstrument> paymentInstruments = List.of(INSTRUMENT);
        
        Mockito.when(
                        paymentInstrumentRepositoryMock.findByInitiativeIdAndUserIdAndChannelAndStatusNotContaining(
                                INITIATIVE_ID,
                                USER_ID, CHANNEL, PaymentInstrumentConstants.STATUS_INACTIVE))
                .thenReturn(paymentInstruments);
        try {
            HpanGetDTO hpanGetDTO = paymentInstrumentService.getHpanFromIssuer(INITIATIVE_ID, USER_ID,
                    CHANNEL);
            HpanDTO actual = hpanGetDTO.getInstrumentList().get(0);
            assertEquals(INSTRUMENT.getId(), actual.getInstrumentId());
            assertEquals(INSTRUMENT.getChannel(), actual.getChannel());
            assertEquals(INSTRUMENT.getMaskedPan(), actual.getMaskedPan());
            assertEquals(INSTRUMENT.getBrandLogo(), actual.getBrandLogo());
            assertFalse(hpanGetDTO.getInstrumentList().isEmpty());
        } catch (PaymentInstrumentException e) {
            Assertions.fail();
        }
    }
    
    @Test
    void getHpanFromIssuer_status_pending_deactivation_ok() {
        final PaymentInstrument INSTRUMENT = PaymentInstrument.builder()
                .initiativeId(INITIATIVE_ID)
                .userId(USER_ID)
                .idWallet(ID_WALLET)
                .hpan(HPAN)
                .maskedPan(MASKED_PAN)
                .brandLogo(BRAND_LOGO)
                .status(PaymentInstrumentConstants.STATUS_PENDING_DEACTIVATION_REQUEST)
                .channel(CHANNEL)
                .build();
        List<PaymentInstrument> paymentInstruments = List.of(INSTRUMENT);
        
        Mockito.when(
                        paymentInstrumentRepositoryMock.findByInitiativeIdAndUserIdAndChannelAndStatusNotContaining(
                                INITIATIVE_ID,
                                USER_ID, CHANNEL, PaymentInstrumentConstants.STATUS_INACTIVE))
                .thenReturn(paymentInstruments);
        try {
            HpanGetDTO hpanGetDTO = paymentInstrumentService.getHpanFromIssuer(INITIATIVE_ID, USER_ID,
                    CHANNEL);
            HpanDTO actual = hpanGetDTO.getInstrumentList().get(0);
            assertEquals(INSTRUMENT.getId(), actual.getInstrumentId());
            assertEquals(INSTRUMENT.getChannel(), actual.getChannel());
            assertEquals(INSTRUMENT.getMaskedPan(), actual.getMaskedPan());
            assertEquals(INSTRUMENT.getBrandLogo(), actual.getBrandLogo());
            assertFalse(hpanGetDTO.getInstrumentList().isEmpty());
        } catch (PaymentInstrumentException e) {
            Assertions.fail();
        }
    }
    
    @Test
    void enrollIssuer_ok_empty() {
        
        final InstrumentIssuerDTO dto = new InstrumentIssuerDTO(INITIATIVE_ID, USER_ID, HPAN, CHANNEL,
                "", "");
        
        Mockito.when(paymentInstrumentRepositoryMock.findByHpanAndStatusNotContaining(HPAN,
                PaymentInstrumentConstants.STATUS_INACTIVE)).thenReturn(new ArrayList<>());
        
        try {
            paymentInstrumentService.enrollFromIssuer(dto);
        } catch (PaymentInstrumentException e) {
            fail();
        }
    }
    
    @Test
    void enrollIssuer_ok_idemp() {
        final InstrumentIssuerDTO dto = new InstrumentIssuerDTO(INITIATIVE_ID, USER_ID, HPAN, CHANNEL,
                "", "");
        Mockito.when(paymentInstrumentRepositoryMock.findByHpanAndStatusNotContaining(HPAN,
                PaymentInstrumentConstants.STATUS_INACTIVE)).thenReturn(List.of(TEST_INSTRUMENT));
        
        try {
            paymentInstrumentService.enrollFromIssuer(dto);
        } catch (PaymentInstrumentException e) {
            Assertions.fail();
        }
    }
    
    @Test
    void enrollIssuer_ok_other_initiative() {
        final InstrumentIssuerDTO dto = new InstrumentIssuerDTO(INITIATIVE_ID, USER_ID, HPAN, CHANNEL,
                "", "");
        Mockito.when(paymentInstrumentRepositoryMock.findByHpanAndStatusNotContaining(HPAN,
                PaymentInstrumentConstants.STATUS_INACTIVE)).thenReturn(List.of(TEST_INSTRUMENT));
        
        try {
            paymentInstrumentService.enrollFromIssuer(dto);
        } catch (PaymentInstrumentException e) {
            fail();
        }
        assertEquals(HPAN, TEST_INSTRUMENT.getHpan());
    }
    
    @Test
    void enrollIssuer_ok_not_already_active() {
        final InstrumentIssuerDTO dto = new InstrumentIssuerDTO("INITIATIVE_ID", USER_ID, HPAN, CHANNEL, "", "");
        PaymentInstrument paymentInstrument = TEST_INSTRUMENT;
        List<PaymentInstrument> paymentInstrumentList = new ArrayList<>();
        paymentInstrumentList.add(paymentInstrument);
        Mockito.when(paymentInstrumentRepositoryMock.findByHpanAndStatusNotContaining(HPAN,
                PaymentInstrumentConstants.STATUS_INACTIVE)).thenReturn(paymentInstrumentList);
        
        paymentInstrumentService.enrollFromIssuer(dto);
        
        assertNotEquals(dto.getInitiativeId(), paymentInstrument.getInitiativeId());
    }
    
    @Test
    void enrollIssuer_ko_already_active() {
        final InstrumentIssuerDTO dto = new InstrumentIssuerDTO(INITIATIVE_ID, USER_ID_FAIL, HPAN, CHANNEL, "", "");
        Mockito.when(paymentInstrumentRepositoryMock.findByHpanAndStatusNotContaining(HPAN,
                PaymentInstrumentConstants.STATUS_INACTIVE)).thenReturn(List.of(TEST_INSTRUMENT));
        
        try {
            paymentInstrumentService.enrollFromIssuer(dto);
        } catch (PaymentInstrumentException e) {
            assertEquals(HttpStatus.FORBIDDEN.value(), e.getCode());
            assertEquals(PaymentInstrumentConstants.ERROR_PAYMENT_INSTRUMENT_ALREADY_ACTIVE,
                    e.getMessage());
        }
    }
    
    @Test
    void enrollIssuer_ko_rule_engine() {
        final InstrumentIssuerDTO dto = new InstrumentIssuerDTO(INITIATIVE_ID, USER_ID, HPAN, CHANNEL,
                "", "");
        Mockito.when(paymentInstrumentRepositoryMock.findByHpanAndStatus(HPAN,
                PaymentInstrumentConstants.STATUS_ACTIVE)).thenReturn(new ArrayList<>());
        
        Mockito.when(paymentInstrumentRepositoryMock.countByHpanAndStatusIn(HPAN,
                List.of(PaymentInstrumentConstants.STATUS_ACTIVE,
                        PaymentInstrumentConstants.STATUS_PENDING_DEACTIVATION_REQUEST))).thenReturn(0);
        
        doNothing().when(paymentInstrumentRepositoryMock).delete(any());
        
        Mockito.doThrow(new PaymentInstrumentException(400, "")).when(rtdProducer)
                .sendInstrument(Mockito.any());
        
        try {
            paymentInstrumentService.enrollFromIssuer(dto);
            Assertions.fail();
        } catch (PaymentInstrumentException e) {
            assertEquals(HttpStatus.BAD_REQUEST.value(), e.getCode());
        }
    }
    
}