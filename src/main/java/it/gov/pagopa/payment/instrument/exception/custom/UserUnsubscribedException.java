package it.gov.pagopa.payment.instrument.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.common.web.exception.ServiceExceptionResponse;

import static it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants.ExceptionCode.USER_UNSUBSCRIBED;


public class UserUnsubscribedException extends ServiceException {

    public UserUnsubscribedException(String message) {
        this(USER_UNSUBSCRIBED, message);
    }

    public UserUnsubscribedException(String code, String message) {
        this(code, message, null, false, null);
    }

    public UserUnsubscribedException(String code, String message, ServiceExceptionResponse response, boolean printStackTrace, Throwable ex) {
        super(code, message, response, printStackTrace, ex);
    }
}
