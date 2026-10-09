package grupo5.notificaciones.infrastructure.adapters.dtos.meta;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MetaWhatsAppRequest(
    @JsonProperty("messaging_product") String messagingProduct,
    @JsonProperty("recipient_type") String recipientType,
    @JsonProperty("to") String to,
    @JsonProperty("type") String type,
    @JsonProperty("text") TextContent text) {
  public record TextContent(
      @JsonProperty("preview_url") boolean previewUrl, @JsonProperty("body") String body) {}
}
