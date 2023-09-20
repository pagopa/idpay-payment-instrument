package it.gov.pagopa.payment.instrument.service;

import it.gov.pagopa.payment.instrument.dto.GenerateCodeDTO;
import it.gov.pagopa.payment.instrument.dto.GeneratedCodeDTO;

public interface PaymentInstrumentCodeService {

  GeneratedCodeDTO generateCode(String userId, GenerateCodeDTO body);

}
