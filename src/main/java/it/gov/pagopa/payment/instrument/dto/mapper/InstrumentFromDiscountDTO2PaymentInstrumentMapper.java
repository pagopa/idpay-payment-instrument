package it.gov.pagopa.payment.instrument.dto.mapper;

import it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants;
import it.gov.pagopa.payment.instrument.dto.InstrumentFromDiscountDTO;
import it.gov.pagopa.payment.instrument.model.PaymentInstrument;
import java.util.function.Function;
import org.springframework.stereotype.Service;

@Service
public class InstrumentFromDiscountDTO2PaymentInstrumentMapper implements
    Function<InstrumentFromDiscountDTO, PaymentInstrument> {

  @Override
  public PaymentInstrument apply(InstrumentFromDiscountDTO instrumentFromDiscountDTO) {
    return PaymentInstrument.builder()
        .initiativeId(instrumentFromDiscountDTO.getInitiativeId())
        .userId(instrumentFromDiscountDTO.getUserId())
        .hpan(PaymentInstrumentConstants.IDPAY_PAYMENT_FAKE_INSTRUMENT_PREFIX.formatted(instrumentFromDiscountDTO.getUserId()))
        .channel(PaymentInstrumentConstants.IDPAY_PAYMENT).status(
            PaymentInstrumentConstants.STATUS_ACTIVE).build();
  }
}
