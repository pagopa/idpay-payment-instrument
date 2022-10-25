package it.gov.pagopa.payment.instrument.repository;

import it.gov.pagopa.payment.instrument.model.PaymentInstrument;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentInstrumentRepository extends MongoRepository<PaymentInstrument, String> {

  List<PaymentInstrument> findByIdWalletAndStatus(String idWallet, String status);

  List<PaymentInstrument> findByIdWalletAndStatusNotContaining(String idWallet, String status);

  Optional<PaymentInstrument> findByInitiativeIdAndUserIdAndId(String initiativeId, String userId,
      String instrumentId);

  int countByInitiativeIdAndUserIdAndStatus(String initiativeId, String userId, String status);

  int countByHpanAndStatus(String hpan, String status);

  List<PaymentInstrument> findByInitiativeIdAndUserId(String initiativeId, String userId);

  List<PaymentInstrument> findByInitiativeIdAndUserIdAndStatus(String initiativeId, String userId,
      String status);

  Optional<PaymentInstrument> findByInitiativeIdAndUserIdAndHpanAndStatus(String initiativeId,
      String userId, String hpan, String status);

  List<PaymentInstrument> findByHpanAndUserIdAndStatus(String hpan, String userId,
      String statusActive);
}
