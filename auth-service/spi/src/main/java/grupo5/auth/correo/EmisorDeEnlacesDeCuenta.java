package grupo5.auth.correo;

import java.time.Instant;
import java.util.UUID;
import org.keycloak.email.EmailException;
import org.keycloak.models.UserModel;

/**
 * Arma el evento de un enlace de cuenta y lo publica. Es la lógica de {@link CorreoPorAmqpProvider}
 * sin depender de Keycloak, para poder probarla sin un servidor: valida que la cuenta tenga email,
 * arma el evento y traduce los fallos al error que Keycloak espera de un proveedor de correo.
 */
final class EmisorDeEnlacesDeCuenta {

  private final PublicadorDeEventos publicador;

  EmisorDeEnlacesDeCuenta(PublicadorDeEventos publicador) {
    this.publicador = publicador;
  }

  void emitir(String tipo, UserModel usuario, String idioma, String enlace, long venceEnMinutos)
      throws EmailException {
    if (usuario == null || usuario.getEmail() == null || usuario.getEmail().isBlank()) {
      throw new EmailException("La cuenta no tiene un email al que mandar el enlace");
    }
    try {
      publicador.publicar(
          new EventoEnlaceDeCuenta(
              tipo,
              usuario.getId(),
              usuario.getEmail(),
              enlace,
              venceEnMinutos,
              idioma,
              Instant.now()),
          nuevoTraceId());
    } catch (PublicacionFallida | IllegalArgumentException e) {
      throw new EmailException("No se pudo publicar el evento de la cuenta", e);
    }
  }

  /** Mismo formato que el resto del sistema: un UUID sin guiones (32 hex). */
  static String nuevoTraceId() {
    return UUID.randomUUID().toString().replace("-", "");
  }
}
