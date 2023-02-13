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

    private static final String CEF = String.format("CEF:0|PagoPa|IDPAY|1.0|7|User interaction|2|vs=172.16.151.21:80 event=PaymentInstrument srcip=%s srcport=17548 dstip=172.16.128.37 dstport=82",
            SRCIP);
    private static final String MSG = " msg=";
    private static final String ID_WALLET = "idWallet=";
    private static final String HPAN = "hpan=";
    private static final String DATE = "date=";
    private static final String CHANNEL = "channel=";
    final Logger logger = Logger.getLogger("AUDIT");


    private String buildLog(String eventLog, String idWallet, String hpan, String channel) {
        String logWallet = "";
        if (idWallet != null){
            logWallet = ID_WALLET + idWallet;
        }
        return CEF + MSG + eventLog + " " + logWallet + " " + HPAN + hpan + " " + CHANNEL + channel;
    }

    private String buildLogWithDate(String eventLog, String idWallet, String hpan, String channel,LocalDateTime date) {
        return buildLog(eventLog, idWallet, hpan, channel) + " " + DATE + date;
    }

    public void logEnrollInstrumentKO(String msg, String idWallet, String hpan, String channel) {
        String testLog = this.buildLog("Enrollment of the instrument failed: " + msg, idWallet, hpan, channel);
        logger.info(testLog);
    }

    public void logEnrollInstrFromIssuerKO(String msg, String hpan, String channel) {
        String testLog = this.buildLog("Enrollment of the instrument from Issuer failed: " + msg, null, hpan, channel);
        logger.info(testLog);
    }

    public void logEnrollInstrumentComplete(String idWallet, String hpan, String channel) {
        String testLog = this.buildLog("Enrollment of the instrument completed: " , idWallet, hpan, channel);
        logger.info(testLog);
    }

    public void logEnrollInstrFromIssuerComplete(String hpan, String channel) {
        String testLog = this.buildLog("Enrollment of the instrument from Issuer completed: ", null, hpan, channel);
        logger.info(testLog);
    }

    public void logDeactivationComplete(String idWallet, String hpan, String channel, LocalDateTime date) {
        String testLog = this.buildLogWithDate("Deactivation of the Payment Instrument completed", idWallet, hpan, channel, date);
        logger.info(testLog);
    }

    public void logDeactivationKO(String idWallet, String hpan, String channel, LocalDateTime date) {
        String testLog = this.buildLogWithDate("Deactivation of the Payment Instrument failed", idWallet, hpan, channel, date);
        logger.info(testLog);
    }

    public void logAckEnrollComplete(String idWallet, String hpan, String channel, LocalDateTime date) {
        String testLog = this.buildLogWithDate("Activation of the Payment Instrument completed", idWallet, hpan, channel, date);
        logger.info(testLog);
    }

    public void logAckEnrollKO(String idWallet, String hpan, String channel, LocalDateTime date) {
        String testLog = this.buildLogWithDate("Activation of the Payment Instrument failed", idWallet, hpan, channel, date);
        logger.info(testLog);
    }
}
