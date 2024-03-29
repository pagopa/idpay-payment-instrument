package it.gov.pagopa.payment.instrument.repository;

import it.gov.pagopa.payment.instrument.model.PaymentInstrumentCode;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentInstrumentCodeRepository extends MongoRepository<PaymentInstrumentCode, String>, PaymentInstrumentCodeRepositoryExt {

}
