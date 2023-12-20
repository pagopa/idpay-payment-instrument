package it.gov.pagopa.payment.instrument.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.common.web.exception.ServiceExceptionResponse;

public class IdpayCodeEncryptOrDecryptException extends ServiceException {

    public IdpayCodeEncryptOrDecryptException(String code, String message, Throwable ex) {
        this(code, message,null, false, ex);
    }

    public IdpayCodeEncryptOrDecryptException(String code, String message, ServiceExceptionResponse response, boolean printStackTrace, Throwable ex) {
        super(code, message, response, printStackTrace, ex);
    }
}
