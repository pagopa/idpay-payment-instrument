package it.gov.pagopa.payment.instrument.utils;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;

@Component
@AllArgsConstructor
@Slf4j(topic = "AUDIT")
public class AuditUtilities {
    public static final String SRCIP;

    static {
        String srcIp;
        try {
            srcIp = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            log.error("Cannot determine the ip of the current host", e);
            srcIp="UNKNOWN";
        }

        SRCIP = srcIp;
    }

    private static final String CEF = String.format("CEF:0|PagoPa|IDPAY|1.0|7|User interaction|2| event=PaymentInstrument dstip=%s", SRCIP);
    private static final String CEF_WALLET_PATTERN = CEF + " msg={} cs1Label=idWallet cs1={} cs3Label=channel cs3={}";
    private static final String CEF_HPAN_PATTERN = CEF + " msg={} cs2Label=hpan cs2={} cs3Label=channel cs3={}";
    private static final String CEF_WALLET_PATTERN_DATE = CEF_WALLET_PATTERN + " cs4Label=date cs4={}";

    private void logAuditString(String pattern, String... parameters) {
        log.info(pattern, (Object[]) parameters);
    }

    public void logEnrollInstrumentKO(String msg, String idWallet, String channel) {
        logAuditString(
                CEF_WALLET_PATTERN,
                "Enrollment of the instrument failed: " + msg, idWallet, channel
        );
    }

    public void logEnrollInstrFromIssuerKO(String msg, String hpan, String channel) {
        logAuditString(
                CEF_HPAN_PATTERN,
                "Enrollment of the instrument from Issuer failed: " + msg, hpan, channel
        );
    }

    public void logEnrollInstrumentComplete(String idWallet, String channel) {
        logAuditString(
                CEF_WALLET_PATTERN,
                "Enrollment of the instrument completed.", idWallet, channel
        );
    }

    public void logEnrollInstrFromIssuerComplete(String hpan, String channel) {
        logAuditString(
                CEF_HPAN_PATTERN,
                "Enrollment of the instrument from Issuer completed.", hpan, channel
        );
    }

    public void logDeactivationComplete(String idWallet, String channel, LocalDateTime date) {
        logAuditString(
                CEF_WALLET_PATTERN_DATE,
                "Deactivation of the instrument completed.", idWallet, channel, date.toString()
        );
    }

    public void logDeactivationKO(String idWallet, String channel, LocalDateTime date) {
        logAuditString(
                CEF_WALLET_PATTERN_DATE,
                "Deactivation of the instrument failed.", idWallet, channel, date.toString()
        );
    }

    public void logAckEnrollComplete(String idWallet, String channel, LocalDateTime date) {
        logAuditString(
                CEF_WALLET_PATTERN_DATE,
                "Activation of the instrument completed.", idWallet, channel, date.toString()
        );
    }

    public void logAckEnrollKO(String idWallet, String channel, LocalDateTime date) {
        logAuditString(
                CEF_WALLET_PATTERN_DATE,
                "Activation of the instrument failed.", idWallet, channel, date.toString()
        );
    }
}
