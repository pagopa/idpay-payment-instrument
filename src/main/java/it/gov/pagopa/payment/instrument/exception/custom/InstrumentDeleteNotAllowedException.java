package it.gov.pagopa.payment.instrument.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;

import static it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants.ExceptionCode.DELETE_NOT_ALLOWED;

public class InstrumentDeleteNotAllowedException extends ServiceException {


    public InstrumentDeleteNotAllowedException(String message) {
        this(DELETE_NOT_ALLOWED, message);
    }

    public InstrumentDeleteNotAllowedException(String code, String message) {
        this(code, message, false, null);
    }

    public InstrumentDeleteNotAllowedException(String code, String message, boolean printStackTrace, Throwable ex) {
        super(code, message, printStackTrace, ex);
    }
}
