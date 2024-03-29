package it.gov.pagopa.payment.instrument.model;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "payment_instrument")
@FieldNameConstants
@Builder
public class PaymentInstrument {

  @Id
  private String id;
  private String userId;
  private String initiativeId;
  private String idWallet;
  private String hpan;
  private String maskedPan;
  private String brandLogo;
  private String brand;
  private boolean consent;
  private String status;
  private String channel;
  private String instrumentType;
  private String deleteChannel;
  private LocalDateTime activationDate;
  private LocalDateTime deactivationDate;
  private LocalDateTime rtdAckDate;
  private LocalDateTime reAckDate;
  private LocalDateTime updateDate;
  private LocalDateTime creationDate;
}
