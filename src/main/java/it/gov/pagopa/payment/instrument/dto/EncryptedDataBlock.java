package it.gov.pagopa.payment.instrument.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EncryptedDataBlock {

  private String encryptedDataBlock;
  private String keyId;

}
