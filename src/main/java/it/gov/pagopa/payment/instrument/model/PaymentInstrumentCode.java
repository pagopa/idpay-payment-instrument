package it.gov.pagopa.payment.instrument.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;


@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@FieldNameConstants()
@Document(collection = "payment_instrument_code")
public class PaymentInstrumentCode {

  @Id
  private String id;
  private String userId;
  private String idpayCode;
  private int regenerationCodeCounter;
  private LocalDateTime creationDate;

}
