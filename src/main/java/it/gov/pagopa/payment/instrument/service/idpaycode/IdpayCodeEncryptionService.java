package it.gov.pagopa.payment.instrument.service.idpaycode;

import it.gov.pagopa.payment.instrument.dto.PinBlockDTO;

public interface IdpayCodeEncryptionService {

  String buildHashedDataBlock(String code, String secondFactor, String salt);
  String createSHA256Digest(String dataBlock, String salt);
  String verifyPinBlock(String userId, PinBlockDTO pinBlockDTO, String salt);
  String encryptSHADataBlock(String dataBlock);
  String decryptSymmetricKey(String symmetricKey);

}
