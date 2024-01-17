package it.gov.pagopa.payment.instrument.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.common.web.exception.ServiceExceptionPayload;

import static it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants.ExceptionCode.GENERIC_ERROR;

public class PDVInvocationException extends ServiceException {

     public PDVInvocationException(String message) {
        this(GENERIC_ERROR, message, null, false, null);
    }

    public PDVInvocationException(String message, boolean printStackTrace, Throwable ex) {
        this(GENERIC_ERROR, message,null,printStackTrace,ex);
    }

    public PDVInvocationException(String code, String message, ServiceExceptionPayload payload, boolean printStackTrace, Throwable ex) {
        super(code, message, payload, printStackTrace, ex);
    }
}
