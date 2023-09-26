package it.gov.pagopa.payment.instrument.dto.mapper;

import it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants;
import it.gov.pagopa.payment.instrument.dto.InstrumentFromDiscountDTO;
import it.gov.pagopa.payment.instrument.model.PaymentInstrument;
import it.gov.pagopa.payment.instrument.test.fakers.InstrumentFromDiscountDTOFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InstrumentFromDiscountDTO2PaymentInstrumentMapperTest {

  private InstrumentFromDiscountDTO2PaymentInstrumentMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new InstrumentFromDiscountDTO2PaymentInstrumentMapper();
  }

  @Test
  void apply() {
    InstrumentFromDiscountDTO instrumentFromDiscountDTO = InstrumentFromDiscountDTOFaker.mockInstance(
        1);

    PaymentInstrument result = mapper.apply(instrumentFromDiscountDTO);

    Assertions.assertAll(() -> {
      Assertions.assertEquals(instrumentFromDiscountDTO.getInitiativeId(),
          result.getInitiativeId());
      Assertions.assertEquals(instrumentFromDiscountDTO.getUserId(), result.getUserId());
      Assertions.assertEquals(PaymentInstrumentConstants.INSTRUMENT_TYPE_QRCODE, result.getInstrumentType());
      Assertions.assertEquals(PaymentInstrumentConstants.IDPAY_PAYMENT, result.getChannel());
      Assertions.assertEquals(
          PaymentInstrumentConstants.IDPAY_PAYMENT_FAKE_INSTRUMENT_PREFIX.formatted(
              instrumentFromDiscountDTO.getUserId()), result.getHpan());
      Assertions.assertTrue(result.isConsent());
      Assertions.assertNotNull(result.getActivationDate());
    });

  }
}