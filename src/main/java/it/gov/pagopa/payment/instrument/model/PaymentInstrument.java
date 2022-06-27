package it.gov.pagopa.payment.instrument.model;

import java.util.Date;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "payment_instrument")
public class PaymentInstrument {

  public PaymentInstrument(String initiativeId, String userId, String hpan, String status,
      String channel, Date activationDate) {
    this.initiativeId = initiativeId;
    this.userId = userId;
    this.hpan = hpan;
    this.status = status;
    this.channel = channel;
    this.activationDate = activationDate;
  }

  @Id
  private String paymentInstrId;

  private String userId;

  private String initiativeId;

  private String hpan;

  private String status;

  private String channel;

  private Date activationDate;

  private Date deactivationDate;

}
