package it.gov.pagopa.payment.instrument.service.idpaycode;

import it.gov.pagopa.payment.instrument.dto.EncryptedDataBlock;
import it.gov.pagopa.payment.instrument.dto.PinBlockDTO;

public interface IdpayCodeEncryptionService {

  byte[] buildHashedDataBlock(String code, String secondFactor, String salt);
  byte[] createSHA256Digest(byte[] dataBlock, String salt);
  byte[] hashSHADecryptedDataBlock(String userId, PinBlockDTO pinBlockDTO, String salt);
  EncryptedDataBlock encryptSHADataBlock(byte[] dataBlock);
  byte[] decryptSymmetricKey(String symmetricKey);
  byte[] decryptIdpayCode(EncryptedDataBlock encryptedDataBlock);

}
