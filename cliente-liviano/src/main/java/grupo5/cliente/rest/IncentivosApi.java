package grupo5.cliente.rest;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * Lo que el cliente usa de Incentivos (docs/arquitectura/contratos/openapi-incentivos.yaml). Los
 * records toman solo los campos que muestra la pantalla; el resto del JSON se ignora.
 */
@HttpExchange("/api/incentivos")
interface IncentivosApi {

  @GetExchange("/ranking/historial")
  List<RankingMensual> historial();

  @GetExchange("/donantes/{donanteId}")
  PerfilDonante perfilDonante(@PathVariable UUID donanteId);

  @GetExchange("/donantes/{donanteId}/misiones")
  List<Mision> misiones(@PathVariable UUID donanteId);

  @GetExchange("/donantes/{donanteId}/insignias")
  List<Insignia> insignias(@PathVariable UUID donanteId);

  @GetExchange("/donantes/{donanteId}/ascensos")
  List<Ascenso> ascensos(@PathVariable UUID donanteId);

  record RankingMensual(String periodo, List<Entrada> entradas, Instant calculadoEn) {}

  record Entrada(int posicion, String nombreDonante, long misionesCompletadas) {}

  record PerfilDonante(
      UUID donanteId,
      UUID idPersona,
      String nombre,
      String categoria,
      LocalDate fechaRegistro,
      int misionesCompletadas,
      int insigniasGanadas) {}

  record Mision(
      String nombre,
      String descripcion,
      String categoria,
      int progresoActual,
      int objetivo,
      int porcentaje,
      int distanciaAlObjetivo,
      boolean completada,
      LocalDate fechaCompletada,
      Insignia insignia) {}

  record Insignia(
      String nombre,
      String descripcion,
      String imagenUrl,
      boolean visible,
      LocalDate fechaObtenida) {}

  record Ascenso(String categoriaAnterior, String categoriaNueva, LocalDate fecha) {}
}
