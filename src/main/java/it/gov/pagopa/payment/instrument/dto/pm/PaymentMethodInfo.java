package it.gov.pagopa.payment.instrument.dto.pm;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

@Data
@AllArgsConstructor
@ToString
public class PaymentMethodInfo {
  String blurredNumber;
  String brand;
  String brandLogo;
  String expireMonth;
  String expireYear;
  String hashPan;
  String holder;
  String type;
  String bankName;
  String instituteCode;
  String numberObfuscated;
  List<BPayPaymentInstrumentWallet> paymentInstruments;
  String uidHash;
  String uuid;

  public static class BPayPaymentInstrumentWallet{
    Boolean defaultReceive;
    Boolean defaultSend;
  }
}
