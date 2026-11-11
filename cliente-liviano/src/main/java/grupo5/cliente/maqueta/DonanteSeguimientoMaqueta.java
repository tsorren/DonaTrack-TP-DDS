package grupo5.cliente.maqueta;

import grupo5.cliente.puertos.DonanteSeguimientoPuerto;
import grupo5.cliente.vistas.DonanteSeguimientoVista;
import grupo5.cliente.vistas.DonanteSeguimientoVista.Punto;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Seguimiento de la empresa de ejemplo: las siete donaciones y los puntos sintéticos del mapa que
 * escribía la plantilla, uno por barrio.
 */
@Component
@ConditionalOnProperty(
    name = "cliente.datos.donante",
    havingValue = "maqueta",
    matchIfMissing = true)
class DonanteSeguimientoMaqueta implements DonanteSeguimientoPuerto {

  private static final DonanteSeguimientoVista SEGUIMIENTO =
      new DonanteSeguimientoVista(
          DonanteDonacionesDeEjemplo.DONACIONES,
          Map.of(
              "lentejas", new Punto(-34.6392, -58.4150, -34.6418, -58.4291),
              "utiles", new Punto(-34.6240, -58.3944, -34.6052, -58.3846),
              "leche", new Punto(-34.6405, -58.3960, -34.6683, -58.4067)),
          "hoy, 15 de octubre, a las 16:25");

  @Override
  public DonanteSeguimientoVista seguimiento(UUID donante) {
    return SEGUIMIENTO;
  }
}
