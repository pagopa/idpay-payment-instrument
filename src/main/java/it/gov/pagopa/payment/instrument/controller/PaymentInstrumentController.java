package it.gov.pagopa.payment.instrument.controller;

import it.gov.pagopa.payment.instrument.dto.*;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


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
          @PathVariable("idWallet") String idWallet,
          @PathVariable("userId") String userId,
          @RequestParam(required = false) List<String> statusList);

  @PutMapping("/discount/enroll")
  ResponseEntity<Void> enrollDiscountInitiative(@Valid @RequestBody InstrumentFromDiscountDTO body);

  @PutMapping("/rollback/{initiativeId}/{userId}")
  ResponseEntity<Void> rollback(@PathVariable("initiativeId") String initiativeId, @PathVariable("userId") String userId);

  @PostMapping("/generate-code/{userId}")
  ResponseEntity<GenerateCodeRespDTO> generateCode(@PathVariable("userId") String userId, @RequestBody GenerateCodeReqDTO body);

}
