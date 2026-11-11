package grupo5.cliente.rest;

import grupo5.cliente.puertos.AvisosSinLeer;
import grupo5.cliente.puertos.CuentaActual;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

/**
 * El conteo de sin leer desde Notificaciones, con la persona de la cuenta. La administración no
 * tiene persona en el token: su cabecera queda sin número. Si Notificaciones no responde, tampoco:
 * la cabecera no rompe la pantalla.
 */
// @Primary solo para el análisis estático (S9352): la propiedad ya deja un único adaptador activo.
@Component
@Primary
@ConditionalOnProperty(name = "cliente.datos.avisos", havingValue = "rest")
class AvisosRest implements AvisosSinLeer {

  private final NotificacionesApi notificaciones;
  private final CuentaActual cuentas;

  AvisosRest(NotificacionesApi notificaciones, CuentaActual cuentas) {
    this.notificaciones = notificaciones;
    this.cuentas = cuentas;
  }

  @Override
  public long sinLeer(String prefijoDeArea, HttpServletRequest pedido) {
    Optional<UUID> persona =
        switch (prefijoDeArea) {
          case "donante" -> cuentas.donante(pedido);
          case "entidad" -> cuentas.entidad(pedido);
          default -> Optional.empty();
        };
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
