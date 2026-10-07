package grupo5.notificaciones.infrastructure.adapters.dtos.sendgrid;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record SendGridEmailRequest(
    @JsonProperty("personalizations") List<Personalization> personalizations,
    @JsonProperty("from") EmailAddress from,
    @JsonProperty("subject") String subject,
    @JsonProperty("content") List<Content> content) {
  public record Personalization(@JsonProperty("to") List<EmailAddress> to) {}

  public record EmailAddress(
      @JsonProperty("email") String email, @JsonProperty("name") String name) {}

  public record Content(@JsonProperty("type") String type, @JsonProperty("value") String value) {}
}
