package it.gov.pagopa.payment.instrument.dto.mapper;

import it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants;
import it.gov.pagopa.payment.instrument.dto.InstrumentAckDTO;
import it.gov.pagopa.payment.instrument.dto.RuleEngineAckDTO;
import org.springframework.stereotype.Service;

@Service
public class AckMapper {

  public InstrumentAckDTO ackToWallet(RuleEngineAckDTO dto, String channel, String maskedPan,
      String brandLogo, int nInstr) {
    String operationType = (dto.getRejectedHpanList().isEmpty()) ? dto.getOperationType() : PaymentInstrumentConstants.REJECTED.concat(dto.getOperationType());
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
}
