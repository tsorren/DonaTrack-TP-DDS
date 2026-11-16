package grupo5.notificaciones.services.impl;

import grupo5.notificaciones.dto.input.EventoCuentaEnlaceSolicitadoV1;

/**
 * El texto del correo de cuenta. {@code CorreoAdapter} manda un solo texto, sin asunto aparte: la
 * primera línea hace de título. Solo hay español, que es el único idioma del realm.
 */
final class MensajeDeCuenta {

  private MensajeDeCuenta() {}

  static String armar(EventoCuentaEnlaceSolicitadoV1 evento) {
    return switch (evento.tipo()) {
      case EventoCuentaEnlaceSolicitadoV1.RECUPERAR_CLAVE ->
          """
Recuperá tu contraseña de DonaTrack

Pediste elegir una contraseña nueva. Abrí este enlace para hacerlo: vale %d minutos y se puede usar una sola vez.

%s

Si no lo pediste vos, ignorá este mensaje: tu contraseña no cambia."""
              .formatted(evento.venceEnMinutos(), evento.enlace());
      case EventoCuentaEnlaceSolicitadoV1.VERIFICAR_EMAIL ->
          """
          Verificá tu email en DonaTrack

          Para activar tu cuenta, abrí este enlace: vale %d minutos y se puede usar una sola vez.

          %s

          Si no creaste una cuenta en DonaTrack, ignorá este mensaje."""
              .formatted(evento.venceEnMinutos(), evento.enlace());
      default -> throw new IllegalArgumentException("Tipo de enlace de cuenta desconocido");
    };
  }
}
