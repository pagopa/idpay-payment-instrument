package it.gov.pagopa.payment.instrument.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class WalletCallDTO {

  private List<WalletDTO> walletDTOlist;
}
