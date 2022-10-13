package it.gov.pagopa.payment.instrument.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class WalletCallDTO {

  private List<WalletDTO> walletDTOlist;
}
