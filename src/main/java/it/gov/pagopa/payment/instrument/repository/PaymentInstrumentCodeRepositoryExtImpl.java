package it.gov.pagopa.payment.instrument.repository;

import it.gov.pagopa.payment.instrument.model.PaymentInstrumentCode;
import java.time.LocalDateTime;
import org.springframework.data.mongodb.core.MongoTemplate;
import it.gov.pagopa.payment.instrument.model.PaymentInstrumentCode.Fields;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

public class PaymentInstrumentCodeRepositoryExtImpl implements PaymentInstrumentCodeRepositoryExt {
  private final MongoTemplate mongoTemplate;

  public PaymentInstrumentCodeRepositoryExtImpl(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  @Override
  public void updateCode(String userId, String code, LocalDateTime creationDate) {
    mongoTemplate.updateFirst(
        Query.query(Criteria.where(Fields.userId).is(userId)),
        new Update()
            .set(Fields.userId, userId)
            .set(Fields.code, code)
            .set(Fields.creationDate, creationDate),
        PaymentInstrumentCode.class);
  }
}
