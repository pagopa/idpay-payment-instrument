package it.gov.pagopa.payment.instrument.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.common.web.exception.ServiceExceptionPayload;

public class IdpayCodeEncryptOrDecryptException extends ServiceException {

    public IdpayCodeEncryptOrDecryptException(String code, String message,boolean printStackTrace, Throwable ex) {
        this(code, message,null, printStackTrace, ex);
    }

    public IdpayCodeEncryptOrDecryptException(String code, String message, ServiceExceptionPayload payload, boolean printStackTrace, Throwable ex) {
        super(code, message, payload, printStackTrace, ex);
    }
}
