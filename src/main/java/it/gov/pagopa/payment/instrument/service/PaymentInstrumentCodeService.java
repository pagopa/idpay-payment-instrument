package it.gov.pagopa.payment.instrument.service;

import it.gov.pagopa.payment.instrument.dto.GenerateCodeReqDTO;
import it.gov.pagopa.payment.instrument.dto.GenerateCodeRespDTO;

public interface PaymentInstrumentCodeService {

  GenerateCodeRespDTO generateCode(String userId, GenerateCodeReqDTO body);

}
