package grupo5.cliente.maqueta;

import grupo5.cliente.puertos.EntidadInicioPuerto;
import grupo5.cliente.vistas.EntidadInicioVista;
import grupo5.cliente.vistas.EntidadInicioVista.Necesidad;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * El Inicio del Comedor Barrio Ejemplo el 15 de octubre de 2026: sus cinco necesidades vigentes,
 * las de Necesidades.
 */
@Component
@ConditionalOnProperty(
    name = "cliente.datos.entidad",
    havingValue = "maqueta",
    matchIfMissing = true)
class EntidadInicioMaqueta implements EntidadInicioPuerto {

  private static final String ALIMENTOS = "cat-alimentos";
  private static final String FIN_DE_OCTUBRE = "Octubre, hasta el 31";

  private static final EntidadInicioVista INICIO =
      new EntidadInicioVista(
          "jueves 15 de octubre de 2026",
          "15 de octubre, 18:30",
          List.of(
              new Necesidad(
                  "Aceite de girasol",
                  ALIMENTOS,
                  "0 de 20 litros",
                  0,
                  20,
                  "Hasta el 20 de octubre"),
              new Necesidad(
                  "Leche en polvo",
                  ALIMENTOS,
                  "12 de 24 unidades",
                  12,
                  24,
                  "Hasta el 25 de octubre"),
              new Necesidad("Lentejas secas", ALIMENTOS, "10 de 40 kg", 10, 40, FIN_DE_OCTUBRE),
              new Necesidad("Arroz", ALIMENTOS, "15 de 40 kg", 15, 40, FIN_DE_OCTUBRE),
              new Necesidad(
                  "Ropa de abrigo",
                  "cat-ropa",
                  "5 de 30 unidades",
                  5,
                  30,
                  "Hasta el 15 de noviembre")));

  @Override
  public EntidadInicioVista inicio(UUID entidad) {
    return INICIO;
  }
}
