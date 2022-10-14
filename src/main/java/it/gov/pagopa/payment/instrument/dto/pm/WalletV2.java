package it.gov.pagopa.payment.instrument.dto.pm;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WalletV2 {

  String createDate;
  List<String> enableableFunctions;
  Boolean favourite;
  String idWallet;
  String onboardingChannel;
  String updateDate;
  WalletType walletType;
  PaymentMethodInfo info;

}
