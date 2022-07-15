package it.gov.pagopa.payment.instrument.controller;

import it.gov.pagopa.payment.instrument.dto.DeactivationBodyDTO;
import it.gov.pagopa.payment.instrument.dto.EnrollmentBodyDTO;
import it.gov.pagopa.payment.instrument.dto.HpanGetDTO;
import it.gov.pagopa.payment.instrument.dto.InstrumentResponseDTO;
import javax.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * IdPay - Payment Instrument
 */

@RestController
@RequestMapping("/idpay/instrument")
public interface PaymentInstrumentController {

  /**
   * Enrollment of a Payment Instrument
   *
   * @param body
   * @return
   */
  @PutMapping("/enroll")
  ResponseEntity<InstrumentResponseDTO> enrollInstrument(
      @Valid @RequestBody EnrollmentBodyDTO body);

  /**
   * Deactivation of a Payment Instrument
   *
   * @param body
   * @return
   */
  @DeleteMapping("/deactivate")
  ResponseEntity<InstrumentResponseDTO> deleteInstrument(
      @Valid @RequestBody DeactivationBodyDTO body);

  /**
   * Enrollment of a Payment Instrument
   *
   * @param initiativeId
   * @param userId
   * @return
   */
  @GetMapping("/{initiativeId}/{userId}")
  ResponseEntity<HpanGetDTO> getHpan(@PathVariable("initiativeId") String initiativeId,
      @PathVariable("userId") String userId);

}
