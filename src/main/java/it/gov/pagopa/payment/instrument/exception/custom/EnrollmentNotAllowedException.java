package it.gov.pagopa.payment.instrument.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.common.web.exception.ServiceExceptionResponse;

import static it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants.ExceptionCode.ENROLL_NOT_ALLOWED_FOR_REFUND_INITIATIVE;

public class EnrollmentNotAllowedException  extends ServiceException {


    public EnrollmentNotAllowedException(String message) {
        this(ENROLL_NOT_ALLOWED_FOR_REFUND_INITIATIVE, message);
    }

    public EnrollmentNotAllowedException(String code, String message) {
        this(code, message,null, false, null);
    }

    public EnrollmentNotAllowedException(String code, String message, ServiceExceptionResponse response, boolean printStackTrace, Throwable ex) {
        super(code, message, response, printStackTrace, ex);
    }
}
