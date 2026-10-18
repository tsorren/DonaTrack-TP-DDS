package grupo5.notificaciones.infrastructure.adapters.dtos.twilio;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TwilioSmsRequest(
    @JsonProperty("To") String to,
    @JsonProperty("From") String from,
    @JsonProperty("Body") String body) {}
