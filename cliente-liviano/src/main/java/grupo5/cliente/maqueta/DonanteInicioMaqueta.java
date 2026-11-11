package grupo5.cliente.maqueta;

import grupo5.cliente.puertos.DonanteInicioPuerto;
import grupo5.cliente.vistas.DonanteDonacionVista;
import grupo5.cliente.vistas.DonanteInicioVista;
import grupo5.cliente.vistas.DonanteInicioVista.Aviso;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * El inicio de la empresa de ejemplo: las siete donaciones de viaje-datos, las tres noticias más
 * nuevas (las de datos-donante) y las fechas de la maqueta ("hoy" es el 15 de octubre de 2026).
 */
@Component
@ConditionalOnProperty(
    name = "cliente.datos.donante",
    havingValue = "maqueta",
    matchIfMissing = true)
class DonanteInicioMaqueta implements DonanteInicioPuerto {

  private static final DonanteInicioVista INICIO =
      new DonanteInicioVista(
          DonanteDonacionesDeEjemplo.DONACIONES,
          List.of(
              new Aviso(donacion("lentejas"), 0),
              new Aviso(donacion("arroz"), 0),
              new Aviso(donacion("leche"), 0)),
          4,
          2,
          "octubre de 2026",
          "15 de octubre",
          "15 de octubre, 16:25",
          "1 de octubre",
          "2026-10-15",
          "2026-10-14");

  private static DonanteDonacionVista donacion(String clave) {
    return DonanteDonacionesDeEjemplo.DONACIONES.stream()
        .filter(d -> d.clave().equals(clave))
        .findFirst()
        .orElseThrow();
  }

  @Override
  public DonanteInicioVista inicio(UUID donante) {
    return INICIO;
  }
}
