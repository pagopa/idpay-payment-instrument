package it.gov.pagopa.payment.instrument.dto.pm;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WalletV2 {

  LocalDateTime createDate;
  List<String> enableableFunctions;
  Boolean favourite;
  String idWallet;
  String onboardingChannel;
  LocalDateTime updateDate;
  WalletType walletType;
  PaymentMethodInfo info;

}
