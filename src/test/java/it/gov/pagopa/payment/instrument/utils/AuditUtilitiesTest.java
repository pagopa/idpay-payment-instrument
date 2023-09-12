package it.gov.pagopa.payment.instrument.utils;

import ch.qos.logback.classic.LoggerContext;
import it.gov.pagopa.common.utils.MemoryAppender;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

class AuditUtilitiesTest {

    private static final String MSG = " TEST_MSG";
    private static final String ID_WALLET  = "TEST_ID_WALLET";
    private static final String HPAN = "TEST_HPAN";
    private static final LocalDateTime DATE = LocalDateTime.now();
    private static final String CHANNEL = "CHANNEL";
    private static final String USER_ID = "TEST_USER_ID";
    private static final String INITIATIVE_ID = "TEST_INITIATIVE_ID";

    private MemoryAppender memoryAppender;

    private final AuditUtilities auditUtilities = new AuditUtilities();

    @BeforeEach
    public void setup() {
        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("AUDIT");
        memoryAppender = new MemoryAppender();
        memoryAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        logger.setLevel(ch.qos.logback.classic.Level.INFO);
        logger.addAppender(memoryAppender);
        memoryAppender.start();
    }

    @Test
    void logEnrollInstrumentKO_ok(){
        auditUtilities.logEnrollInstrumentKO(MSG, ID_WALLET, CHANNEL);

        Assertions.assertEquals(
                ("CEF:0|PagoPa|IDPAY|1.0|7|User interaction|2| event=PaymentInstrument dstip=%s msg=Enrollment of the instrument failed:" +
                        " %s cs1Label=idWallet cs1=%s cs3Label=channel cs3=%s")
                        .formatted(
                                AuditUtilities.SRCIP,
                                MSG,
                                ID_WALLET,
                                CHANNEL
                        ),
                memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
        );
    }
    @Test
    void logEnrollInstrFromIssuerKO_ok(){
        auditUtilities.logEnrollInstrFromIssuerKO(MSG, HPAN, CHANNEL);

        Assertions.assertEquals(
                ("CEF:0|PagoPa|IDPAY|1.0|7|User interaction|2| event=PaymentInstrument dstip=%s msg=Enrollment of the instrument from Issuer failed:" +
                        " %s cs2Label=hpan cs2=%s cs3Label=channel cs3=%s")
                        .formatted(
                                AuditUtilities.SRCIP,
                                MSG,
                                HPAN,
                                CHANNEL
                        ),
                memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
        );
    }
    @Test
    void logEnrollInstrumentComplete_ok(){
        auditUtilities.logEnrollInstrumentComplete(ID_WALLET, CHANNEL);

        Assertions.assertEquals(
                ("CEF:0|PagoPa|IDPAY|1.0|7|User interaction|2| event=PaymentInstrument dstip=%s msg=Enrollment of the instrument completed." +
                        " cs1Label=idWallet cs1=%s cs3Label=channel cs3=%s")
                        .formatted(
                                AuditUtilities.SRCIP,
                                ID_WALLET,
                                CHANNEL
                        ),
                memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
        );
    }
    @Test
    void logEnrollInstrFromIssuerComplete_ok(){
        auditUtilities.logEnrollInstrFromIssuerComplete(HPAN, CHANNEL);

        Assertions.assertEquals(
                ("CEF:0|PagoPa|IDPAY|1.0|7|User interaction|2| event=PaymentInstrument dstip=%s msg=Enrollment of the instrument from Issuer completed." +
                        " cs2Label=hpan cs2=%s cs3Label=channel cs3=%s")
                        .formatted(
                                AuditUtilities.SRCIP,
                                HPAN,
                                CHANNEL
                        ),
                memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
        );
    }
    @Test
    void logDeactivationComplete_ok(){
        auditUtilities.logDeactivationComplete(ID_WALLET, CHANNEL, DATE);

        Assertions.assertEquals(
                ("CEF:0|PagoPa|IDPAY|1.0|7|User interaction|2| event=PaymentInstrument dstip=%s msg=Deactivation of the instrument completed." +
                        " cs1Label=idWallet cs1=%s cs3Label=channel cs3=%s cs4Label=date cs4=%s")
                        .formatted(
                                AuditUtilities.SRCIP,
                                ID_WALLET,
                                CHANNEL,
                                DATE
                        ),
                memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
        );
    }
    @Test
    void logDeactivationKO_ok(){
        auditUtilities.logDeactivationKO(ID_WALLET, CHANNEL, DATE);

        Assertions.assertEquals(
                ("CEF:0|PagoPa|IDPAY|1.0|7|User interaction|2| event=PaymentInstrument dstip=%s msg=Deactivation of the instrument failed." +
                        " cs1Label=idWallet cs1=%s cs3Label=channel cs3=%s cs4Label=date cs4=%s")
                        .formatted(
                                AuditUtilities.SRCIP,
                                ID_WALLET,
                                CHANNEL,
                                DATE
                        ),
                memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
        );
    }
    @Test
    void logAckEnrollComplete_ok(){
        auditUtilities.logAckEnrollComplete(ID_WALLET, CHANNEL, DATE);

        Assertions.assertEquals(
                ("CEF:0|PagoPa|IDPAY|1.0|7|User interaction|2| event=PaymentInstrument dstip=%s msg=Activation of the instrument completed." +
                        " cs1Label=idWallet cs1=%s cs3Label=channel cs3=%s cs4Label=date cs4=%s")
                        .formatted(
                                AuditUtilities.SRCIP,
                                ID_WALLET,
                                CHANNEL,
                                DATE
                        ),
                memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
        );
    }
    @Test
    void logAckEnrollKO_ok(){
        auditUtilities.logAckEnrollKO(ID_WALLET, CHANNEL, DATE);

        Assertions.assertEquals(
                ("CEF:0|PagoPa|IDPAY|1.0|7|User interaction|2| event=PaymentInstrument dstip=%s msg=Activation of the instrument failed." +
                        " cs1Label=idWallet cs1=%s cs3Label=channel cs3=%s cs4Label=date cs4=%s")
                        .formatted(
                                AuditUtilities.SRCIP,
                                ID_WALLET,
                                CHANNEL,
                                DATE
                        ),
                memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
        );
    }
    @Test
    void logDeleteInstrument_ok(){
        auditUtilities.logDeleteInstrument(USER_ID, INITIATIVE_ID);

        Assertions.assertEquals(
                ("CEF:0|PagoPa|IDPAY|1.0|7|User interaction|2| event=PaymentInstrument dstip=%s msg=Payment instruments deleted" +
                        " suser=%s cs1Label=initiativeId cs1=%s")
                        .formatted(
                                AuditUtilities.SRCIP,
                                USER_ID,
                                INITIATIVE_ID
                        ),
                memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
        );
    }
}
