package it.gov.pagopa.payment.instrument.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import it.gov.pagopa.payment.instrument.connector.DecryptRestConnector;
import it.gov.pagopa.payment.instrument.connector.EncryptRestConnector;
import it.gov.pagopa.payment.instrument.connector.PMRestClientConnector;
import it.gov.pagopa.payment.instrument.connector.RewardCalculatorConnector;
import it.gov.pagopa.payment.instrument.connector.WalletRestConnector;
import it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants;
import it.gov.pagopa.payment.instrument.dto.CFDTO;
import it.gov.pagopa.payment.instrument.dto.DecryptCfDTO;
import it.gov.pagopa.payment.instrument.dto.EncryptedCfDTO;
import it.gov.pagopa.payment.instrument.dto.HpanDTO;
import it.gov.pagopa.payment.instrument.dto.HpanGetDTO;
import it.gov.pagopa.payment.instrument.dto.InstrumentAckDTO;
import it.gov.pagopa.payment.instrument.dto.InstrumentDetailDTO;
import it.gov.pagopa.payment.instrument.dto.InstrumentIssuerDTO;
import it.gov.pagopa.payment.instrument.dto.QueueCommandOperationDTO;
import it.gov.pagopa.payment.instrument.dto.RuleEngineAckDTO;
import it.gov.pagopa.payment.instrument.dto.RuleEngineRequestDTO;
import it.gov.pagopa.payment.instrument.dto.WalletCallDTO;
import it.gov.pagopa.payment.instrument.dto.mapper.AckMapper;
import it.gov.pagopa.payment.instrument.dto.mapper.MessageMapper;
import it.gov.pagopa.payment.instrument.dto.pm.PaymentMethodInfo;
import it.gov.pagopa.payment.instrument.dto.pm.PaymentMethodInfo.BPayPaymentInstrumentWallet;
import it.gov.pagopa.payment.instrument.dto.pm.PaymentMethodInfoList;
import it.gov.pagopa.payment.instrument.dto.pm.WalletV2;
import it.gov.pagopa.payment.instrument.dto.pm.WalletV2ListResponse;
import it.gov.pagopa.payment.instrument.dto.rtd.RTDEnrollAckDTO;
import it.gov.pagopa.payment.instrument.dto.rtd.RTDMessage;
import it.gov.pagopa.payment.instrument.dto.rtd.RTDRevokeCardDTO;
import it.gov.pagopa.payment.instrument.event.producer.ErrorProducer;
import it.gov.pagopa.payment.instrument.event.producer.RTDProducer;
import it.gov.pagopa.payment.instrument.event.producer.RuleEngineProducer;
import it.gov.pagopa.payment.instrument.exception.PaymentInstrumentException;
import it.gov.pagopa.payment.instrument.model.PaymentInstrument;
import it.gov.pagopa.payment.instrument.repository.PaymentInstrumentRepository;
import it.gov.pagopa.payment.instrument.repository.PaymentInstrumentRepositoryExtended;
import it.gov.pagopa.payment.instrument.utils.AuditUtilities;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@ContextConfiguration(classes = PaymentInstrumentServiceImpl.class)
@TestPropertySource(
        locations = "classpath:application.yml",
        properties = {
                "app.delete.paginationSize=100",
                "app.delete.delayTime=1000"
        })
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
    @MockBean
    RewardCalculatorConnector rewardCalculatorConnector;
    @Autowired
    PaymentInstrumentService paymentInstrumentService;
    @MockBean
    MessageMapper messageMapper;
    @MockBean
    AckMapper ackMapper;
    @MockBean
    AuditUtilities auditUtilities;
    @MockBean
    PaymentInstrumentRepositoryExtended paymentInstrumentRepositoryExtended;
    private static final String USER_ID = "TEST_USER_ID";
    private static final String USER_ID_FAIL = "TEST_USER_ID_FAIL";
    private static final String INITIATIVE_ID = "TEST_INITIATIVE_ID";
    private static final String INITIATIVE_ID_OTHER = "TEST_INITIATIVE_ID_OTHER";
    private static final String INITIATIVE_ID_ANOTHER = "TEST_INITIATIVE_ID_ANOTHER";
    private static final String OPERATION_TYPE_DELETE_INITIATIVE = "DELETE_INITIATIVE";
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
    private static final String BRAND_LOGO = "BRAND_LOGO";
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
    public static final String CARD = "CARD";
    private static final WalletV2 WALLET_V2_CARD = new WalletV2(CREATE_DATE, ENABLEABLE_FUNCTIONS,
            FAVOURITE, ID_WALLET, ONBOARDING_CHANNEL, UPDATE_DATE, CARD, PAYMENT_METHOD_INFO);
    private static final WalletV2 WALLET_V2_PBD_KO = new WalletV2(CREATE_DATE,
            ENABLEABLE_FUNCTIONS_KO,
            FAVOURITE, ID_WALLET, ONBOARDING_CHANNEL, UPDATE_DATE, CARD, PAYMENT_METHOD_INFO);
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
    @Value("${app.delete.paginationSize}")
    private String pagination;
    private static final PaymentInstrument TEST_INSTRUMENT = PaymentInstrument.builder()
            .id(INSTRUMENT_ID)
            .initiativeId(INITIATIVE_ID)
            .userId(USER_ID)
            .idWallet(ID_WALLET)
            .hpan(HPAN)
            .maskedPan(MASKED_PAN)
            .brandLogo(BRAND_LOGO)
            .brand(BRAND)
            .instrumentType(PaymentInstrumentConstants.INSTRUMENT_TYPE_CARD)
            .consent(CONSENT)
            .status(PaymentInstrumentConstants.STATUS_ACTIVE)
            .channel(CHANNEL)
            .deleteChannel(DELETE_CHANNEL)
            .activationDate(TEST_ACTIVATION_DATE)
            .deactivationDate(TEST_DEACTIVATION_DATE)
            .rtdAckDate(TEST_RULE_ENGINE_ACKDATE)
            .updateDate(TEST_DATE)
            .build();

    private static final PaymentInstrument TEST_INSTRUMENT_ACTIVE = PaymentInstrument.builder()
            .id(INSTRUMENT_ID)
            .initiativeId(INITIATIVE_ID)
            .userId(USER_ID)
            .idWallet(ID_WALLET)
            .hpan(HPAN)
            .maskedPan(MASKED_PAN)
            .brandLogo(BRAND_LOGO)
            .brand(BRAND)
            .consent(CONSENT)
            .status(PaymentInstrumentConstants.STATUS_ACTIVE)
            .channel(CHANNEL)
            .deleteChannel(DELETE_CHANNEL)
            .activationDate(TEST_ACTIVATION_DATE)
            .deactivationDate(TEST_DEACTIVATION_DATE)
            .rtdAckDate(TEST_RULE_ENGINE_ACKDATE)
            .updateDate(TEST_DATE.plusHours(1))
            .build();
    
    private static final PaymentInstrument TEST_PENDING_ENROLLMENT_INSTRUMENT = PaymentInstrument.builder()
            .id(INSTRUMENT_ID)
            .initiativeId(INITIATIVE_ID_OTHER)
            .userId(USER_ID)
            .idWallet(ID_WALLET)
            .hpan(HPAN)
            .maskedPan(MASKED_PAN)
            .brandLogo(BRAND_LOGO)
            .instrumentType(CARD)
            .brand(BRAND)
            .consent(CONSENT)
            .status(PaymentInstrumentConstants.STATUS_PENDING_RE)
            .channel(CHANNEL)
            .deleteChannel(DELETE_CHANNEL)
            .activationDate(TEST_ACTIVATION_DATE)
            .deactivationDate(TEST_DEACTIVATION_DATE)
            .rtdAckDate(TEST_RULE_ENGINE_ACKDATE)
            .updateDate(TEST_DATE)
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
            .updateDate(TEST_DATE)
            .build();
    
    private static final PaymentInstrument TEST_INACTIVE_INSTRUMENT = PaymentInstrument.builder()
            .id(INSTRUMENT_ID)
            .initiativeId(INITIATIVE_ID_ANOTHER)
            .userId(USER_ID)
            .idWallet(ID_WALLET)
            .hpan(HPAN)
            .maskedPan(MASKED_PAN)
            .brandLogo(BRAND_LOGO)
            .brand(BRAND)
            .consent(CONSENT)
            .status(PaymentInstrumentConstants.STATUS_INACTIVE)
            .channel(CHANNEL)
            .deleteChannel(DELETE_CHANNEL)
            .activationDate(TEST_ACTIVATION_DATE)
            .deactivationDate(TEST_DEACTIVATION_DATE)
            .rtdAckDate(TEST_RULE_ENGINE_ACKDATE)
            .updateDate(TEST_DATE)
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
            .updateDate(TEST_DATE)
            .build();
    
    private static final PaymentInstrument TEST_ENROLLMENT_FAILED = PaymentInstrument.builder()
            .id(INSTRUMENT_ID)
            .initiativeId(INITIATIVE_ID)
            .userId(USER_ID)
            .idWallet(ID_WALLET)
            .hpan(HPAN)
            .maskedPan(MASKED_PAN)
            .brandLogo(BRAND_LOGO)
            .brand(BRAND)
            .consent(CONSENT)
            .status(PaymentInstrumentConstants.STATUS_ENROLLMENT_FAILED)
            .channel(CHANNEL)
            .deleteChannel(DELETE_CHANNEL)
            .activationDate(TEST_ACTIVATION_DATE)
            .deactivationDate(TEST_DEACTIVATION_DATE)
            .rtdAckDate(TEST_RULE_ENGINE_ACKDATE)
            .updateDate(TEST_DATE.minusHours(1))
            .build();
    private static final PaymentInstrument TEST_ENROLLMENT_FAILED_KO_RE = PaymentInstrument.builder()
            .id(INSTRUMENT_ID)
            .initiativeId(INITIATIVE_ID)
            .userId(USER_ID)
            .idWallet(ID_WALLET)
            .hpan(HPAN)
            .maskedPan(MASKED_PAN)
            .brandLogo(BRAND_LOGO)
            .consent(CONSENT)
            .status(PaymentInstrumentConstants.STATUS_ENROLLMENT_FAILED_KO_RE)
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
            .operationType(PaymentInstrumentConstants.OPERATION_ADD)
            .channel(CHANNEL)
            .build();

    @Test
    void enrollInstrument_ok_idemp() {
        Mockito.when(paymentInstrumentRepositoryMock.findByHpan(HPAN)).thenReturn(List.of(TEST_INSTRUMENT_ACTIVE));
        Mockito.when(decryptRestConnector.getPiiByToken(USER_ID)).thenReturn(DECRYPT_CF_DTO);
        Mockito.when(
                pmRestClientConnector.getWalletList(USER_ID)).thenReturn(WALLET_V_2_LIST_RESPONSE_CARD);
        try {
            paymentInstrumentService.enrollInstrument(INITIATIVE_ID, USER_ID, ID_WALLET, CHANNEL, PaymentInstrumentConstants.INSTRUMENT_TYPE_CARD
            );
        } catch (PaymentInstrumentException e) {
            fail();
        }
    }

    @Test
    void enrollInstrument_ok_empty() {
        Mockito.when(paymentInstrumentRepositoryMock.findByHpan(HPAN)).thenReturn(new ArrayList<>());
        
        Mockito.when(decryptRestConnector.getPiiByToken(USER_ID)).thenReturn(DECRYPT_CF_DTO);
        Mockito.when(
                pmRestClientConnector.getWalletList(USER_ID)).thenReturn(WALLET_V_2_LIST_RESPONSE_CARD);
        
        try {
            paymentInstrumentService.enrollInstrument(INITIATIVE_ID, USER_ID, ID_WALLET, CHANNEL, PaymentInstrumentConstants.INSTRUMENT_TYPE_CARD
            );
        } catch (PaymentInstrumentException e) {
            fail();
        }
    }

    @Test
    void enrollInstrument_ko_consent() {
        Mockito.when(paymentInstrumentRepositoryMock.findByHpan(HPAN)).thenReturn(new ArrayList<>());
        
        Mockito.when(decryptRestConnector.getPiiByToken(USER_ID)).thenReturn(DECRYPT_CF_DTO);
        Mockito.when(
                pmRestClientConnector.getWalletList(USER_ID)).thenReturn(WALLET_V_2_LIST_RESPONSE_PBD_KO);
        
        try {
            paymentInstrumentService.enrollInstrument(INITIATIVE_ID, USER_ID, ID_WALLET, CHANNEL, PaymentInstrumentConstants.INSTRUMENT_TYPE_CARD);
            fail();
        } catch (PaymentInstrumentException e) {
            assertEquals(HttpStatus.NOT_FOUND.value(), e.getCode());
        }
    }
    

    
    @Test
    void enrollInstrument_ok_satispay() {
        List<PaymentInstrument> listFailed = new ArrayList<>();
        listFailed.add(TEST_ENROLLMENT_FAILED_KO_RE);
        Mockito.when(paymentInstrumentRepositoryMock.findByHpan(UUID)).thenReturn(listFailed);
        
        Mockito.when(decryptRestConnector.getPiiByToken(USER_ID)).thenReturn(DECRYPT_CF_DTO);
        Mockito.when(
                pmRestClientConnector.getWalletList(USER_ID)).thenReturn(WALLET_V_2_LIST_RESPONSE_SATISPAY);
        
        try {
            paymentInstrumentService.enrollInstrument(INITIATIVE_ID, USER_ID, ID_WALLET, CHANNEL, PaymentInstrumentConstants.INSTRUMENT_TYPE_CARD
            );
        } catch (PaymentInstrumentException e) {
            fail();
        }
    }
    
    @Test
    void enrollInstrument_ok_bpay() {
        Mockito.when(paymentInstrumentRepositoryMock.findByHpan(UUID)).thenReturn(List.of(TEST_ENROLLMENT_FAILED_KO_RE));
        
        Mockito.when(decryptRestConnector.getPiiByToken(USER_ID)).thenReturn(DECRYPT_CF_DTO);
        Mockito.when(
                pmRestClientConnector.getWalletList(USER_ID)).thenReturn(WALLET_V_2_LIST_RESPONSE_BPAY);
        
        try {
            paymentInstrumentService.enrollInstrument(INITIATIVE_ID_OTHER, USER_ID, ID_WALLET, CHANNEL, PaymentInstrumentConstants.INSTRUMENT_TYPE_CARD
            );
        } catch (PaymentInstrumentException e) {
            fail();
        }
    }
    
    @Test
    void enrollInstrument_ok_other_initiative() {
        Mockito.when(paymentInstrumentRepositoryMock.findByHpan(HPAN)).thenReturn(List.of(TEST_PENDING_ENROLLMENT_INSTRUMENT));
        
        Mockito.when(decryptRestConnector.getPiiByToken(USER_ID)).thenReturn(DECRYPT_CF_DTO);
        Mockito.when(
                pmRestClientConnector.getWalletList(USER_ID)).thenReturn(WALLET_V_2_LIST_RESPONSE_CARD);
        
        try {
            paymentInstrumentService.enrollInstrument(INITIATIVE_ID, USER_ID, ID_WALLET, CHANNEL, PaymentInstrumentConstants.INSTRUMENT_TYPE_CARD
            );
        } catch (PaymentInstrumentException e) {
            fail();
        }
        assertEquals(ID_WALLET, TEST_INSTRUMENT.getIdWallet());
    }
    
    @Test
    void enrollInstrument_pm_ko() {
        Mockito.when(paymentInstrumentRepositoryMock.findByHpan(HPAN)).thenReturn(new ArrayList<>());
        
        Mockito.when(paymentInstrumentRepositoryMock.countByHpanAndStatusIn(HPAN,
                List.of(PaymentInstrumentConstants.STATUS_ACTIVE,
                        PaymentInstrumentConstants.STATUS_PENDING_DEACTIVATION_REQUEST))).thenReturn(0);
        Mockito.when(decryptRestConnector.getPiiByToken(USER_ID)).thenReturn(DECRYPT_CF_DTO);
        Mockito.when(
                pmRestClientConnector.getWalletList(USER_ID)).thenReturn(WALLET_V_2_LIST_RESPONSE_CARD);
        
        try {
            paymentInstrumentService.enrollInstrument(INITIATIVE_ID_OTHER, USER_ID, ID_WALLET_KO, CHANNEL, PaymentInstrumentConstants.INSTRUMENT_TYPE_CARD
            );
            Assertions.fail();
        } catch (PaymentInstrumentException e) {
            assertEquals(HttpStatus.NOT_FOUND.value(), e.getCode());
        }
    }
    
    @Test
    void enrollInstrumentFailed_ok() {
        Mockito.when(paymentInstrumentRepositoryMock.findByHpan(HPAN)).thenReturn(List.of(TEST_ENROLLMENT_FAILED));
        when(paymentInstrumentRepositoryMock.save(any())).thenReturn(mock(PaymentInstrument.class));
        doNothing().when(rtdProducer).sendInstrument(any());
        Mockito.when(decryptRestConnector.getPiiByToken(USER_ID)).thenReturn(DECRYPT_CF_DTO);
        Mockito.when(
                pmRestClientConnector.getWalletList(USER_ID)).thenReturn(WALLET_V_2_LIST_RESPONSE_CARD);
        paymentInstrumentService.enrollInstrument(INITIATIVE_ID, USER_ID, ID_WALLET, CHANNEL, PaymentInstrumentConstants.INSTRUMENT_TYPE_CARD);
        verify(rtdProducer).sendInstrument(any());
    }
    
    @Test
    void enrollInstrumentFailed_ko() {
        Mockito.when(paymentInstrumentRepositoryMock.findByHpan(HPAN)).thenReturn(List.of(TEST_ENROLLMENT_FAILED));
        doThrow(new PaymentInstrumentException(HttpStatus.BAD_REQUEST.value(), ""))
                .when(rtdProducer).sendInstrument(any());
        Mockito.when(decryptRestConnector.getPiiByToken(USER_ID)).thenReturn(DECRYPT_CF_DTO);
        Mockito.when(
                pmRestClientConnector.getWalletList(USER_ID)).thenReturn(WALLET_V_2_LIST_RESPONSE_CARD);
        doNothing().when(paymentInstrumentRepositoryMock).delete(any());
        try {
            paymentInstrumentService.enrollInstrument(INITIATIVE_ID, USER_ID, ID_WALLET, CHANNEL, PaymentInstrumentConstants.INSTRUMENT_TYPE_CARD);
        } catch (PaymentInstrumentException e) {
            assertEquals(HttpStatus.BAD_REQUEST.value(), e.getCode());
        }
    }
    
    @Test
    void idWallet_ko() {
        Mockito.when(paymentInstrumentRepositoryMock.findByHpan(HPAN)).thenReturn(new ArrayList<>());
        
        Mockito.when(decryptRestConnector.getPiiByToken(USER_ID)).thenReturn(DECRYPT_CF_DTO);
        
        Request request =
                Request.create(Request.HttpMethod.GET, "url", new HashMap<>(), null, new RequestTemplate());
        
        Mockito.doThrow(new FeignException.BadRequest("", request, new byte[0], null))
                .when(pmRestClientConnector).getWalletList(USER_ID);
        
        try {
            paymentInstrumentService.enrollInstrument(INITIATIVE_ID_OTHER, USER_ID, ID_WALLET, CHANNEL, PaymentInstrumentConstants.INSTRUMENT_TYPE_CARD
            );
            Assertions.fail();
        } catch (PaymentInstrumentException e) {
            assertEquals(HttpStatus.BAD_REQUEST.value(), e.getCode());
        }
    }
    
    @Test
    void enrollInstrument_ko_already_associated() {
        Mockito.when(paymentInstrumentRepositoryMock.findByHpan(HPAN)).thenReturn(List.of(TEST_INACTIVE_INSTRUMENT));
        
        Mockito.when(decryptRestConnector.getPiiByToken(USER_ID_FAIL)).thenReturn(DECRYPT_CF_DTO);
        
        Mockito.when(
                pmRestClientConnector.getWalletList(USER_ID)).thenReturn(
                WALLET_V_2_LIST_RESPONSE_CARD);
        
        try {
            paymentInstrumentService.enrollInstrument(INITIATIVE_ID, USER_ID_FAIL, ID_WALLET, CHANNEL, PaymentInstrumentConstants.INSTRUMENT_TYPE_CARD
            );
        } catch (PaymentInstrumentException e) {
            assertEquals(HttpStatus.FORBIDDEN.value(), e.getCode());
            assertEquals(PaymentInstrumentConstants.ERROR_PAYMENT_INSTRUMENT_ALREADY_ASSOCIATED,
                    e.getMessage());
        }
    }
    
    @Test
    void deactiveInstrument_ko_rule_engine() {
        Mockito.when(
                        paymentInstrumentRepositoryMock.findByInitiativeIdAndUserIdAndId(INITIATIVE_ID,
                                USER_ID, INSTRUMENT_ID))
                .thenReturn(Optional.of(TEST_INSTRUMENT_ACTIVE));
        
        Mockito.when(paymentInstrumentRepositoryMock.countByHpanAndStatusIn(HPAN,
                List.of(PaymentInstrumentConstants.STATUS_ACTIVE,
                        PaymentInstrumentConstants.STATUS_PENDING_DEACTIVATION_REQUEST))).thenReturn(0);
        
        Mockito.doThrow(new PaymentInstrumentException(400, "")).when(producer)
                .sendInstruments(Mockito.any());
        
        try {
            paymentInstrumentService.deactivateInstrument(INITIATIVE_ID, USER_ID, INSTRUMENT_ID);
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
            paymentInstrumentService.enrollInstrument(INITIATIVE_ID, USER_ID, ID_WALLET, CHANNEL, PaymentInstrumentConstants.INSTRUMENT_TYPE_CARD
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
        
        Mockito.when(ackMapper.ackToWallet(Mockito.any(), Mockito.any(), Mockito.anyString(), Mockito.anyString(),Mockito.anyString(), Mockito.anyString(), Mockito.anyInt())).thenReturn(TEST_INSTRUMENT_ACK_DTO);
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
        Mockito.when(ackMapper.ackToWallet(Mockito.any(),Mockito.any(),Mockito.anyString(),Mockito.anyString(),Mockito.anyString(),Mockito.anyString(),Mockito.anyInt())).thenReturn(TEST_INSTRUMENT_ACK_DTO);
        final RuleEngineAckDTO dto = new RuleEngineAckDTO(INITIATIVE_ID, USER_ID,
                PaymentInstrumentConstants.OPERATION_ADD, List.of(), List.of(HPAN), LocalDateTime.now());
        

        Mockito.when(
                        paymentInstrumentRepositoryMock.findByInitiativeIdAndUserIdAndHpanAndStatus(INITIATIVE_ID,
                                USER_ID, HPAN, PaymentInstrumentConstants.STATUS_PENDING_RE))
                .thenReturn(Optional.of(TEST_PENDING_ENROLLMENT_INSTRUMENT));
        
        paymentInstrumentService.processAck(dto);
        
        assertEquals(PaymentInstrumentConstants.STATUS_ENROLLMENT_FAILED_KO_RE,
                TEST_PENDING_ENROLLMENT_INSTRUMENT.getStatus());
        assertEquals(PaymentInstrumentConstants.INSTRUMENT_TYPE_CARD,
            TEST_PENDING_ENROLLMENT_INSTRUMENT.getInstrumentType());
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
            paymentInstrumentService.enrollInstrument(INITIATIVE_ID, USER_ID, ID_WALLET, CHANNEL, PaymentInstrumentConstants.INSTRUMENT_TYPE_CARD
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
                .instrumentType(PaymentInstrumentConstants.INSTRUMENT_TYPE_CARD)
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
                .instrumentType(PaymentInstrumentConstants.INSTRUMENT_TYPE_CARD)
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
                .instrumentType(PaymentInstrumentConstants.INSTRUMENT_TYPE_CARD)
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
            assertEquals(INSTRUMENT.getInstrumentType(), actual.getInstrumentType());
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
    void disableAllPayInstrument_emptyList() {

        Mockito.when(
                        paymentInstrumentRepositoryMock.findByInitiativeIdAndUserIdAndStatus(INITIATIVE_ID, USER_ID,
                                PaymentInstrumentConstants.STATUS_ACTIVE))
                .thenReturn(Collections.emptyList());

        paymentInstrumentService.deactivateAllInstruments(INITIATIVE_ID, USER_ID,
                LocalDateTime.now().toString());
        Mockito.verify(rewardCalculatorConnector,never()).disableUserInitiativeInstruments(Mockito.anyString(),Mockito.anyString());
    }
    @Test
    void disableAllPayInstrument_ok_channel_IDPAY_PAYMENT() {

        List<PaymentInstrument> paymentInstruments = new ArrayList<>();
        paymentInstruments.add(TEST_INSTRUMENT);
        TEST_INSTRUMENT.setChannel(PaymentInstrumentConstants.IDPAY_PAYMENT);

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
    void disableAllPayInstrument_ko() {
        
        List<PaymentInstrument> paymentInstruments = new ArrayList<>();
        paymentInstruments.add(TEST_INSTRUMENT);
        
        Mockito.doThrow(new PaymentInstrumentException(400, "error")).when(rewardCalculatorConnector).disableUserInitiativeInstruments(
                anyString(),anyString());
        
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

        Mockito.when(
                        paymentInstrumentRepositoryMock.findByInitiativeIdAndUserIdAndStatus(INITIATIVE_ID, USER_ID,
                                PaymentInstrumentConstants.STATUS_INACTIVE))
                .thenReturn(paymentInstruments);
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
        
        Mockito.when(messageMapper.apply(Mockito.any(RuleEngineRequestDTO.class))).thenReturn(
                MessageBuilder.withPayload(new RuleEngineRequestDTO()).build());
        
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
        TEST_INSTRUMENT.setStatus(PaymentInstrumentConstants.STATUS_INACTIVE);
        TEST_INSTRUMENT.setDeactivationDate(LocalDateTime.now());
        Mockito.when(
                        paymentInstrumentRepositoryMock.findByInitiativeIdAndUserIdAndStatus(INITIATIVE_ID, USER_ID,
                                PaymentInstrumentConstants.STATUS_INACTIVE))
                .thenReturn(paymentInstrumentList);
        paymentInstrumentService.rollback(INITIATIVE_ID,USER_ID);
        assertNull(TEST_INSTRUMENT.getDeactivationDate());
        assertNotEquals(PaymentInstrumentConstants.STATUS_INACTIVE, TEST_INSTRUMENT.getStatus());
        Mockito.verify(rewardCalculatorConnector, Mockito.times(1)).enableUserInitiativeInstruments(anyString(),anyString());
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
                        paymentInstrumentRepositoryMock.findByInitiativeIdAndUserIdAndChannelAndStatusIn(
                                INITIATIVE_ID,
                                USER_ID, CHANNEL, List.of(PaymentInstrumentConstants.STATUS_ACTIVE,
                                                PaymentInstrumentConstants.STATUS_PENDING_RTD,
                                                PaymentInstrumentConstants.STATUS_PENDING_RE,
                                                PaymentInstrumentConstants.STATUS_PENDING_DEACTIVATION_REQUEST)))
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
                        paymentInstrumentRepositoryMock.findByInitiativeIdAndUserIdAndChannelAndStatusIn(
                                INITIATIVE_ID,
                                USER_ID, CHANNEL, List.of(PaymentInstrumentConstants.STATUS_ACTIVE,
                                                PaymentInstrumentConstants.STATUS_PENDING_RTD,
                                                PaymentInstrumentConstants.STATUS_PENDING_RE,
                                                PaymentInstrumentConstants.STATUS_PENDING_DEACTIVATION_REQUEST)))
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
                        paymentInstrumentRepositoryMock.findByInitiativeIdAndUserIdAndChannelAndStatusIn(
                                INITIATIVE_ID,
                                USER_ID, CHANNEL, List.of(PaymentInstrumentConstants.STATUS_ACTIVE,
                                                PaymentInstrumentConstants.STATUS_PENDING_RTD,
                                                PaymentInstrumentConstants.STATUS_PENDING_RE,
                                                PaymentInstrumentConstants.STATUS_PENDING_DEACTIVATION_REQUEST)))
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
        
        final InstrumentIssuerDTO dto = new InstrumentIssuerDTO(INITIATIVE_ID, USER_ID, HPAN, CHANNEL, PaymentInstrumentConstants.INSTRUMENT_TYPE_CARD,
                "", "", "");
        
        Mockito.when(paymentInstrumentRepositoryMock.findByHpan(HPAN)).thenReturn(new ArrayList<>());
        
        try {
            paymentInstrumentService.enrollFromIssuer(dto);
        } catch (PaymentInstrumentException e) {
            fail();
        }
    }
    
    @Test
    void enrollIssuer_ok_idemp() {
        final InstrumentIssuerDTO dto = new InstrumentIssuerDTO(INITIATIVE_ID, USER_ID, HPAN, CHANNEL, PaymentInstrumentConstants.INSTRUMENT_TYPE_CARD,
                "", "", "");
        Mockito.when(paymentInstrumentRepositoryMock.findByHpan(HPAN)).thenReturn(List.of(TEST_INSTRUMENT));
        
        try {
            paymentInstrumentService.enrollFromIssuer(dto);
        } catch (PaymentInstrumentException e) {
            Assertions.fail();
        }
    }
    
    @Test
    void enrollIssuer_ok_other_initiative() {
        final InstrumentIssuerDTO dto = new InstrumentIssuerDTO(INITIATIVE_ID_OTHER, USER_ID, HPAN, CHANNEL, PaymentInstrumentConstants.INSTRUMENT_TYPE_CARD,
                "", "","");
        Mockito.when(paymentInstrumentRepositoryMock.findByHpan(HPAN)).thenReturn(List.of(TEST_INSTRUMENT));
        
        try {
            paymentInstrumentService.enrollFromIssuer(dto);
        } catch (PaymentInstrumentException e) {
            fail();
        }
        assertEquals(HPAN, TEST_INSTRUMENT.getHpan());
    }

    @Test
    void enrollIssuer_ok_other_initiative_status_inactive() {
        final InstrumentIssuerDTO dto = new InstrumentIssuerDTO(INITIATIVE_ID, USER_ID, HPAN, CHANNEL, PaymentInstrumentConstants.INSTRUMENT_TYPE_CARD,
                "", "", "");
        Mockito.when(paymentInstrumentRepositoryMock.findByHpan(HPAN)).thenReturn(List.of(TEST_INACTIVE_INSTRUMENT));

        try {
            paymentInstrumentService.enrollFromIssuer(dto);
        } catch (PaymentInstrumentException e) {
            Assertions.fail();
        }
    }

    @Test
    void enrollIssuer_ok_status_faild_ko_re() {
        final InstrumentIssuerDTO dto = new InstrumentIssuerDTO(INITIATIVE_ID, USER_ID, HPAN, CHANNEL, PaymentInstrumentConstants.INSTRUMENT_TYPE_CARD,
                "", "", "");
        Mockito.when(paymentInstrumentRepositoryMock.findByHpan(HPAN)).thenReturn(List.of(TEST_ENROLLMENT_FAILED_KO_RE));

        try {
            paymentInstrumentService.enrollFromIssuer(dto);
        } catch (PaymentInstrumentException e) {
            Assertions.fail();
        }
    }
    
    @Test
    void enrollIssuer_ko_already_associated() {
        final InstrumentIssuerDTO dto = new InstrumentIssuerDTO(INITIATIVE_ID, USER_ID_FAIL, HPAN, CHANNEL, PaymentInstrumentConstants.INSTRUMENT_TYPE_CARD, "", "", "");
        Mockito.when(paymentInstrumentRepositoryMock.findByHpan(HPAN)).thenReturn(List.of(TEST_INSTRUMENT));
        
        try {
            paymentInstrumentService.enrollFromIssuer(dto);
        } catch (PaymentInstrumentException e) {
            assertEquals(HttpStatus.FORBIDDEN.value(), e.getCode());
            assertEquals(PaymentInstrumentConstants.ERROR_PAYMENT_INSTRUMENT_ALREADY_ASSOCIATED,
                    e.getMessage());
        }
    }
    
    @Test
    void enrollIssuer_ko_rule_engine() {
        final InstrumentIssuerDTO dto = new InstrumentIssuerDTO(INITIATIVE_ID, USER_ID, HPAN, CHANNEL, PaymentInstrumentConstants.INSTRUMENT_TYPE_CARD,
                "", "", "");
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

    @Test
    void getInstrumentInitiativesDetail_withFilterStatus(){
        List<PaymentInstrument> instrumentList = new ArrayList<>();
        instrumentList.add(TEST_INSTRUMENT_ACTIVE);
        instrumentList.add(TEST_PENDING_ENROLLMENT_INSTRUMENT);
        instrumentList.add(TEST_PENDING_DEACTIVATION_INSTRUMENT);
        instrumentList.add(TEST_INACTIVE_INSTRUMENT);
        instrumentList.add(TEST_ENROLLMENT_FAILED_KO_RE);
        instrumentList.add(TEST_INSTRUMENT_PENDING_RTD);

        List<String> statusList = List.of(PaymentInstrumentConstants.STATUS_ACTIVE,
                PaymentInstrumentConstants.STATUS_PENDING_RE,
                PaymentInstrumentConstants.STATUS_PENDING_RTD);

        Mockito.when(paymentInstrumentRepositoryMock.findByIdWalletAndUserId(ID_WALLET, USER_ID)).thenReturn(instrumentList);

        InstrumentDetailDTO instrumentDetailDTO = paymentInstrumentService.getInstrumentInitiativesDetail(ID_WALLET, USER_ID, statusList);
        assertEquals(MASKED_PAN, instrumentDetailDTO.getMaskedPan());
        assertEquals(BRAND, instrumentDetailDTO.getBrand());
        assertEquals(3, instrumentDetailDTO.getInitiativeList().size());
        List<String> expectedStatusList = List.of(PaymentInstrumentConstants.STATUS_ACTIVE, PaymentInstrumentConstants.STATUS_PENDING_ENROLLMENT_REQUEST);
        assertTrue(instrumentDetailDTO.getInitiativeList().stream().allMatch(initiative ->
                expectedStatusList.contains(initiative.getStatus())));
    }
    @Test
    void getInstrumentInitiativesDetail_noInstrumentFound(){
        Mockito.when(paymentInstrumentRepositoryMock.findByIdWalletAndUserId(ID_WALLET, USER_ID)).thenReturn(new ArrayList<>());
        Mockito.when(decryptRestConnector.getPiiByToken(USER_ID)).thenReturn(DECRYPT_CF_DTO);
        Mockito.when(pmRestClientConnector.getWalletList(USER_ID)).thenReturn(WALLET_V_2_LIST_RESPONSE_CARD);

        InstrumentDetailDTO instrumentDetailDTO = paymentInstrumentService.getInstrumentInitiativesDetail(ID_WALLET, USER_ID, new ArrayList<>());

        assertEquals(BLURRED_NUMBER, instrumentDetailDTO.getMaskedPan());
        assertEquals(BRAND, instrumentDetailDTO.getBrand());
        assertEquals(0, instrumentDetailDTO.getInitiativeList().size());
    }
    @Test
    void getInstrumentInitiativesDetail_noFilterStatus(){
        List<PaymentInstrument> instrumentList = new ArrayList<>();
        instrumentList.add(TEST_INSTRUMENT_ACTIVE);
        instrumentList.add(TEST_ENROLLMENT_FAILED);

        Mockito.when(paymentInstrumentRepositoryMock.findByIdWalletAndUserId(ID_WALLET, USER_ID)).thenReturn(instrumentList);
        Mockito.when(decryptRestConnector.getPiiByToken(USER_ID)).thenReturn(DECRYPT_CF_DTO);
        Mockito.when(pmRestClientConnector.getWalletList(USER_ID)).thenReturn(WALLET_V_2_LIST_RESPONSE_CARD);

        InstrumentDetailDTO instrumentDetailDTO = paymentInstrumentService.getInstrumentInitiativesDetail(ID_WALLET, USER_ID, null);

        assertEquals(MASKED_PAN, instrumentDetailDTO.getMaskedPan());
        assertEquals(BRAND, instrumentDetailDTO.getBrand());
        assertEquals(2, instrumentDetailDTO.getInitiativeList().size());
        assertEquals(PaymentInstrumentConstants.STATUS_ACTIVE, instrumentDetailDTO.getInitiativeList().get(0).getStatus());
    }

    @ParameterizedTest
    @MethodSource("operationTypeAndInvocationTimes")
    void processOperation(String operationType, int times) {
        // Given
        final QueueCommandOperationDTO queueCommandOperationDTO = QueueCommandOperationDTO.builder()
                .entityId(INITIATIVE_ID)
                .operationType(operationType)
                .operationTime(LocalDateTime.now().minusMinutes(5))
                .build();
        PaymentInstrument paymentInstrument = PaymentInstrument.builder()
                .id(INSTRUMENT_ID)
                .initiativeId(INITIATIVE_ID)
                .build();
        final List<PaymentInstrument> deletedPage = List.of(paymentInstrument);

        if(times == 2){
            final List<PaymentInstrument> instrumentsPage = createPaymentInstrumentPage(Integer.parseInt(pagination));
            when(paymentInstrumentRepositoryExtended.deletePaged(queueCommandOperationDTO.getEntityId(), Integer.parseInt(pagination)))
                    .thenReturn(instrumentsPage)
                    .thenReturn(deletedPage);
        } else {
            when(paymentInstrumentRepositoryExtended.deletePaged(queueCommandOperationDTO.getEntityId(), Integer.parseInt(pagination)))
                    .thenReturn(deletedPage);
        }

        // When
        if(times == 1){
            Thread.currentThread().interrupt();
        }
        paymentInstrumentService.processOperation(queueCommandOperationDTO);

        // Then
        Mockito.verify(paymentInstrumentRepositoryExtended, Mockito.times(times)).deletePaged(queueCommandOperationDTO.getEntityId(), Integer.parseInt(pagination));
    }

    private static Stream<Arguments> operationTypeAndInvocationTimes() {
        return Stream.of(
                Arguments.of(OPERATION_TYPE_DELETE_INITIATIVE, 1),
                Arguments.of(OPERATION_TYPE_DELETE_INITIATIVE, 2),
                Arguments.of("OPERATION_TYPE_TEST", 0)
        );
    }

    private List<PaymentInstrument> createPaymentInstrumentPage(int pageSize){
        List<PaymentInstrument> paymentInstrumentsPage = new ArrayList<>();

        for(int i=0;i<pageSize; i++){
            paymentInstrumentsPage.add(PaymentInstrument.builder()
                    .id(INSTRUMENT_ID+i)
                    .initiativeId(INITIATIVE_ID)
                    .build());
        }

        return paymentInstrumentsPage;
    }
}