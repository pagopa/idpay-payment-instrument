package it.gov.pagopa.payment.instrument.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.common.web.exception.ServiceExceptionPayload;

import static it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants.ExceptionCode.USER_UNSUBSCRIBED;


public class UserUnsubscribedException extends ServiceException {

    public UserUnsubscribedException(String message, boolean printStackTrace, Throwable ex) {
        this(USER_UNSUBSCRIBED, message, null, printStackTrace,ex);
    }

    public UserUnsubscribedException(String code, String message, ServiceExceptionPayload payload, boolean printStackTrace, Throwable ex) {
        super(code, message, payload, printStackTrace, ex);
    }
}
