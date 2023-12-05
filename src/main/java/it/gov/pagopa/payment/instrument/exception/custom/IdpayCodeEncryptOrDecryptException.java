package it.gov.pagopa.payment.instrument.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;

public class IdpayCodeEncryptOrDecryptException extends ServiceException {

    public IdpayCodeEncryptOrDecryptException(String code, String message, Throwable ex) {
        this(code, message, false, ex);
    }

    public IdpayCodeEncryptOrDecryptException(String code, String message, boolean printStackTrace, Throwable ex) {
        super(code, message, printStackTrace, ex);
    }
}