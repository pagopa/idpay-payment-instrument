package it.gov.pagopa.payment.instrument.dto.mapper;

import it.gov.pagopa.payment.instrument.dto.RuleEngineRequestDTO;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
public class MessageMapper {
  public Message<RuleEngineRequestDTO> apply(RuleEngineRequestDTO ruleEngineRequestDTO) {
    return MessageBuilder.withPayload(ruleEngineRequestDTO)
        .setHeader(KafkaHeaders.MESSAGE_KEY, ruleEngineRequestDTO.getUserId().concat(ruleEngineRequestDTO.getInitiativeId()))
        .build();
  }
}
