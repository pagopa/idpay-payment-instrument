package it.gov.pagopa.payment.instrument.repository;

import it.gov.pagopa.payment.instrument.model.PaymentInstrument;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentInstrumentRepository extends MongoRepository<PaymentInstrument, String> {

  List<PaymentInstrument> findByIdWalletAndStatus(String idWallet, String status);
  Optional<PaymentInstrument> findByInitiativeIdAndUserIdAndId(String initiativeId, String userId,
      String instrumentId);

  int countByInitiativeIdAndUserIdAndStatusIn(String initiativeId, String userId, List<String> status);

  int countByHpanAndStatusIn(String hpan, List<String> status);

  List<PaymentInstrument> findByInitiativeIdAndUserIdAndStatusNotContaining(String initiativeId, String userId, String status);
  List<PaymentInstrument> findByInitiativeIdAndUserIdAndChannelAndStatusIn(String initiativeId, String userId, String channel, List<String> status);
  List<PaymentInstrument> findByInitiativeIdAndUserIdAndStatus(String initiativeId, String userId, String status);
  List<PaymentInstrument> findByInitiativeIdAndUserIdAndStatusIn(String initiativeId, String userId,
      List<String> status);

  Optional<PaymentInstrument> findByInitiativeIdAndUserIdAndHpanAndStatus(String initiativeId,
      String userId, String hpan, String status);

  PaymentInstrument findByInitiativeIdAndHpanAndStatus(String initiativeId, String hpan, String status);

  List<PaymentInstrument> findByHpanAndUserIdAndStatus(String hpan, String userId,
      String status);

  List<PaymentInstrument> findByHpanAndStatus(String hpan, String status);
  List<PaymentInstrument> findByHpan(String hpan);

  List<PaymentInstrument> findByStatusRegex(String status);
  List<PaymentInstrument> findByIdWalletAndUserId(String idWallet, String userId);

}
