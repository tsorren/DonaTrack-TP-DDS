package grupo5.cliente.maqueta;

import grupo5.cliente.puertos.RankingPuerto;
import grupo5.cliente.vistas.RankingVista;
import grupo5.cliente.vistas.RankingVista.Mes;
import grupo5.cliente.vistas.RankingVista.Puesto;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Los rankings de ejemplo; con {@code ?escenario=vacio}, ninguno (el 204 de Incentivos). */
@Component
@ConditionalOnProperty(
    name = "cliente.datos.ranking",
    havingValue = "maqueta",
    matchIfMissing = true)
class RankingMaqueta implements RankingPuerto {

  private static final String ANA = "Ana Ejemplo";
  private static final String JUAN = "Juan Ejemplo";
  private static final String DISTRIBUIDORA = "Distribuidora Ejemplo SA";
  private static final String FUNDACION = "Fundación Ejemplo Sur";

  private static final List<Mes> MESES =
      List.of(
          new Mes(
              "2026-09",
              "Septiembre de 2026",
              "30 de septiembre de 2026, 23:59",
              List.of(
                  new Puesto(ANA, 5),
                  new Puesto(JUAN, 4),
                  new Puesto(DISTRIBUIDORA, 3),
                  new Puesto(FUNDACION, 2),
                  new Puesto("Pedro Ejemplo", 1))),
          new Mes(
              "2026-08",
              "Agosto de 2026",
              "31 de agosto de 2026, 23:59",
              List.of(
                  new Puesto(JUAN, 6),
                  new Puesto(ANA, 4),
                  new Puesto(FUNDACION, 2),
                  new Puesto(DISTRIBUIDORA, 1))),
          new Mes(
              "2026-07",
              "Julio de 2026",
              "31 de julio de 2026, 23:59",
              List.of(new Puesto(DISTRIBUIDORA, 3), new Puesto(ANA, 2), new Puesto(JUAN, 1))));

  @Override
  public RankingVista ranking() {
    return new RankingVista(Escenario.actual() == Escenario.VACIO ? List.of() : MESES, "octubre");
  }
}
