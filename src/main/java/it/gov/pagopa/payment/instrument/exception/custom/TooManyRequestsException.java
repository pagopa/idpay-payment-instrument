package it.gov.pagopa.payment.instrument.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;

import static it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants.ExceptionCode.TOO_MANY_REQUESTS;

public class TooManyRequestsException extends ServiceException {


    public TooManyRequestsException(String message) {
        this(TOO_MANY_REQUESTS, message);
    }

    public TooManyRequestsException(String code, String message) {
        this(code, message, false, null);
    }

    public TooManyRequestsException(String code, String message, boolean printStackTrace, Throwable ex) {
        super(code, message, printStackTrace, ex);
    }
}
