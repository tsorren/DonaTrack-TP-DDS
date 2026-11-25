package grupo5.logistica.models.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;

import grupo5.logistica.models.entities.camiones.PosicionCamion;
import grupo5.logistica.models.entities.rutas.EstimadorDeLlegada;
import grupo5.logistica.models.entities.rutas.direccion.CoordenadasGeograficas;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * HG-02: la hora estimada de llegada. 0,1° de latitud son 11,119 km; a 20 km/h, 2002 segundos
 * (redondeado). Cada parada pendiente anterior suma 10 minutos.
 */
class EstimadorDeLlegadaTest {

  private static final Instant POSICION_EN = Instant.parse("2026-11-25T14:00:00Z");
  private static final PosicionCamion CAMION = new PosicionCamion(-34.6, -58.4, POSICION_EN);

  @Test
  void estimar_destinoDirecto_sumaElTiempoDeLaDistanciaALaHoraDeLaPosicion() {
    Instant estimada =
        EstimadorDeLlegada.estimar(CAMION, List.of(new CoordenadasGeograficas(-34.7, -58.4)), 0);

    assertEquals(POSICION_EN.plusSeconds(2002), estimada);
  }

  @Test
  void estimar_destinoEnLaPosicionDelCamion_esLaHoraDeLaPosicion() {
    Instant estimada =
        EstimadorDeLlegada.estimar(CAMION, List.of(new CoordenadasGeograficas(-34.6, -58.4)), 0);

    assertEquals(POSICION_EN, estimada);
  }

  @Test
  void estimar_conParadasAnteriores_recorreCadaTramoYSumaDiezMinutosPorParada() {
    Instant estimada =
        EstimadorDeLlegada.estimar(
            CAMION,
            List.of(
                new CoordenadasGeograficas(-34.7, -58.4), new CoordenadasGeograficas(-34.6, -58.4)),
            1);

    // Ida y vuelta: 22,239 km son 4003 segundos, más la demora de la parada intermedia.
    assertEquals(POSICION_EN.plusSeconds(4003 + 600L), estimada);
  }

  @Test
  void estimar_paradaAnteriorSinCoordenadas_sumaSuDemoraSinDistancia() {
    Instant estimada =
        EstimadorDeLlegada.estimar(CAMION, List.of(new CoordenadasGeograficas(-34.7, -58.4)), 1);

    assertEquals(POSICION_EN.plusSeconds(2002 + 600L), estimada);
  }
}
