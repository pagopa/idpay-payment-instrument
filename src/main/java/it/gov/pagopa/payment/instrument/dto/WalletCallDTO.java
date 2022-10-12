package it.gov.pagopa.payment.instrument.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class WalletCallDTO {

  private List<WalletDTO> walletDTOlist;
}
