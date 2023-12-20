package it.gov.pagopa.payment.instrument.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.common.web.exception.ServiceExceptionResponse;

import static it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants.ExceptionCode.GENERIC_ERROR;

public class PinBlockException extends ServiceException {


    public PinBlockException(String message) {
        this(GENERIC_ERROR, message);
    }

    public PinBlockException(String code, String message) {
        this(code, message, null, false, null);
    }

    public PinBlockException(String code, String message, ServiceExceptionResponse response, boolean printStackTrace, Throwable ex) {
        super(code, message, response,printStackTrace, ex);
    }
}
