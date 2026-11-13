package grupo5.cliente.rest;

import grupo5.cliente.puertos.AvisosSinLeer;
import grupo5.cliente.puertos.CuentaActual;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

/**
 * El conteo de sin leer desde Notificaciones, con la persona de la cuenta. La administración no
 * tiene persona en el token: cuenta los de la bandeja compartida de la Administración (R11-30), la
 * persona fija {@code cliente.notificaciones.administracion}. Si Notificaciones no responde, la
 * cabecera queda sin número: no rompe la pantalla.
 */
// @Primary solo para el análisis estático (S9352): la propiedad ya deja un único adaptador activo.
@Component
@Primary
@ConditionalOnProperty(name = "cliente.datos.avisos", havingValue = "rest")
class AvisosRest implements AvisosSinLeer {

  /**
   * La persona de la Administración en Notificaciones, la misma de {@code AdminConstantes.ID_ADMIN}
   * en los servicios (la que ya recibe el aviso de contacto).
   */
  static final String ADMINISTRACION = "00000000-0000-0000-0000-000000000001";

  private final NotificacionesApi notificaciones;
  private final CuentaActual cuentas;
  private final UUID administracion;

  AvisosRest(
      NotificacionesApi notificaciones,
      CuentaActual cuentas,
      @Value("${cliente.notificaciones.administracion:" + ADMINISTRACION + "}")
          UUID administracion) {
    this.notificaciones = notificaciones;
    this.cuentas = cuentas;
    this.administracion = administracion;
  }

  /**
   * La persona de Notificaciones de la cuenta del área; la del admin es la de la Administración.
   */
  static Optional<UUID> persona(
      String prefijoDeArea, CuentaActual cuentas, UUID administracion, HttpServletRequest pedido) {
    return switch (prefijoDeArea) {
      case "donante" -> cuentas.donante(pedido);
      case "entidad" -> cuentas.entidad(pedido);
      case "admin" -> Optional.of(administracion);
      default -> Optional.empty();
    };
  }

  @Override
  public long sinLeer(String prefijoDeArea, HttpServletRequest pedido) {
    Optional<UUID> persona = persona(prefijoDeArea, cuentas, administracion, pedido);
    if (persona.isEmpty()) {
      return 0;
    }
    try {
      NotificacionesApi.Conteo conteo = notificaciones.sinLeer(persona.get());
      return conteo == null || conteo.noLeidas() == null ? 0 : conteo.noLeidas();
    } catch (RestClientException sinRespuesta) {
      return 0;
    }
  }
}
