package it.gov.pagopa.payment.instrument.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;

import static it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants.ExceptionCode.GENERIC_ERROR;

public class InternalServerErrorException extends ServiceException {

    public InternalServerErrorException(String message) {
        this(GENERIC_ERROR, message);
    }

    public InternalServerErrorException(String code, String message) {
        this(code, message, false, null);
    }

    public InternalServerErrorException(String code, String message, boolean printStackTrace, Throwable ex) {
        super(code, message, printStackTrace, ex);
    }
}
