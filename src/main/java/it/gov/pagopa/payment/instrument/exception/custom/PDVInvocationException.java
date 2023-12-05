package it.gov.pagopa.payment.instrument.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;

import static it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants.ExceptionCode.GENERIC_ERROR;

public class PDVInvocationException extends ServiceException {


    public PDVInvocationException(String message) {
        this(GENERIC_ERROR, message);
    }

    public PDVInvocationException(String code, String message) {
        this(code, message, false, null);
    }

    public PDVInvocationException(String code, String message, boolean printStackTrace, Throwable ex) {
        super(code, message, printStackTrace, ex);
    }
}
