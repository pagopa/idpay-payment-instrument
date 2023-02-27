package it.gov.pagopa.payment.instrument.controller;

import it.gov.pagopa.payment.instrument.dto.*;
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
  public ResponseEntity<Void> enrollInstrument(EnrollmentBodyDTO body)
      {
    paymentInstrumentService.enrollInstrument(
        body.getInitiativeId(),
        body.getUserId(),
        body.getIdWallet(),
        body.getChannel()
    );

    return new ResponseEntity<>(HttpStatus.OK);
  }


  @Override
  public ResponseEntity<Void> deleteInstrument(DeactivationBodyDTO body) {
    paymentInstrumentService.deactivateInstrument(body.getInitiativeId(), body.getUserId(),
        body.getInstrumentId());
    return new ResponseEntity<>(HttpStatus.OK);
  }


  @Override
  public ResponseEntity<Void> disableAllInstrument(UnsubscribeBodyDTO body) {
    paymentInstrumentService.deactivateAllInstruments(body.getInitiativeId(), body.getUserId(),
        body.getUnsubscribeDate());
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<HpanGetDTO> getHpan(String initiativeId, String userId) {
    HpanGetDTO hpanGetDTO = paymentInstrumentService.getHpan(initiativeId, userId);
    return new ResponseEntity<>(hpanGetDTO, HttpStatus.OK);
  }

  @Override
  public ResponseEntity<HpanGetDTO> getHpanFromIssuer(String initiativeId, String userId,
      String channel) {
    HpanGetDTO hpanGetDTO = paymentInstrumentService.getHpanFromIssuer(initiativeId, userId, channel);
    return new ResponseEntity<>(hpanGetDTO, HttpStatus.OK);
  }

  @Override
  public ResponseEntity<HpanGetDTO> enrollFromIssuer(InstrumentIssuerDTO body) {
    paymentInstrumentService.enrollFromIssuer(body);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @Override
  public ResponseEntity<InstrumentDetailDTO> getInstrumentInitiativesDetail(String idWallet, String userId) {
    InstrumentDetailDTO instrumentDetailDTO = paymentInstrumentService.getInstrumentInitiativesDetail(userId, idWallet);
    return new ResponseEntity<>(instrumentDetailDTO, HttpStatus.OK);
  }

}
