package it.gov.pagopa.payment.instrument.dto.pm;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WalletV2ListResponse {

  List<WalletV2> data;
}
