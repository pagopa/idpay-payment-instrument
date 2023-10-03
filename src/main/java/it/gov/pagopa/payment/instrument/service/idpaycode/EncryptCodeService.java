package it.gov.pagopa.payment.instrument.service.idpaycode;

public interface EncryptCodeService {

  String encryptIdpayCode(String code, String secondFactor, String salt);

}
