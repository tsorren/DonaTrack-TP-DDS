package grupo5.cliente.rest;

import java.time.Instant;
import java.util.List;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * Lo que el cliente usa de Incentivos (docs/arquitectura/contratos/openapi-incentivos.yaml). Los
 * records toman solo los campos que muestra la pantalla; el resto del JSON se ignora.
 */
@HttpExchange("/api/incentivos/ranking")
interface IncentivosApi {

  @GetExchange("/historial")
  List<RankingMensual> historial();

  record RankingMensual(String periodo, List<Entrada> entradas, Instant calculadoEn) {}

  record Entrada(int posicion, String nombreDonante, long misionesCompletadas) {}
}
