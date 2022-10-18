package it.gov.pagopa.payment.instrument.controller;

import it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants;
import it.gov.pagopa.payment.instrument.dto.DeactivationBodyDTO;
import it.gov.pagopa.payment.instrument.dto.EnrollmentBodyDTO;
import it.gov.pagopa.payment.instrument.dto.HpanGetDTO;
import it.gov.pagopa.payment.instrument.dto.InstrumentResponseDTO;
import it.gov.pagopa.payment.instrument.dto.UnsubscribeBodyDTO;
import it.gov.pagopa.payment.instrument.dto.pm.PaymentMethodInfoList;
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
  public ResponseEntity<InstrumentResponseDTO> enrollInstrument(EnrollmentBodyDTO body)
      {
    PaymentMethodInfoList info = paymentInstrumentService.enrollInstrument(
        body.getInitiativeId(),
        body.getUserId(),
        body.getIdWallet(),
        body.getChannel(),
        body.getActivationDate());

    int nInstr = paymentInstrumentService.countByInitiativeIdAndUserIdAndStatus(
        body.getInitiativeId(),
        body.getUserId(), PaymentInstrumentConstants.STATUS_ACTIVE);

    return new ResponseEntity<>(
        new InstrumentResponseDTO(nInstr, info.getBrandLogo(), info.getMaskedPan()), HttpStatus.OK);
  }


  @Override
  public ResponseEntity<InstrumentResponseDTO> deleteInstrument(DeactivationBodyDTO body) {
    PaymentMethodInfoList info = paymentInstrumentService.deactivateInstrument(body.getInitiativeId(), body.getUserId(),
        body.getInstrumentId(), body.getDeactivationDate());
    int nInstr = paymentInstrumentService.countByInitiativeIdAndUserIdAndStatus(
        body.getInitiativeId(),
        body.getUserId(), PaymentInstrumentConstants.STATUS_ACTIVE);
    return new ResponseEntity<>(new InstrumentResponseDTO(nInstr, info.getBrandLogo(), info.getMaskedPan()), HttpStatus.OK);
  }


  @Override
  public ResponseEntity<Void> disableAllInstrument(UnsubscribeBodyDTO body) {
    paymentInstrumentService.deactivateAllInstrument(body.getInitiativeId(), body.getUserId(),
        body.getUnsubscribeDate());
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<HpanGetDTO> getHpan(String initiativeId, String userId) {
    HpanGetDTO hpanGetDTO = paymentInstrumentService.gethpan(initiativeId, userId);
    return new ResponseEntity<>(hpanGetDTO, HttpStatus.OK);
  }
}
