package it.gov.pagopa.payment.instrument.dto.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import it.gov.pagopa.payment.instrument.dto.RuleEngineQueueDTO;
import it.gov.pagopa.payment.instrument.dto.mapper.MessageMapper;
import java.time.LocalDateTime;
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
  private static final String OPERATION_TYPE = "ADD_INSTRUMENT";
  @Test
  void testApply() {

    RuleEngineQueueDTO ruleEngineQueueDTO = new RuleEngineQueueDTO();
    ruleEngineQueueDTO.setUserId(USER_ID);
    ruleEngineQueueDTO.setInitiativeId(INITIATIVE_ID);
    ruleEngineQueueDTO.setOperationType(OPERATION_TYPE);
    ruleEngineQueueDTO.setHpan(HPAN);
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
    assertEquals(OPERATION_TYPE, ruleEngineQueueDTO.getOperationType());
    assertEquals(HPAN, ruleEngineQueueDTO.getHpan());
    assertNotNull(ruleEngineQueueDTO.getOperationDate());
  }

}
