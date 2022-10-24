package it.gov.pagopa.payment.instrument.dto.mapper;

import static com.mongodb.assertions.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants;
import it.gov.pagopa.payment.instrument.dto.InstrumentAckDTO;
import it.gov.pagopa.payment.instrument.dto.RuleEngineAckDTO;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@ContextConfiguration(classes = AckMapper.class)
class AckMapperTest {

  private static final String USER_ID = "TEST_USER_ID";
  private static final String INITIATIVE_ID = "TEST_INITIATIVE_ID";
  private static final String HPAN = "TEST_HPAN";
  private static final String CHANNEL = "TEST_CHANNEL";
  private static final String MASKED_PAN = "TEST_MASKED_PAN";
  private static final String BRAND_LOGO = "TEST_BRAND_LOGO";
  private static final LocalDateTime DATE = LocalDateTime.now();
  private static final int NINSTR = 1;
  private static final RuleEngineAckDTO RULE_ENGINE_ACK_DTO = new RuleEngineAckDTO(INITIATIVE_ID,
      USER_ID, PaymentInstrumentConstants.OPERATION_ADD, List.of(HPAN), List.of(), DATE);

  @Autowired
  AckMapper ackMapper;

  @Test
  void ackToWallet() {
    InstrumentAckDTO actual = ackMapper.ackToWallet(RULE_ENGINE_ACK_DTO, CHANNEL, MASKED_PAN, BRAND_LOGO, NINSTR);

    assertNotNull(actual);
    assertEquals(INITIATIVE_ID, actual.getInitiativeId());
    assertEquals(USER_ID, actual.getUserId());
    assertEquals(MASKED_PAN, actual.getMaskedPan());
    assertEquals(BRAND_LOGO, actual.getBrandLogo());
    assertEquals(CHANNEL, actual.getChannel());
    assertEquals(NINSTR, actual.getNinstr());
    assertEquals(DATE, actual.getOperationDate());
  }
}