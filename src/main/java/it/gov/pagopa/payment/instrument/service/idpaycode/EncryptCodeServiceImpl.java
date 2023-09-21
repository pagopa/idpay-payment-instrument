package it.gov.pagopa.payment.instrument.service.idpaycode;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EncryptCodeServiceImpl implements EncryptCodeService {

  public EncryptCodeServiceImpl() {
    // TODO to be implemented
  }

  @Override
  public String encryptIdpayCode(String code) {
    return code;
  }
}
