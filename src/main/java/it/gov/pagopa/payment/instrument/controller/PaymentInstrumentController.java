package it.gov.pagopa.payment.instrument.controller;

import it.gov.pagopa.payment.instrument.dto.*;

import javax.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/idpay/instrument")
public interface PaymentInstrumentController {

  @PutMapping("/enroll")
  ResponseEntity<Void> enrollInstrument(
      @Valid @RequestBody EnrollmentBodyDTO body);

  @DeleteMapping("/deactivate")
  ResponseEntity<Void> deleteInstrument(
      @Valid @RequestBody DeactivationBodyDTO body);

  @DeleteMapping("/disableall")
  ResponseEntity<Void> disableAllInstrument(
      @Valid @RequestBody UnsubscribeBodyDTO body);

  @GetMapping("/{initiativeId}/{userId}")
  ResponseEntity<HpanGetDTO> getHpan(@PathVariable("initiativeId") String initiativeId,
      @PathVariable("userId") String userId);

  @GetMapping("/{initiativeId}/{userId}/{channel}")
  ResponseEntity<HpanGetDTO> getHpanFromIssuer(@PathVariable("initiativeId") String initiativeId,
      @PathVariable("userId") String userId, @PathVariable("channel") String channel);

  @PutMapping("/hb/enroll")
  ResponseEntity<HpanGetDTO> enrollFromIssuer(@Valid @RequestBody InstrumentIssuerDTO body);

  @GetMapping("/initiatives/{idWallet}/{userId}/detail")
  ResponseEntity<InstrumentDetailDTO> getInstrumentInitiativesDetail(
          @PathVariable("userId") String userId,
          @PathVariable("idWallet") String idWallet);

}
