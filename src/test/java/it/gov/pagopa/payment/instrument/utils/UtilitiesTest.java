package it.gov.pagopa.payment.instrument.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import it.gov.pagopa.common.web.dto.ErrorDTO;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.HashMap;

import static it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants.ExceptionCode.GENERIC_ERROR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@ContextConfiguration(classes = Utilities.class)
class UtilitiesTest {

    @Autowired
    Utilities utilities;

    @MockitoBean
    JsonMapper objectMapper;

    private static final String BAD_REQUEST = "BAD REQUEST";

    @Test
    void exceptionConverter_ok() {
        Request request =
                Request.create(Request.HttpMethod.PUT, "url", new HashMap<>(), null, new RequestTemplate());
        FeignException.BadRequest e = new FeignException.BadRequest(BAD_REQUEST, request, new byte[0], null);

        ErrorDTO errorDTO = new ErrorDTO("DUMMY_CODE", "DUMMY_MESSAGE");
        Mockito.when(objectMapper.readValue(anyString(), (Class<ErrorDTO>) any()))
                .thenReturn(errorDTO);

        ErrorDTO error = utilities.exceptionErrorDTOConverter(e);

        assertEquals(errorDTO, error);

    }

    @SneakyThrows
    @Test
    void exceptionConverter_JsonProcessingException(){
        Request request =
                Request.create(Request.HttpMethod.PUT, "url", new HashMap<>(), null, new RequestTemplate());
        FeignException.BadRequest e = new FeignException.BadRequest(BAD_REQUEST, request, new byte[0], null);

        Mockito.when(objectMapper.readValue(anyString(), (Class<ErrorDTO>) any()))
                .thenThrow(JacksonException.class);

        ErrorDTO error = utilities.exceptionErrorDTOConverter(e);

        assertEquals(new ErrorDTO(GENERIC_ERROR, null), error);

    }
}



