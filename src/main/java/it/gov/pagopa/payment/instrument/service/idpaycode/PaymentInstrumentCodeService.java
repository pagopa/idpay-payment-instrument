package it.gov.pagopa.payment.instrument.service.idpaycode;

import it.gov.pagopa.payment.instrument.dto.GenerateCodeRespDTO;

public interface PaymentInstrumentCodeService {

  GenerateCodeRespDTO generateCode(String userId, String initiativeId);

  boolean codeStatus(String userId);

}
