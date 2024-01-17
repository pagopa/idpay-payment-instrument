package it.gov.pagopa.payment.instrument.config;

import it.gov.pagopa.common.web.dto.ErrorDTO;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants.ExceptionCode;
@Configuration
public class PaymentInstrumentErrorManagerConfig {
    @Bean
    ErrorDTO defaultErrorDTO() {
        return new ErrorDTO(
                ExceptionCode.GENERIC_ERROR,
                "A generic error occurred"
        );
    }
    @Bean
    ErrorDTO tooManyRequestsErrorDTO() {
        return new ErrorDTO(ExceptionCode.TOO_MANY_REQUESTS, "Too Many Requests");
    }
    @Bean
    ErrorDTO templateValidationErrorDTO(){
        return new ErrorDTO(ExceptionCode.INVALID_REQUEST, null);
    }
}