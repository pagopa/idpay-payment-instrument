package it.gov.pagopa.payment.instrument.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.common.web.exception.ServiceExceptionPayload;

import static it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants.ExceptionCode.DELETE_NOT_ALLOWED;

public class InstrumentDeleteNotAllowedException extends ServiceException {


    public InstrumentDeleteNotAllowedException(String message) {
        this(DELETE_NOT_ALLOWED, message);
    }

    public InstrumentDeleteNotAllowedException(String code, String message) {
        this(code, message, null, false, null);
    }

    public InstrumentDeleteNotAllowedException(String code, String message, ServiceExceptionPayload payload, boolean printStackTrace, Throwable ex) {
        super(code, message, payload, printStackTrace, ex);
    }
}
