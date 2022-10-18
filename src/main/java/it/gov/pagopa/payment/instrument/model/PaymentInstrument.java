package it.gov.pagopa.payment.instrument.model;

import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "payment_instrument")
public class PaymentInstrument {

  public PaymentInstrument(String initiativeId, String userId, String idWallet,
      String hpan, String maskedPan, String brandLogo, String status, String channel,
      LocalDateTime activationDate) {
    this.userId = userId;
    this.initiativeId = initiativeId;
    this.idWallet = idWallet;
    this.hpan = hpan;
    this.maskedPan = maskedPan;
    this.brandLogo = brandLogo;
    this.status = status;
    this.channel = channel;
    this.activationDate = activationDate;
  }

  @Id
  private String id;

  private String userId;

  private String initiativeId;

  private String idWallet;
  private String hpan;

  private String maskedPan;

  private String brandLogo;

  private String status;
  private String channel;
  private String deleteChannel;

  private LocalDateTime activationDate;

  private LocalDateTime requestDeactivationDate;

}
