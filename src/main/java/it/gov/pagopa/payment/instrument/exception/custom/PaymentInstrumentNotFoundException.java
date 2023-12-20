package it.gov.pagopa.payment.instrument.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.common.web.exception.ServiceExceptionResponse;

import static it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants.ExceptionCode.INSTRUMENT_NOT_FOUND;

public class PaymentInstrumentNotFoundException extends ServiceException {


    public PaymentInstrumentNotFoundException(String message) {
        this(INSTRUMENT_NOT_FOUND, message);
    }

    public PaymentInstrumentNotFoundException(String code, String message) {
        this(code, message, null, false, null);
    }

    public PaymentInstrumentNotFoundException(String code, String message, ServiceExceptionResponse response, boolean printStackTrace, Throwable ex) {
        super(code, message, response, printStackTrace, ex);
    }
}
