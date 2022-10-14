package it.gov.pagopa.payment.instrument.dto.pm;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaymentMethodInfo {
  String blurredNumber;
  String brand;
  String brandLogo;
  String expireMonth;
  String expireYear;
  String hashPan;
  String holder;
  List<String> htokenList;
  String issuerAbiCode;
  CardType type;
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
  public enum CardType{
    PP,
    DEB,
    CRD,
    PRV
  }
}