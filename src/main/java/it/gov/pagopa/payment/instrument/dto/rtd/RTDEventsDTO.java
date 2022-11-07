package it.gov.pagopa.payment.instrument.dto.rtd;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type",
    visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = RTDRevokeCardDTO.class, name = "RevokeCard"),
    @JsonSubTypes.Type(value = RTDEnrollVirtualCardDTO.class, name = "EnrollCard"),
    @JsonSubTypes.Type(value = RTDRevokeTokenDTO.class, name = "RevokeToken"),
    @JsonSubTypes.Type(value = RTDEnrollAckDTO.class, name = "EnrollAck")
})
public interface RTDEventsDTO {

}
