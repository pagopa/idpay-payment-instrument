package it.gov.pagopa.payment.instrument.service.idpaycode;

import it.gov.pagopa.payment.instrument.dto.GenerateCodeRespDTO;
import it.gov.pagopa.payment.instrument.dto.PinBlockDTO;

public interface PaymentInstrumentCodeService {

  GenerateCodeRespDTO generateCode(String userId, String initiativeId);

  boolean codeStatus(String userId);

  boolean verifyPinBlock(String userId, PinBlockDTO pinBlockDTO);

  String getSecondFactor(String userId);

}
