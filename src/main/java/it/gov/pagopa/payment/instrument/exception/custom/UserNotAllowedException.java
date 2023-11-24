package it.gov.pagopa.payment.instrument.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;

import static it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants.ExceptionCode.INSTRUMENT_ALREADY_ASSOCIATED;

public class UserNotAllowedException extends ServiceException {


    public UserNotAllowedException(String message) {
        this(INSTRUMENT_ALREADY_ASSOCIATED, message);
    }

    public UserNotAllowedException(String code, String message) {
        this(code, message, false, null);
    }

    public UserNotAllowedException(String code, String message, boolean printStackTrace, Throwable ex) {
        super(code, message, printStackTrace, ex);
    }
}
