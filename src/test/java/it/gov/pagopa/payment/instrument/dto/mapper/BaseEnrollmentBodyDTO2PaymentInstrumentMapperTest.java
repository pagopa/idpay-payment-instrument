package it.gov.pagopa.payment.instrument.dto.mapper;

import it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants;
import it.gov.pagopa.payment.instrument.dto.BaseEnrollmentBodyDTO;
import it.gov.pagopa.payment.instrument.model.PaymentInstrument;
import it.gov.pagopa.payment.instrument.test.fakers.BaseEnrollmentDTOFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BaseEnrollmentBodyDTO2PaymentInstrumentMapperTest {

  private BaseEnrollmentBodyDTO2PaymentInstrument mapper;

  @BeforeEach
  void setUp() {
    mapper = new BaseEnrollmentBodyDTO2PaymentInstrument();
  }

  @Test
  void apply() {
    BaseEnrollmentBodyDTO baseEnrollmentDTO = BaseEnrollmentDTOFaker.mockInstance(1);
    String testHpan = "TEST_HPAN";

    PaymentInstrument result = mapper.apply(baseEnrollmentDTO, testHpan);

    Assertions.assertAll(() -> {
      Assertions.assertEquals(baseEnrollmentDTO.getInitiativeId(),
          result.getInitiativeId());
      Assertions.assertEquals(baseEnrollmentDTO.getUserId(), result.getUserId());
      Assertions.assertEquals(baseEnrollmentDTO.getChannel(), result.getChannel());
      Assertions.assertEquals(testHpan, result.getHpan());
      Assertions.assertEquals(baseEnrollmentDTO.getInstrumentType(), result.getInstrumentType());
      Assertions.assertEquals(PaymentInstrumentConstants.STATUS_PENDING_RE, result.getStatus());
    });

  }
}