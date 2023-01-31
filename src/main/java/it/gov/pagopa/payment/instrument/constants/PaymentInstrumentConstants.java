package it.gov.pagopa.payment.instrument.constants;

public final class PaymentInstrumentConstants {

  public static final String STATUS_INACTIVE = "INACTIVE";
  public static final String STATUS_ACTIVE = "ACTIVE";
  public static final String REGEX_PENDING_ENROLL = "^PENDING_ENROLL_";
  public static final String STATUS_PENDING_RTD = "PENDING_ENROLL_RTD";
  public static final String STATUS_PENDING_RE = "PENDING_ENROLL_RE";
  public static final String STATUS_ENROLLMENT_FAILED = "ENROLLMENT_FAILED";
  public static final String OPERATION_ADD = "ADD_INSTRUMENT";
  public static final String OPERATION_DELETE = "DELETE_INSTRUMENT";
  public static final String ERROR_MANDATORY_FIELD = "The field is mandatory!";
  public static final String ERROR_PAYMENT_INSTRUMENT_NOT_FOUND = "The selected payment instrument is not active for such user and initiative.";
  public static final String ERROR_PAYMENT_INSTRUMENT_ALREADY_ACTIVE = "Payment instrument already in use by another citizen";

  public static final String ERROR_MSG_HEADER_SRC_TYPE = "srcType";
  public static final String ERROR_MSG_HEADER_SRC_SERVER = "srcServer";
  public static final String ERROR_MSG_HEADER_SRC_TOPIC = "srcTopic";
  public static final String ERROR_MSG_HEADER_DESCRIPTION = "description";
  public static final String ERROR_MSG_HEADER_RETRYABLE = "retryable";
  public static final String ERROR_MSG_HEADER_STACKTRACE = "stacktrace";
  public static final String ERROR_MSG_HEADER_CLASS = "rootCauseClass";
  public static final String ERROR_MSG_HEADER_MESSAGE = "rootCauseMessage";
  public static final String KAFKA = "kafka";
  public static final String ERROR_QUEUE = "Error sending message to queue";
  public static final String PM="PAYMENT_MANAGER";
  public static final String IO="APP_IO";
  public static final String SATISPAY = "Satispay";
  public static final String BPAY = "BPay";
  public static final String BPD = "BPD";
  public static final String STATUS_PENDING_ENROLLMENT_REQUEST = "PENDING_ENROLLMENT_REQUEST";
  public static final String STATUS_PENDING_DEACTIVATION_REQUEST = "PENDING_DEACTIVATION_REQUEST";
  public static final String STATUS_FAILED_ENROLLMENT_REQUEST = "INACTIVE_FAILED_ENROLLMENT_REQUEST";
  public static final String ID_PAY = "ID_PAY";

  private PaymentInstrumentConstants() {
  }
}
