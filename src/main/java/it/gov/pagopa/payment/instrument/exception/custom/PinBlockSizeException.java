package it.gov.pagopa.payment.instrument.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;

import static it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants.ExceptionCode.PIN_LENGTH_NOT_VALID;

public class PinBlockSizeException extends ServiceException {


    public PinBlockSizeException(String message) {
        this(PIN_LENGTH_NOT_VALID, message);
    }

    public PinBlockSizeException(String code, String message) {
        this(code, message, false, null);
    }

    public PinBlockSizeException(String code, String message, boolean printStackTrace, Throwable ex) {
        super(code, message, printStackTrace, ex);
    }
}
