package it.gov.pagopa.payment.instrument.config;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.payment.instrument.exception.custom.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class ServiceExceptionConfig {

    @Bean
    public Map<Class<? extends ServiceException>, HttpStatus> serviceExceptionMapper() {
        Map<Class<? extends ServiceException>, HttpStatus> exceptionMap = new HashMap<>();

        // BadRequest
        exceptionMap.put(PinBlockSizeException.class, HttpStatus.BAD_REQUEST);

        // Forbidden
        exceptionMap.put(UserNotAllowedException.class, HttpStatus.FORBIDDEN);
        exceptionMap.put(InstrumentDeleteNotAllowedException.class, HttpStatus.FORBIDDEN);
        exceptionMap.put(IdpayCodeEncryptOrDecryptException.class, HttpStatus.FORBIDDEN);
        exceptionMap.put(InitiativeInvalidException.class, HttpStatus.FORBIDDEN);
        exceptionMap.put(UserUnsubscribedException.class, HttpStatus.FORBIDDEN);


        // NotFound
        exceptionMap.put(UserNotOnboardedException.class, HttpStatus.NOT_FOUND);
        exceptionMap.put(PaymentInstrumentNotFoundException.class, HttpStatus.NOT_FOUND);
        exceptionMap.put(IDPayCodeNotFoundException.class, HttpStatus.NOT_FOUND);

        // InternalServerError
        exceptionMap.put(RewardCalculatorInvocationException.class, HttpStatus.INTERNAL_SERVER_ERROR);
        exceptionMap.put(WalletInvocationException.class, HttpStatus.INTERNAL_SERVER_ERROR);
        exceptionMap.put(PMInvocationException.class, HttpStatus.INTERNAL_SERVER_ERROR);
        exceptionMap.put(PDVInvocationException.class, HttpStatus.INTERNAL_SERVER_ERROR);
        exceptionMap.put(PinBlockException.class, HttpStatus.INTERNAL_SERVER_ERROR);
        exceptionMap.put(InternalServerErrorException.class, HttpStatus.INTERNAL_SERVER_ERROR);
        return exceptionMap;
    }
}
