package it.gov.pagopa.payment.instrument.service.idpaycode;

public interface EncryptCodeService {

  String buildHashedPinBlock(String code, String secondFactor, String salt);
  String encryptWithAzureAPI(String hashedPinBlock);

}
