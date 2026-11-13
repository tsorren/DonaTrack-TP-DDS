package grupo5.cliente.maqueta;

import grupo5.cliente.puertos.SolicitudEntidadPuerto;
import grupo5.cliente.vistas.SolicitudEntidadVista;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * La solicitud del Merendero Ejemplo Oeste, con los datos de ejemplo del registro (R11-41). {@code
 * ?escenario=corregida} trae los datos después de corregirlos, {@code sin-datos} falla como si
 * Donaciones no contestara y {@code aprobada} responde que el equipo ya la aprobó. Corregir no
 * guarda nada: en la maqueta el formulario vuelve a su pantalla.
 */
@Component
@ConditionalOnProperty(
    name = "cliente.datos.entidad",
    havingValue = "maqueta",
    matchIfMissing = true)
class SolicitudEntidadMaqueta implements SolicitudEntidadPuerto {

  private static final String CORREO = "rosa@merendero-ejemplo.test";

  private static final SolicitudEntidadVista ENVIADA =
      new SolicitudEntidadVista(
          "Merendero Ejemplo Oeste",
          "Merendero",
          "asociacion",
          "Calle Ejemplo",
          1234,
          "1000",
          "Localidad Ejemplo",
          "Provincia Ejemplo",
          "Rosa Ejemplo",
          CORREO);

  private static final SolicitudEntidadVista CORREGIDA =
      new SolicitudEntidadVista(
          "Merendero Ejemplo Oeste",
          "Comedor",
          "asociacion",
          "Calle Ejemplo",
          1250,
          "1000",
          "Localidad Ejemplo",
          "Provincia Ejemplo",
          "Rosa Ejemplo",
          CORREO);

  @Override
  public SolicitudEntidadVista leer(UUID personaId) {
    return switch (escenario()) {
      case "sin-datos" -> throw new IllegalStateException("Donaciones no contestó (maqueta)");
      case "aprobada" -> throw new YaAprobada();
      case "corregida" -> CORREGIDA;
      default -> ENVIADA;
    };
  }

  @Override
  public void corregir(UUID personaId, SolicitudEntidadVista cambios) {
    // En la maqueta el formulario no guarda: SitioController lo devuelve a su pantalla.
  }

  private static String escenario() {
    if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes pedido) {
      String valor = pedido.getRequest().getParameter("escenario");
      return valor == null ? "" : valor;
    }
    return "";
  }
}
