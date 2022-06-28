package it.gov.pagopa.payment.instrument.controller;

import it.gov.pagopa.payment.instrument.dto.DeactivationBodyDTO;
import it.gov.pagopa.payment.instrument.dto.EnrollmentBodyDTO;
import it.gov.pagopa.payment.instrument.dto.InstrumentResponseDTO;
import it.gov.pagopa.payment.instrument.service.PaymentInstrumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentInstrumentControllerImpl implements PaymentInstrumentController {

  @Autowired
  PaymentInstrumentService paymentInstrumentService;

  @Override
  public ResponseEntity<InstrumentResponseDTO> enrollInstrument(EnrollmentBodyDTO body) {
    paymentInstrumentService.enrollInstrument(body.getInitiativeId(), body.getUserId(),
        body.getHpan(),
        body.getChannel(),
        body.getActivationDate());
    int nInstr = paymentInstrumentService.countByInitiativeIdAndUserId(body.getInitiativeId(),
        body.getUserId());
    return new ResponseEntity<>(new InstrumentResponseDTO(nInstr), HttpStatus.OK);
  }

  @Override
  public ResponseEntity<InstrumentResponseDTO> deleteInstrument(DeactivationBodyDTO body) {
    paymentInstrumentService.deactivateInstrument(body.getInitiativeId(), body.getUserId(),
        body.getHpan(), body.getDeactivationDate());
    int nInstr = paymentInstrumentService.countByInitiativeIdAndUserId(body.getInitiativeId(),
        body.getUserId());
    return new ResponseEntity<>(new InstrumentResponseDTO(nInstr), HttpStatus.OK);
  }
}
