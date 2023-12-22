package it.gov.pagopa.payment.instrument.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.common.web.exception.ServiceExceptionPayload;

import static it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants.ExceptionCode.ENROLL_NOT_ALLOWED_FOR_REFUND_INITIATIVE;

public class EnrollmentNotAllowedException  extends ServiceException {


    public EnrollmentNotAllowedException(String message ,boolean printStackTrace, Throwable ex) {
        this(ENROLL_NOT_ALLOWED_FOR_REFUND_INITIATIVE, message, null, printStackTrace, ex);
    }

    public EnrollmentNotAllowedException(String code, String message, ServiceExceptionPayload payload, boolean printStackTrace, Throwable ex) {
        super(code, message, payload, printStackTrace, ex);
    }
}
