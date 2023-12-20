package it.gov.pagopa.payment.instrument.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.common.web.exception.ServiceExceptionResponse;

import static it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants.ExceptionCode.INITIATIVE_ENDED;

public class InitiativeInvalidException extends ServiceException {


    public InitiativeInvalidException(String message) {
        this(INITIATIVE_ENDED, message);
    }

    public InitiativeInvalidException(String code, String message) {
        this(code, message, null, false, null);
    }

    public InitiativeInvalidException(String code, String message, ServiceExceptionResponse response, boolean printStackTrace, Throwable ex) {
        super(code, message, response, printStackTrace, ex);
    }
}
