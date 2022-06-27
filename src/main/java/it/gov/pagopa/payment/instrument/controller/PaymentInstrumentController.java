package it.gov.pagopa.payment.instrument.controller;

import it.gov.pagopa.payment.instrument.dto.DeactivationBodyDTO;
import it.gov.pagopa.payment.instrument.dto.EnrollmentBodyDTO;
import javax.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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
  ResponseEntity<Void> enrollInstrument(@Valid @RequestBody EnrollmentBodyDTO body);

  /**
   * Deactivation of a Payment Instrument
   *
   * @param body
   * @return
   */
  @DeleteMapping("/deactivate")
  ResponseEntity<Void> deleteInstrument(@Valid @RequestBody DeactivationBodyDTO body);

}
