package it.gov.pagopa.payment.instrument.repository;

import it.gov.pagopa.payment.instrument.model.PaymentInstrument;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentInstrumentRepository extends MongoRepository<PaymentInstrument, String> {

  List<PaymentInstrument> findByIdWalletAndStatus(String idWallet, String status);

  List<PaymentInstrument> findByInitiativeIdAndUserIdAndHpan(String initiativeId, String userId,
      String hpan);
  List<PaymentInstrument> findByInitiativeIdAndUserIdAndId(String initiativeId, String userId,
      String instrumentId);

  int countByInitiativeIdAndUserIdAndStatus(String initiativeId, String userId, String status);

  int countByHpanAndStatus(String hpan, String status);

  List<PaymentInstrument> findByInitiativeIdAndUserId(String initiativeId,String userId);

  List<PaymentInstrument> findByInitiativeIdAndUserIdAndStatus(String initiativeId,String userId, String status);

  List<PaymentInstrument> findByHpanAndUserIdAndStatus(String hpan,String userId, String statusActive);
}
