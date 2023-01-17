package it.gov.pagopa.payment.instrument.dto.mapper;

import it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants;
import it.gov.pagopa.payment.instrument.dto.InstrumentAckDTO;
import it.gov.pagopa.payment.instrument.dto.RuleEngineAckDTO;
import it.gov.pagopa.payment.instrument.dto.rtd.RTDEnrollAckDTO;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class AckMapper {

  public InstrumentAckDTO ackToWallet(RuleEngineAckDTO dto, String channel, String maskedPan,
      String brandLogo, int nInstr) {
    String operationType = (dto.getRejectedHpanList().isEmpty()) ? dto.getOperationType() : dto.getOperationType().concat("REJECTED_");
    return InstrumentAckDTO.builder()
        .initiativeId(dto.getInitiativeId())
        .userId(dto.getUserId())
        .channel(channel)
        .maskedPan(maskedPan)
        .brandLogo(brandLogo)
        .ninstr(nInstr)
        .operationType(operationType)
        .operationDate(dto.getTimestamp())
        .build();
  }

  public InstrumentAckDTO ackToWalletRTD(RTDEnrollAckDTO dto, String channel, String maskedPan,
          String brandLogo, int nInstr, String userId, LocalDateTime timestamp) {
    String operationType = PaymentInstrumentConstants.OPERATION_ADD;
    return InstrumentAckDTO.builder()
            .initiativeId(dto.getCorrelationId())
            .userId(userId)
            .channel(channel)
            .maskedPan(maskedPan)
            .brandLogo(brandLogo)
            .ninstr(nInstr)
            .operationType(operationType)
            .operationDate(timestamp)
            .build();
  }
}
