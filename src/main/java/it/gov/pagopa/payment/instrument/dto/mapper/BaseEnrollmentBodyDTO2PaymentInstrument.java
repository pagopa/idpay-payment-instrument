package it.gov.pagopa.payment.instrument.dto.mapper;

import it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants;
import it.gov.pagopa.payment.instrument.dto.BaseEnrollmentBodyDTO;
import it.gov.pagopa.payment.instrument.model.PaymentInstrument;
import org.springframework.stereotype.Service;

import java.util.function.BiFunction;

@Service
public class BaseEnrollmentBodyDTO2PaymentInstrument implements
        BiFunction<BaseEnrollmentBodyDTO, String, PaymentInstrument> {

  @Override
  public PaymentInstrument apply(BaseEnrollmentBodyDTO baseEnrollmentBodyDTO, String hpan) {
    return PaymentInstrument.builder()
            .initiativeId(baseEnrollmentBodyDTO.getInitiativeId())
            .userId(baseEnrollmentBodyDTO.getUserId())
            .consent(true)
            .hpan(hpan)
            .channel(baseEnrollmentBodyDTO.getChannel())
            .instrumentType(baseEnrollmentBodyDTO.getInstrumentType())
            .status(PaymentInstrumentConstants.STATUS_PENDING_RE)
            .build();
  }
}
