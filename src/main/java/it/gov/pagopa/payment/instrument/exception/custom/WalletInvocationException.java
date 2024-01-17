package it.gov.pagopa.payment.instrument.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.common.web.exception.ServiceExceptionPayload;

import static it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants.ExceptionCode.GENERIC_ERROR;

public class WalletInvocationException extends ServiceException {


    public WalletInvocationException(String message,boolean printStackTrace, Throwable ex) {
        this(GENERIC_ERROR, message,null,printStackTrace,ex);
    }

    public WalletInvocationException(String code, String message, ServiceExceptionPayload payload, boolean printStackTrace, Throwable ex) {
        super(code, message, payload, printStackTrace, ex);
    }
}
