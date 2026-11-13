package grupo5.notificaciones.dto.input;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.util.UUID;

/**
 * Evento {@code cuenta.enlace-solicitado.v1}, publicado por el SPI de correo de Keycloak (SPEC-06
 * E2-06, D8): una cuenta pidió un enlace de un solo uso para recuperar la clave o verificar el
 * email.
 *
 * <p>Lleva un secreto (el enlace) y un dato personal (el destinatario). Por eso no es un {@code
 * EventoNotificable}, no genera una {@code Notificacion} y nunca se guarda: se manda el correo y
 * listo. Y por eso {@link #toString()} no los muestra.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record EventoCuentaEnlaceSolicitadoV1(
    @NotBlank(message = "El tipo es obligatorio")
        @Pattern(regexp = "RECUPERAR_CLAVE|VERIFICAR_EMAIL", message = "El tipo no es conocido")
        String tipo,
    @NotNull(message = "El ID de la cuenta es obligatorio") UUID cuentaId,
    @NotBlank(message = "El destinatario es obligatorio")
        @Email(message = "El destinatario no es un email")
        String destinatario,
    @NotBlank(message = "El enlace es obligatorio")
        @Pattern(regexp = "^https?://\\S+$", message = "El enlace tiene que ser http o https")
        String enlace,
    @Positive(message = "El vencimiento tiene que ser positivo") long venceEnMinutos,
    @NotBlank(message = "El idioma es obligatorio") String idioma,
    @NotNull(message = "La fecha es obligatoria") Instant fecha) {

  public static final String RECUPERAR_CLAVE = "RECUPERAR_CLAVE";
  public static final String VERIFICAR_EMAIL = "VERIFICAR_EMAIL";

  /**
   * Sin el enlace ni el destinatario: un {@code log.info("{}", evento)} no tiene que filtrarlos.
   */
  @Override
  public String toString() {
    return "EventoCuentaEnlaceSolicitadoV1[tipo=" + tipo + ", cuentaId=" + cuentaId + "]";
  }
}
