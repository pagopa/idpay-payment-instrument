package it.gov.pagopa.payment.instrument.dto.pm;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum WalletType {
  CARD,
  BANCOMAT,
  SATISPAY,
  BPAY
}
