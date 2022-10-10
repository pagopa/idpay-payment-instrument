package it.gov.pagopa.payment.instrument.dto.pm;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BPayInfo {
  String bankName;
  String brandLogo;
  String instituteCode;
  String numberObfuscated;
  List<BPayPaymentInstrumentWallet> paymentInstruments;
  String uidHash;

  public static class BPayPaymentInstrumentWallet{
    Boolean defaultReceive;
    Boolean defaultSend;
  }
}
