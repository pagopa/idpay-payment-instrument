package it.gov.pagopa.payment.instrument.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.common.web.exception.ServiceExceptionPayload;

import static it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants.ExceptionCode.IDPAYCODE_NOT_FOUND;

public class IDPayCodeNotFoundException extends ServiceException {


    public IDPayCodeNotFoundException(String message) {
        this(IDPAYCODE_NOT_FOUND, message);
    }

    public IDPayCodeNotFoundException(String code, String message) {
        this(code, message, null, false, null);
    }

    public IDPayCodeNotFoundException(String code, String message, ServiceExceptionPayload payload, boolean printStackTrace, Throwable ex) {
        super(code, message, payload, printStackTrace, ex);
    }
}
