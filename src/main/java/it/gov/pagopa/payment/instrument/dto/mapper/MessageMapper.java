package it.gov.pagopa.payment.instrument.dto.mapper;

import it.gov.pagopa.payment.instrument.dto.RuleEngineQueueDTO;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
public class MessageMapper {
  public Message<RuleEngineQueueDTO> apply(RuleEngineQueueDTO ruleEngineQueueDTO) {
    return MessageBuilder.withPayload(ruleEngineQueueDTO)
        .setHeader(KafkaHeaders.MESSAGE_KEY,ruleEngineQueueDTO.getUserId().concat(ruleEngineQueueDTO.getInitiativeId()))
        .build();
  }
}