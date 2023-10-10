package it.gov.pagopa.payment.instrument.controller;

import it.gov.pagopa.payment.instrument.dto.*;
import it.gov.pagopa.payment.instrument.service.idpaycode.PaymentInstrumentCodeService;
import it.gov.pagopa.payment.instrument.service.PaymentInstrumentDiscountService;
import it.gov.pagopa.payment.instrument.service.PaymentInstrumentService;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentInstrumentControllerImpl implements PaymentInstrumentController {

  private final PaymentInstrumentService paymentInstrumentService;
  private final PaymentInstrumentDiscountService paymentInstrumentDiscountService;
  private final PaymentInstrumentCodeService paymentInstrumentCodeService;

  public PaymentInstrumentControllerImpl(PaymentInstrumentService paymentInstrumentService,
      PaymentInstrumentDiscountService paymentInstrumentDiscountService,
      PaymentInstrumentCodeService paymentInstrumentCodeService) {
    this.paymentInstrumentService = paymentInstrumentService;
    this.paymentInstrumentDiscountService = paymentInstrumentDiscountService;
    this.paymentInstrumentCodeService = paymentInstrumentCodeService;
  }

  @Override
  public ResponseEntity<Void> enrollInstrument(EnrollmentBodyDTO body)
      {
    paymentInstrumentService.enrollInstrument(
        body.getInitiativeId(),
        body.getUserId(),
        body.getIdWallet(),
        body.getChannel(),
        body.getInstrumentType()
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
  public ResponseEntity<InstrumentDetailDTO> getInstrumentInitiativesDetail(String idWallet, String userId, List<String> statusList) {
    InstrumentDetailDTO instrumentDetailDTO = paymentInstrumentService.getInstrumentInitiativesDetail(idWallet, userId, statusList);
    return new ResponseEntity<>(instrumentDetailDTO, HttpStatus.OK);
  }

  @Override
  public ResponseEntity<Void> enrollDiscountInitiative(InstrumentFromDiscountDTO body) {
    paymentInstrumentDiscountService.enrollDiscountInitiative(body);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @Override
  public ResponseEntity<Void> rollback(String initiativeId, String userId) {
    paymentInstrumentService.rollback(initiativeId,userId);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<GenerateCodeRespDTO> generateCode(String userId, Optional<GenerateCodeReqDTO> body) {
    String initiativeId = body.map(GenerateCodeReqDTO::getInitiativeId).orElse(null);
    GenerateCodeRespDTO generateCodeRespDTO = paymentInstrumentCodeService.generateCode(userId, initiativeId);

    return new ResponseEntity<>(generateCodeRespDTO, HttpStatus.OK);
  }

  @Override
  public ResponseEntity<Void> enrollInstrumentCode(BaseEnrollmentBodyDTO body) {
    paymentInstrumentDiscountService.enrollInstrumentCode(body);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @Override
  public ResponseEntity<CheckEnrollmentDTO> codeStatus(String userId) {
    return new ResponseEntity<>(new CheckEnrollmentDTO(paymentInstrumentCodeService.codeStatus(userId)), HttpStatus.OK);
  }

  @Override
  public ResponseEntity<VerifyPinBlockDTO> verifyPinBlock(String userId, PinBlockDTO pinBlockDTO) {
    return new ResponseEntity<>(new VerifyPinBlockDTO(paymentInstrumentCodeService.verifyPinBlock(userId, pinBlockDTO)), HttpStatus.OK);
  }

  @Override
  public ResponseEntity<SecondFactorDTO> secondFactor(String userId) {
    return new ResponseEntity<>(new SecondFactorDTO(paymentInstrumentCodeService.getSecondFactor(userId)), HttpStatus.OK);
  }
}
