package it.gov.pagopa.payment.instrument.utils;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import it.gov.pagopa.payment.instrument.exception.PaymentInstrumentException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@ContextConfiguration(classes = {Utilities.class,InetAddress.class})
class UtilitiesTest {
    private static final String SRCIP;

    static {
        try {
            SRCIP = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            throw new PaymentInstrumentException(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        }
    }

    private static final String MSG = " TEST_MSG";
    private static final String ID_WALLET  = "TEST_ID_WALLET";
    private static final String HPAN = "TEST_HPAN";
    private static final LocalDateTime DATE = LocalDateTime.now();
    private static final String CHANNEL = "CHANNEL";

    @MockBean
    Logger logger;
    @Autowired
    Utilities utilities;
    @MockBean
    InetAddress inetAddress;
    MemoryAppender memoryAppender;

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
            utilities.logEnrollInstrumentKO(MSG, ID_WALLET, CHANNEL);
        assertThat(memoryAppender.contains(ch.qos.logback.classic.Level.DEBUG,MSG)).isFalse();
    }

    @Test
    void logEnrollInstrFromIssuerKO_ok(){
        utilities.logEnrollInstrFromIssuerKO(MSG, HPAN, CHANNEL);
        assertThat(memoryAppender.contains(ch.qos.logback.classic.Level.DEBUG,MSG)).isFalse();
    }

    @Test
    void logEnrollInstrumentComplete_ok(){
        utilities.logEnrollInstrumentComplete(ID_WALLET, CHANNEL);
        assertThat(memoryAppender.contains(ch.qos.logback.classic.Level.DEBUG,MSG)).isFalse();
    }

    @Test
    void logEnrollInstrFromIssuerComplete_ok(){
        utilities.logEnrollInstrFromIssuerComplete(HPAN, CHANNEL);
        assertThat(memoryAppender.contains(ch.qos.logback.classic.Level.DEBUG,MSG)).isFalse();
    }

    @Test
    void logDeactivationComplete_ok(){
        utilities.logDeactivationComplete(ID_WALLET, CHANNEL, DATE);
        assertThat(memoryAppender.contains(ch.qos.logback.classic.Level.DEBUG,MSG)).isFalse();
    }

    @Test
    void logDeactivationKO_ok(){
        utilities.logDeactivationKO(ID_WALLET, CHANNEL, DATE);
        assertThat(memoryAppender.contains(ch.qos.logback.classic.Level.DEBUG,MSG)).isFalse();
    }

    @Test
    void logAckEnrollComplete_ok(){
        utilities.logAckEnrollComplete(ID_WALLET, CHANNEL, DATE);
        assertThat(memoryAppender.contains(ch.qos.logback.classic.Level.DEBUG,MSG)).isFalse();
    }

    @Test
    void logAckEnrollKO_ok(){
        utilities.logAckEnrollKO(ID_WALLET, CHANNEL, DATE);
        assertThat(memoryAppender.contains(ch.qos.logback.classic.Level.DEBUG,MSG)).isFalse();
    }

    public static class MemoryAppender extends ListAppender<ILoggingEvent> {
        public void reset() {
            this.list.clear();
        }

        public boolean contains(ch.qos.logback.classic.Level level, String string) {
            return this.list.stream()
                    .anyMatch(event -> event.toString().contains(string)
                            && event.getLevel().equals(level));
        }

        public int countEventsForLogger(String loggerName) {
            return (int) this.list.stream()
                    .filter(event -> event.getLoggerName().contains(loggerName))
                    .count();
        }

        public List<ILoggingEvent> search() {
            return this.list.stream()
                    .filter(event -> event.toString().contains(MSG))
                    .collect(Collectors.toList());
        }

        public List<ILoggingEvent> search(Level level) {
            return this.list.stream()
                    .filter(event -> event.toString().contains(MSG)
                            && event.getLevel().equals(level))
                    .collect(Collectors.toList());
        }

        public int getSize() {
            return this.list.size();
        }

        public List<ILoggingEvent> getLoggedEvents() {
            return Collections.unmodifiableList(this.list);
        }
    }

}
