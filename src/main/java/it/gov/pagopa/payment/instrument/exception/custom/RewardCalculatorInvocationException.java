package it.gov.pagopa.payment.instrument.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.common.web.exception.ServiceExceptionPayload;

import static it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants.ExceptionCode.GENERIC_ERROR;

public class RewardCalculatorInvocationException extends ServiceException {


    public RewardCalculatorInvocationException(String message, boolean printStackTrace, Throwable ex) {
        this(GENERIC_ERROR, message,null,printStackTrace,ex);
    }
    public RewardCalculatorInvocationException(String code, String message, ServiceExceptionPayload payload, boolean printStackTrace, Throwable ex) {
        super(code, message, payload, printStackTrace, ex);
    }
}
