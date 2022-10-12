package it.gov.pagopa.payment.instrument.dto.pm;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WalletV2 {
  @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
  LocalDateTime createDate;
  List<String> enableableFunctions;
  Boolean favourite;
  String idWallet;
  String onboardingChannel;
  @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")



  LocalDateTime updateDate;
  WalletType walletType;
  PaymentMethodInfo info;

}
