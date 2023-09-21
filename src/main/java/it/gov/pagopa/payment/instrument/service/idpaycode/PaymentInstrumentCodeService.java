package it.gov.pagopa.payment.instrument.service.idpaycode;

import it.gov.pagopa.payment.instrument.dto.CheckEnrollmentDTO;
import it.gov.pagopa.payment.instrument.dto.GenerateCodeRespDTO;

public interface PaymentInstrumentCodeService {

  GenerateCodeRespDTO generateCode(String userId, String initiativeId);

  CheckEnrollmentDTO codeStatus(String userId);

}
