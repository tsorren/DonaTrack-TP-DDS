package grupo5.cliente.rest;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PutExchange;

/**
 * Lo que el cliente usa de Incentivos (docs/arquitectura/contratos/openapi-incentivos.yaml): el
 * ranking y, del donante, su perfil, misiones, insignias, ascensos y métricas. Los records copian
 * los campos de cada DTO; el resto del JSON se ignora. La declara entera el esqueleto de la
 * conexión de pantallas (SPEC-08): los frentes la usan y no la cambian. {@code donanteId} es el id
 * del donante en Donaciones, que es el de su persona ({@code Donante.id == personaId}; que
 * Incentivos use el mismo es una inferencia del alta por evento, sin verificar contra un servicio).
 * Con ingreso, la llamada lleva el token del usuario (lo pone {@code ClientesHttp}, SPEC-09, F5);
 * sin ingreso va sin cabecera.
 */
@HttpExchange("/api/incentivos")
interface IncentivosApi {

  @GetExchange("/ranking/historial")
  List<RankingMensual> historial();

  /** El último ranking; 204 (null) si no hay. */
  @GetExchange("/ranking/ultimo")
  RankingMensual ultimoRanking();

  @GetExchange("/donantes/{donanteId}")
  PerfilDeDonante donante(@PathVariable UUID donanteId);

  @GetExchange("/donantes/{donanteId}/misiones")
  List<Mision> misiones(@PathVariable UUID donanteId);

  @GetExchange("/donantes/{donanteId}/insignias")
  List<Insignia> insignias(
      @PathVariable UUID donanteId, @RequestParam(required = false) Boolean soloVisibles);

  @PutExchange("/donantes/{donanteId}/insignias/{nombreInsignia}/visibilidad")
  void mostrarInsignia(
      @PathVariable UUID donanteId,
      @PathVariable String nombreInsignia,
      @RequestParam boolean visible);

  @GetExchange("/donantes/{donanteId}/ascensos")
  List<Ascenso> ascensos(@PathVariable UUID donanteId);

  @GetExchange("/donantes/{donanteId}/metricas")
  Metricas metricas(@PathVariable UUID donanteId);

  /** Para el inicio del admin. */
  @GetExchange("/admin/resumen")
  Resumen resumen();

  record RankingMensual(
      String periodo, List<Entrada> entradas, List<Entrada> podio, Instant calculadoEn) {}

  record Entrada(int posicion, String nombreDonante, long misionesCompletadas, UUID donanteId) {}

  record PerfilDeDonante(
      UUID donanteId,
      UUID idPersona,
      String nombre,
      String categoria,
      LocalDate fechaRegistro,
      Integer misionesCompletadas,
      Integer insigniasGanadas) {}

  /**
   * Una misión del donante, en el orden de MisionFactory. {@code categoria}: COLABORADOR,
   * SOSTENEDOR o TRANSFORMADOR; el avance ({@code progresoActual} de {@code objetivo}) no dice en
   * qué unidad está.
   */
  record Mision(
      String nombre,
      String descripcion,
      String categoria,
      Integer progresoActual,
      Integer objetivo,
      Integer porcentaje,
      Integer distanciaAlObjetivo,
      Boolean completada,
      LocalDate fechaCompletada,
      Insignia insignia) {}

  record Insignia(
      String nombre,
      String descripcion,
      String imagenUrl,
      Boolean visible,
      LocalDate fechaObtenida) {}

  record Ascenso(String categoriaAnterior, String categoriaNueva, LocalDate fecha) {}

  record Metricas(
      UUID donanteId,
      String categoria,
      Integer totalDonacionesHistoricas,
      Integer totalOrganizacionesAyudadas,
      Integer totalDonacionesExitosas,
      LocalDate ultimaDonacion,
      MisionActiva misionActiva,
      Map<String, Long> donacionesPorPeriodo,
      Integer posicionEnRanking) {}

  record MisionActiva(
      String nombre,
      String descripcion,
      Integer progresoActual,
      Integer objetivo,
      Integer porcentaje,
      Integer distanciaAlObjetivo) {}

  record Resumen(Map<String, Long> donantesPorCategoria, Map<String, Long> evolucionMensual) {}
}
