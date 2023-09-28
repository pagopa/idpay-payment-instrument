package it.gov.pagopa.payment.instrument.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import it.gov.pagopa.common.web.dto.ErrorDTO;
import java.util.HashMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@ContextConfiguration(classes = Utilities.class)
class UtilitiesTest {

    @Autowired
    Utilities utilities;

    @MockBean
    ObjectMapper objectMapper;

    private static final String BAD_REQUEST = "BAD REQUEST";
    private static final String MESSAGE = "test";

    @Test
    void exceptionConverter_ok() throws JsonProcessingException {
        Request request =
                Request.create(Request.HttpMethod.PUT, "url", new HashMap<>(), null, new RequestTemplate());
        FeignException.BadRequest e = new FeignException.BadRequest(BAD_REQUEST, request, new byte[0], null);

        ErrorDTO errorDTO = new ErrorDTO(400, MESSAGE);
        Mockito.when(objectMapper.readValue(anyString(), (Class<ErrorDTO>) any())).thenReturn(errorDTO);

        String error = utilities.exceptionConverter(e);

        assertEquals(MESSAGE, error);
        
    }

    @Test
    void exceptionConverter_ok_DTOMessageNull() throws JsonProcessingException {
        Request request =
                Request.create(Request.HttpMethod.PUT, "url", new HashMap<>(), null, new RequestTemplate());
        FeignException.BadRequest e = new FeignException.BadRequest(BAD_REQUEST, request, new byte[0], null);

        ErrorDTO errorDTO = new ErrorDTO(400, null);
        Mockito.when(objectMapper.readValue(anyString(), (Class<ErrorDTO>) any())).thenReturn(errorDTO);

        String error = utilities.exceptionConverter(e);

        assertEquals(BAD_REQUEST, error);

    }

    @Test
    void exceptionConverter_exception(){
        Request request =
                Request.create(Request.HttpMethod.PUT, "url", new HashMap<>(), null, new RequestTemplate());
        FeignException.BadRequest e = new FeignException.BadRequest(BAD_REQUEST, request, new byte[0], null);

        String error = utilities.exceptionConverter(e);

        assertEquals(BAD_REQUEST, error);

    }
}


