package it.gov.pagopa.payment.instrument.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.common.web.exception.ServiceExceptionPayload;

import static it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants.ExceptionCode.GENERIC_ERROR;

public class PMInvocationException extends ServiceException {


    public PMInvocationException(String message) {
        this(GENERIC_ERROR, message);
    }

    public PMInvocationException(String code, String message) {
        this(code, message, null, false, null);
    }

    public PMInvocationException(String message, boolean printStackTrace, Throwable ex) {
        super(GENERIC_ERROR, message, null, printStackTrace, ex);
    }
    public PMInvocationException(String code, String message, ServiceExceptionPayload payload, boolean printStackTrace, Throwable ex) {
        super(code, message, payload, printStackTrace, ex);
    }
}