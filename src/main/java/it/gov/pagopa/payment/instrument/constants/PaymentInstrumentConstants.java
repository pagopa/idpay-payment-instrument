package it.gov.pagopa.payment.instrument.constants;

public final class PaymentInstrumentConstants {

  public static final String STATUS_INACTIVE = "INACTIVE";
  public static final String STATUS_ACTIVE = "ACTIVE";
  public static final String OPERATION_ADD = "ADD_INSTRUMENT";
  public static final String OPERATION_DELETE = "DELETE_INSTRUMENT";
  public static final String ERROR_MANDATORY_FIELD = "The field is mandatory!";
  public static final String ERROR_PAYMENT_INSTRUMENT_NOT_FOUND = "The selected payment instrument is not active for such user and initiative.";
  public static final String ERROR_PAYMENT_INSTRUMENT_ALREADY_ACTIVE = "Payment instrument already in use by another citizen";

  public static final String ERROR_INITIATIVE_USER = "It doesn't exist payment instrument for such initiative and user";
  public static final String ERROR_MSG_HEADER_SRC_TYPE = "srcType";
  public static final String ERROR_MSG_HEADER_SRC_SERVER = "srcServer";
  public static final String ERROR_MSG_HEADER_SRC_TOPIC = "srcTopic";
  public static final String ERROR_MSG_HEADER_DESCRIPTION = "description";
  public static final String ERROR_MSG_HEADER_RETRYABLE = "retryable";
  public static final String ERROR_MSG_HEADER_STACKTRACE = "stacktrace";
  public static final String ERROR_MSG_HEADER_CLASS = "rootCauseClass";
  public static final String ERROR_MSG_HEADER_MESSAGE = "rootCauseMessage";
  public static final String TOPIC_RTD = "rtd-enrolled-pi";
  public static final String KAFKA = "kafka";
  public static final String BROKER_RTD = "cstar-d-evh-ns.servicebus.windows.net:9093";
  public static final String ERROR_RTD = "error to ADD new instrument to RTD";
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
