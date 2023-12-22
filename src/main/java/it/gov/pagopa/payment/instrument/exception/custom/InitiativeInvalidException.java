package it.gov.pagopa.payment.instrument.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.common.web.exception.ServiceExceptionPayload;

import static it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants.ExceptionCode.INITIATIVE_ENDED;

public class InitiativeInvalidException extends ServiceException {


    public InitiativeInvalidException(String message, boolean printStackTrace, Throwable ex) {
        this(INITIATIVE_ENDED, message, null, printStackTrace,ex);
    }

    public InitiativeInvalidException(String code, String message, ServiceExceptionPayload payload, boolean printStackTrace, Throwable ex) {
        super(code, message, payload, printStackTrace, ex);
    }
}
