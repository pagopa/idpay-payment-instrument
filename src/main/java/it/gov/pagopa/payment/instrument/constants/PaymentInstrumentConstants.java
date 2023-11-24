package it.gov.pagopa.payment.instrument.constants;

public final class PaymentInstrumentConstants {

  public static final String STATUS_INACTIVE = "INACTIVE";
  public static final String STATUS_ACTIVE = "ACTIVE";
  public static final String REGEX_PENDING_ENROLL = "^PENDING_ENROLL_";
  public static final String STATUS_PENDING_RTD = "PENDING_ENROLL_RTD";
  public static final String STATUS_PENDING_RE = "PENDING_ENROLL_RE";
  public static final String STATUS_ENROLLMENT_FAILED = "ENROLLMENT_FAILED";
  public static final String STATUS_ENROLLMENT_FAILED_KO_RE = "ENROLLMENT_FAILED_KO_RE";
  public static final String OPERATION_ADD = "ADD_INSTRUMENT";
  public static final String OPERATION_DELETE = "DELETE_INSTRUMENT";
  public static final String ERROR_MANDATORY_FIELD = "The field is mandatory!";
  public static final String ERROR_PAYMENT_INSTRUMENT_NOT_FOUND = "The selected payment instrument is not active for such user and initiative.";
  public static final String ERROR_PAYMENT_INSTRUMENT_ALREADY_ASSOCIATED_AUDIT = "Payment instrument already associated to another user";

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
  public static final String ID_PAY = "ID_PAY";
  public static final String REJECTED = "REJECTED_";
  public static final String IDPAY_PAYMENT = "IDPAY_PAYMENT";
  public static final String IDPAY_PAYMENT_FAKE_INSTRUMENT_PREFIX = "IDPAY_%s";
  public static final String IDPAY_CODE_FAKE_INSTRUMENT_PREFIX = "IDPAYCODE_%s";
  public static final String OPERATION_TYPE_DELETE_INITIATIVE = "DELETE_INITIATIVE";

  //region instrument type
  public static final String INSTRUMENT_TYPE_CARD = "CARD";
  public static final String INSTRUMENT_TYPE_APP_IO_PAYMENT = "APP_IO_PAYMENT";
  public static final String INSTRUMENT_TYPE_IDPAYCODE = "IDPAYCODE";
  //endregion

  public static final class ExceptionCode {
    public static final String PIN_LENGTH_NOT_VALID = "PAYMENT_INSTRUMENT_PIN_LENGTH_NOT_VALID";
    public static final String INVALID_REQUEST = "PAYMENT_INSTRUMENT_INVALID_REQUEST";
    public static final String INSTRUMENT_ALREADY_ASSOCIATED = "PAYMENT_INSTRUMENT_ALREADY_ASSOCIATED";
    public static final String DELETE_NOT_ALLOWED = "PAYMENT_INSTRUMENT_DELETE_NOT_ALLOWED";
    public static final String ENCRYPTION_ERROR = "PAYMENT_INSTRUMENT_ENCRYPTION_ERROR";
    public static final String DECRYPTION_ERROR = "PAYMENT_INSTRUMENT_DECRYPTION_ERROR";
    public static final String ENROLL_NOT_ALLOWED_FOR_REFUND_INITIATIVE = "PAYMENT_INSTRUMENT_ENROLL_NOT_ALLOWED_FOR_REFUND_INITIATIVE";
    public static final String INITIATIVE_ENDED = "PAYMENT_INSTRUMENT_INITIATIVE_ENDED";
    public static final String USER_UNSUBSCRIBED = "PAYMENT_INSTRUMENT_USER_UNSUBSCRIBED";
    public static final String INSTRUMENT_NOT_FOUND = "PAYMENT_INSTRUMENT_NOT_FOUND";
    public static final String IDPAYCODE_NOT_FOUND = "PAYMENT_INSTRUMENT_IDPAYCODE_NOT_FOUND";
    public static final String USER_NOT_ONBOARDED = "PAYMENT_INSTRUMENT_USER_NOT_ONBOARDED";
    public static final String TOO_MANY_REQUESTS = "PAYMENT_INSTRUMENT_TOO_MANY_REQUESTS";
    public static final String GENERIC_ERROR = "PAYMENT_INSTRUMENT_GENERIC_ERROR";
  }

  public static final class ExceptionMessage {
    public static final String ERROR_PIN_LENGTH_NOT_VALID_MSG = "Pin length is not valid";
    public static final String ERROR_INSTRUMENT_ALREADY_ASSOCIATED_MSG = "Payment Instrument is already associated to another user";
    public static final String ERROR_DELETE_NOT_ALLOWED_MSG = "It's not possible to delete an instrument of AppIO payment types";
    public static final String ENCRYPTION_ERROR_MSG = "Something went wrong creating SHA256 digest";
    public static final String DECRYPTION_ERROR_MSG = "Something gone wrong while extracting datablock from pinblock";
    public static final String ERROR_ENROLL_NOT_ALLOWED_FOR_REFUND_INITIATIVE_MSG = "It is not possible to enroll a idpayCode for a refund type initiative";
    public static final String ERROR_INITIATIVE_ENDED_MSG = "The operation is not allowed because the initiative [%s] has already ended";
    public static final String ERROR_USER_UNSUBSCRIBED_MSG = "The user has unsubscribed from initiative [%s]";
    public static final String ERROR_INSTRUMENT_NOT_FOUND_MSG = "The selected payment instrument has not been found for the current user";
    public static final String ERROR_IDPAYCODE_NOT_FOUND_MSG = "idpayCode is not found for the current user";
    public static final String ERROR_USER_NOT_ONBOARDED_MSG = "The current user is not onboarded on initiative [%s]";
    public static final String ERROR_TOO_MANY_REQUESTS_WALLET_MSG = "Too many requests on the ms wallet";
    public static final String ERROR_TOO_MANY_REQUESTS_INSTRUMENT_MSG = "Too many requests on the ms Payment Instrument";
    public static final String ERROR_INVOCATION_REWARD_MSG = "An error occurred in the microservice reward-calculator";
    public static final String ERROR_INVOCATION_WALLET_MSG = "An error occurred in the microservice wallet";
    public static final String ERROR_DEACTIVATE_INSTRUMENT_NOTIFY_MSG = "Something gone wrong while deactivate instrument notify";
    public static final String ERROR_SEND_INSTRUMENT_NOTIFY_MSG = "Something gone wrong while send RTD instrument notify";
    public static final String ERROR_INVOCATION_PDV_ENCRYPT_MSG= "An error occurred during encrypt";
    public static final String ERROR_INVOCATION_PDV_DECRYPT_MSG= "An error occurred during decrypt";
    public static final String ERROR_INVOCATION_PM_MSG= "An error occurred during call PM service";
    public static final String ERROR_CREATING_PINBLOCK_MSG= "Something went wrong while creating pinBlock";

  }
  private PaymentInstrumentConstants() {
  }
}
