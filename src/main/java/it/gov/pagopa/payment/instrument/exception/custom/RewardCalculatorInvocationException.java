package it.gov.pagopa.payment.instrument.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;

import static it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants.ExceptionCode.GENERIC_ERROR;

public class RewardCalculatorInvocationException extends ServiceException {


    public RewardCalculatorInvocationException(String message) {
        this(GENERIC_ERROR, message);
    }

    public RewardCalculatorInvocationException(String code, String message) {
        this(code, message, false, null);
    }

    public RewardCalculatorInvocationException(String code, String message, boolean printStackTrace, Throwable ex) {
        super(code, message, printStackTrace, ex);
    }
}
