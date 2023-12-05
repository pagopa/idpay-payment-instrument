package it.gov.pagopa.payment.instrument.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;

import static it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants.ExceptionCode.USER_NOT_ONBOARDED;

public class UserNotOnboardedException extends ServiceException {


    public UserNotOnboardedException(String message) {
        this(USER_NOT_ONBOARDED, message);
    }

    public UserNotOnboardedException(String code, String message) {
        this(code, message, false, null);
    }

    public UserNotOnboardedException(String code, String message, boolean printStackTrace, Throwable ex) {
        super(code, message, printStackTrace, ex);
    }
}
