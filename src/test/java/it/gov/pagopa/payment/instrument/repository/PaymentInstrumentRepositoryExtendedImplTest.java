package it.gov.pagopa.payment.instrument.repository;

import it.gov.pagopa.payment.instrument.model.PaymentInstrument;
import it.gov.pagopa.payment.instrument.model.PaymentInstrument.Fields;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@ContextConfiguration(classes = PaymentInstrumentRepositoryExtendedImpl.class)
public class PaymentInstrumentRepositoryExtendedImplTest {

    @Autowired
    PaymentInstrumentRepositoryExtended paymentInstrumentRepositoryExtended;
    @MockBean
    MongoTemplate mongoTemplate;
    private static final String INSTRUMENT_ID = "INSTRUMENT_ID";
    private static final String INITIATIVE_ID = "INITIATIVE_ID";

    @Test
    void deletePaged (){
        // Given
        String initiativeId = INITIATIVE_ID;
        int pageSize = 100;
        Pageable pageable = PageRequest.of(0, pageSize);
        PaymentInstrument paymentInstrument = PaymentInstrument.builder()
                .id(INSTRUMENT_ID)
                .initiativeId(INITIATIVE_ID)
                .build();
        when(mongoTemplate.findAllAndRemove(Query.query(Criteria.where(Fields.initiativeId).is(initiativeId)).with(pageable), PaymentInstrument.class))
                .thenReturn(List.of(paymentInstrument));

        // When
        List<PaymentInstrument> result = paymentInstrumentRepositoryExtended.deletePaged(initiativeId, pageSize);


        Assertions.assertEquals(1, result.size());
        verify(mongoTemplate, times(1)).findAllAndRemove(
                Query.query(Criteria.where(Fields.initiativeId).is(initiativeId)).with(pageable),
                PaymentInstrument.class);
    }

}
