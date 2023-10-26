package it.gov.pagopa.payment.instrument.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@FieldNameConstants()
@Document(collection = "payment_instrument_code")
public class PaymentInstrumentCode {

  @Id
  private String userId;
  private String idpayCode;
  private String salt;
  private String secondFactor;
  private String keyId;
  private int generationCodeCounter;
  private LocalDateTime creationDate;

}
