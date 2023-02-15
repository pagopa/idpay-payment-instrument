package it.gov.pagopa.payment.instrument.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.logging.Logger;

import it.gov.pagopa.payment.instrument.exception.PaymentInstrumentException;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class Utilities {
    private static final String SRCIP;

    static {
        try {
            SRCIP = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            throw new PaymentInstrumentException(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        }
    }

    private static final String CEF = String.format("CEF:0|PagoPa|IDPAY|1.0|7|User interaction|2| event=PaymentInstrument dstip=%s",
            SRCIP);
    private static final String MSG = " msg=";
    private static final String ID_WALLET = "cs1Label=idWallet cs1=";
    private static final String HPAN = "cs2Label=hpan cs2=";
    private static final String DATE = "cs3Label=date cs3=";
    private static final String CHANNEL = "cs4Label=channel cs4=";
    final Logger logger = Logger.getLogger("AUDIT");


    private String buildLogForIssuer(String eventLog, String hpan, String channel) {
        return CEF + MSG + eventLog + " " + HPAN + hpan + " " + CHANNEL + channel;
    }

    private String buildLog(String eventLog, String idWallet, String channel) {
        return CEF + MSG + eventLog + " " + ID_WALLET + idWallet + " " + CHANNEL + channel;
    }

    private String buildLogWithDate(String eventLog, String idWallet, String channel,LocalDateTime date) {
        return buildLog(eventLog, idWallet, channel) + " " + DATE + date;
    }

    public void logEnrollInstrumentKO(String msg, String idWallet, String channel) {
        String testLog = this.buildLog("Enrollment of the instrument failed: " + msg, idWallet, channel);
        logger.info(testLog);
    }

    public void logEnrollInstrFromIssuerKO(String msg, String hpan, String channel) {
        String testLog = this.buildLogForIssuer("Enrollment of the instrument from Issuer failed: " + msg, hpan, channel);
        logger.info(testLog);
    }

    public void logEnrollInstrumentComplete(String idWallet, String channel) {
        String testLog = this.buildLogForIssuer("Enrollment of the instrument completed: " , idWallet, channel);
        logger.info(testLog);
    }

    public void logEnrollInstrFromIssuerComplete(String hpan, String channel) {
        String testLog = this.buildLogForIssuer("Enrollment of the instrument from Issuer completed: ", hpan, channel);
        logger.info(testLog);
    }

    public void logDeactivationComplete(String idWallet, String channel, LocalDateTime date) {
        String testLog = this.buildLogWithDate("Deactivation of the Payment Instrument completed", idWallet, channel, date);
        logger.info(testLog);
    }

    public void logDeactivationKO(String idWallet, String channel, LocalDateTime date) {
        String testLog = this.buildLogWithDate("Deactivation of the Payment Instrument failed", idWallet, channel, date);
        logger.info(testLog);
    }

    public void logAckEnrollComplete(String idWallet, String channel, LocalDateTime date) {
        String testLog = this.buildLogWithDate("Activation of the Payment Instrument completed", idWallet, channel, date);
        logger.info(testLog);
    }

    public void logAckEnrollKO(String idWallet, String channel, LocalDateTime date) {
        String testLog = this.buildLogWithDate("Activation of the Payment Instrument failed", idWallet, channel, date);
        logger.info(testLog);
    }
}
