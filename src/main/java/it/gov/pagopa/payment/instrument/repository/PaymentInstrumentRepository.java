package it.gov.pagopa.payment.instrument.repository;

import it.gov.pagopa.payment.instrument.model.PaymentInstrument;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentInstrumentRepository extends MongoRepository<PaymentInstrument, String> {

  List<PaymentInstrument> findByHpanAndStatus(String hpan, String status);

  List<PaymentInstrument> findByInitiativeIdAndUserIdAndHpan(String initiativeId, String userId,
      String hpan);

  int countByInitiativeIdAndUserIdAndStatus(String initiativeId, String userId, String status);

  int countByHpanAndStatus(String hpan, String status);

  List<PaymentInstrument> findByInitiativeIdAndUserId(String initiativeId,String userId);

  List<PaymentInstrument> findByInitiativeIdAndUserIdAndStatus(String initiativeId,String userId, String status);
}
