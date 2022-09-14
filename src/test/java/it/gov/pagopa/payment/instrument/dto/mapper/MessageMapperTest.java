package it.gov.pagopa.payment.instrument.dto.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants;
import it.gov.pagopa.payment.instrument.dto.RuleEngineQueueDTO;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;

@Slf4j
class MessageMapperTest {
  private static final String USER_ID = "TEST_USER_ID";
  private static final String INITIATIVE_ID = "TEST_INITIATIVE_ID";
  private static final String HPAN = "TEST_HPAN";
  @Test
  void testApply() {

    RuleEngineQueueDTO ruleEngineQueueDTO = new RuleEngineQueueDTO();
    ruleEngineQueueDTO.setUserId(USER_ID);
    ruleEngineQueueDTO.setInitiativeId(INITIATIVE_ID);
    ruleEngineQueueDTO.setOperationType(PaymentInstrumentConstants.OPERATION_ADD);
    ruleEngineQueueDTO.setListHpan(new ArrayList<>(Collections.singleton(HPAN)));
    ruleEngineQueueDTO.setOperationDate(LocalDateTime.now());

    MessageMapper messageMapper = new MessageMapper();
    // When
    Message<RuleEngineQueueDTO> result = messageMapper.apply(ruleEngineQueueDTO);

    // Then
    log.info(Objects.requireNonNull(result.getHeaders().get(KafkaHeaders.MESSAGE_KEY)).toString());
    Assertions.assertEquals(USER_ID+INITIATIVE_ID, result.getHeaders().get(KafkaHeaders.MESSAGE_KEY));
    Assertions.assertSame(ruleEngineQueueDTO, result.getPayload());
    assertEquals(USER_ID, ruleEngineQueueDTO.getUserId());
    assertEquals(INITIATIVE_ID, ruleEngineQueueDTO.getInitiativeId());
    assertEquals(PaymentInstrumentConstants.OPERATION_ADD, ruleEngineQueueDTO.getOperationType());
    assertEquals(HPAN, ruleEngineQueueDTO.getListHpan().get(0));
    assertNotNull(ruleEngineQueueDTO.getOperationDate());
  }

}
