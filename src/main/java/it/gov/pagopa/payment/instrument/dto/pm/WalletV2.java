package it.gov.pagopa.payment.instrument.dto.pm;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class WalletV2 {

  String createDate;
  List<String> enableableFunctions;
  Boolean favourite;
  String idWallet;
  String onboardingChannel;
  String updateDate;
  String walletType;
  PaymentMethodInfo info;

}
