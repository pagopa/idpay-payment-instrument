package it.gov.pagopa.payment.instrument.constants;

public final class PaymentInstrumentConstants {

  public static final String STATUS_INACTIVE = "INACTIVE";
  public static final String STATUS_ACTIVE = "ACTIVE";
  public static final String ERROR_MANDATORY_FIELD = "The field is mandatory!";
  public static final String ERROR_PAYMENT_INSTRUMENT_NOT_FOUND = "The selected payment instrument is not active for such user and initiative.";
  public static final String ERROR_PAYMENT_INSTRUMENT_ALREADY_ACTIVE = "Payment instrument already in use by another citizen";

  public static final String ERROR_INITIATIVE_USER = "It doesn't exist payment instrument for such initiative and user";

  private PaymentInstrumentConstants() {
  }
}
