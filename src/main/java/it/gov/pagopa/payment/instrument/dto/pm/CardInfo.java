package it.gov.pagopa.payment.instrument.dto.pm;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CardInfo {
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
  public enum CardType{
    PP,
    DEB,
    CRD,
    PRV
  }
}
