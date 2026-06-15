package grupo5.incentivos.services;

import grupo5.incentivos.dto.*;
import grupo5.incentivos.infrastructure.NotificacionesClient;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import grupo5.incentivos.models.entities.donante.EventoDonacion;
import grupo5.incentivos.models.entities.insignias.Insignia;
import grupo5.incentivos.models.entities.misiones.Mision;
import grupo5.incentivos.models.repositories.DonanteIncentivosRepository;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class IncentivosService {

  private final DonanteIncentivosRepository repository;
  private final MisionFactory misionFactory;
  private final NotificacionesClient notificacionesClient;
  private final RankingService rankingService;

  public IncentivosService(
      DonanteIncentivosRepository repository,
      MisionFactory misionFactory,
      NotificacionesClient notificacionesClient,
      RankingService rankingService) {
    this.repository = repository;
    this.misionFactory = misionFactory;
    this.notificacionesClient = notificacionesClient;
    this.rankingService = rankingService;
  }

  public DonanteRegistradoDTO registrarDonante(Long donanteId) {
    DonanteIncentivos donante =
        repository
            .buscarPorId(donanteId)
            .orElseGet(
                () -> {
                  DonanteIncentivos nuevo = new DonanteIncentivos(donanteId);
                  nuevo.setMisiones(misionFactory.crearMisionesEstandar());
                  repository.guardar(nuevo);
                  return nuevo;
                });
    return DonanteRegistradoDTO.desde(donante);
  }

  public void procesarDonacion(Long donanteId, EventoDonacion evento) {
    DonanteIncentivos donante =
            repository.buscarPorId(donanteId)
                    .orElseGet(() -> {
                      registrarDonante(donanteId);
                      return obtenerDonante(donanteId);
                    });

    Set<String> misionesCompletadasAntes =
            donante.getMisiones().stream()
                    .filter(Mision::isCompletada)
                    .map(Mision::getNombre)
                    .collect(java.util.stream.Collectors.toSet());

    donante.registrarDonacion(evento);

    donante.getMisiones().stream()
            .filter(Mision::isCompletada)
            .filter(m -> !misionesCompletadasAntes.contains(m.getNombre()))
            .forEach(mision -> {
              Insignia insignia = mision.getInsignia();
              String recompensa = insignia != null ? insignia.getNombre() : "Sin recompensa";
              notificacionesClient.notificarMisionCumplida(donanteId, mision.getNombre(), recompensa);
            });

    if (donante.intentarAscenso()) {
      notificacionesClient.notificarAscensoCategoria(donanteId, donante.getCategoria().name());
    }

    repository.guardar(donante);
  }

  public void procesarDonacionExitosa(Long donanteId, Long organizacionId) {
    DonanteIncentivos donante = obtenerDonante(donanteId);

    Set<String> misionesCompletadasAntes =
            donante.getMisiones().stream()
                    .filter(Mision::isCompletada)
                    .map(Mision::getNombre)
                    .collect(java.util.stream.Collectors.toSet());

    donante.registrarDonacionExitosa(organizacionId);

    donante.getMisiones().stream()
            .filter(Mision::isCompletada)
            .filter(m -> !misionesCompletadasAntes.contains(m.getNombre()))
            .forEach(mision -> {
              Insignia insignia = mision.getInsignia();
              String recompensa = insignia != null ? insignia.getNombre() : "Sin recompensa";
              notificacionesClient.notificarMisionCumplida(donanteId, mision.getNombre(), recompensa);
            });

    if (donante.intentarAscenso()) {
      notificacionesClient.notificarAscensoCategoria(donanteId, donante.getCategoria().name());
    }

    repository.guardar(donante);
  }

  public DonanteIncentivos obtenerDonante(Long donanteId) {
    return repository
        .buscarPorId(donanteId)
        .orElseThrow(
            () ->
                new DonanteIncentivosNotFoundException(
                    "No existe un perfil de incentivos para el donante con id " + donanteId));
  }

  public MetricasDonanteDTO obtenerMetricas(Long donanteId) {
    DonanteIncentivos donante = obtenerDonante(donanteId);
    Integer posicion = rankingService.obtenerPosicionDonante(donanteId).orElse(null);

    int misionesCompletadas =
        (int) donante.getMisiones().stream().filter(Mision::isCompletada).count();

    Map<String, Long> evolucion =
        donante.getMetricas().donacionesPorPeriodo().entrySet().stream()
            .collect(Collectors.toMap(e -> e.getKey().toString(), Map.Entry::getValue));

    return MetricasDonanteDTO.desde(donante, posicion, misionesCompletadas, evolucion);
  }

  public List<MisionDTO> obtenerMisiones(Long donanteId) {
    return obtenerDonante(donanteId).getMisiones().stream().map(MisionDTO::desde).toList();
  }

  public List<InsigniaDTO> obtenerInsignias(Long donanteId) {
    return obtenerDonante(donanteId).getInsignias().stream().map(InsigniaDTO::desde).toList();
  }

  public void configurarVisibilidadInsignia(
      Long donanteId, String nombreInsignia, boolean visible) {
    DonanteIncentivos donante = obtenerDonante(donanteId);
    donante.getInsignias().stream()
        .filter(i -> i.getNombre().equals(nombreInsignia))
        .findFirst()
        .ifPresent(i -> i.setVisible(visible));
    repository.guardar(donante);
  }

  public void darDeBaja(Long donanteId) {
    DonanteIncentivos donante = obtenerDonante(donanteId);
    repository.eliminar(donante);
  }

  public ResumenSistemaDTO obtenerResumenSistema() {
    List<DonanteIncentivos> todos = repository.listarTodos();
    YearMonth mesActual = YearMonth.now();
    YearMonth mesAnterior = mesActual.minusMonths(1);

    int donantesMesActual =
        (int) todos.stream().filter(d -> d.getMetricas().donacionesEnMes(mesActual) > 0).count();

    int donantesMesAnterior =
        (int) todos.stream().filter(d -> d.getMetricas().donacionesEnMes(mesAnterior) > 0).count();

    long totalMisiones =
        todos.stream().flatMap(d -> d.getMisiones().stream()).filter(Mision::isCompletada).count();

    long misionesMesActual =
        todos.stream()
            .mapToLong(
                d -> d.misionesCompletadasEnMes(mesActual.getYear(), mesActual.getMonthValue()))
            .sum();

    Map<String, Long> porCategoria =
        todos.stream()
            .collect(Collectors.groupingBy(d -> d.getCategoria().name(), Collectors.counting()));

    Map<String, Long> evolucion =
        todos.stream()
            .flatMap(d -> d.getMetricas().donacionesPorPeriodo().entrySet().stream())
            .collect(Collectors.toMap(e -> e.getKey().toString(), Map.Entry::getValue, Long::sum));

    return new ResumenSistemaDTO(
        todos.size(),
        donantesMesActual,
        donantesMesAnterior,
        totalMisiones,
        misionesMesActual,
        porCategoria,
        evolucion);
  }

  public List<DonanteIncentivos> listarTodos() {
    return repository.listarTodos();
  }

  public static class DonanteIncentivosNotFoundException extends RuntimeException {
    public DonanteIncentivosNotFoundException(String message) {
      super(message);
    }
  }
}
