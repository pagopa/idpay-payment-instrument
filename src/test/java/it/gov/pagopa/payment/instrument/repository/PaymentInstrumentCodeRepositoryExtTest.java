package it.gov.pagopa.payment.instrument.repository;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@ContextConfiguration(classes = PaymentInstrumentCodeRepositoryExtImpl.class)
class PaymentInstrumentCodeRepositoryExtTest {

  @Autowired
  PaymentInstrumentCodeRepositoryExt paymentInstrumentCodeRepositoryExt;
  @MockBean
  MongoTemplate mongoTemplate;

  @Test
  void updateCode(){
    paymentInstrumentCodeRepositoryExt.updateCode(
        "USERID", "CODE", "SALT", "SECOND_FACTOR", LocalDateTime.now());
    Mockito.verify(mongoTemplate, Mockito.times(1))
        .upsert(Mockito.any(), Mockito.any(), (Class<?>) Mockito.any());
  }

  @Test
  void deleteInstrument(){
    paymentInstrumentCodeRepositoryExt.deleteInstrument("USERID");
    Mockito.verify(mongoTemplate, Mockito.times(1))
        .findAndRemove(Mockito.any(), (Class<?>) Mockito.any());
  }

}
