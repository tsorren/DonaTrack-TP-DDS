package grupo5.incentivos.services;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.incentivos.dto.*;
import grupo5.incentivos.infrastructure.N8nClient;
import grupo5.incentivos.infrastructure.NotificacionesClient;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import grupo5.incentivos.models.entities.donante.EventoDonacion;
import grupo5.incentivos.models.entities.insignias.Insignia;
import grupo5.incentivos.models.entities.misiones.Mision;
import grupo5.incentivos.models.repositories.DonanteIncentivosRepository;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class IncentivosService {

  private final DonanteIncentivosRepository repository;
  private final MisionFactory misionFactory;
  private final NotificacionesClient notificacionesClient;
  private final RankingService rankingService;
  private final N8nClient n8nClient;

  public IncentivosService(
      DonanteIncentivosRepository repository,
      MisionFactory misionFactory,
      NotificacionesClient notificacionesClient,
      RankingService rankingService,
      N8nClient n8nClient) {
    this.repository = repository;
    this.misionFactory = misionFactory;
    this.notificacionesClient = notificacionesClient;
    this.rankingService = rankingService;
    this.n8nClient = n8nClient;
  }

  public DonanteRegistradoDTO registrarDonante(RegistrarDonanteRequest request) {
    DonanteIncentivos donante =
        repository
            .findById(request.idDonante())
            .orElseGet(
                () -> {
                  DonanteIncentivos nuevo =
                      new DonanteIncentivos(
                          request.idDonante(), request.idPersona(), request.nombre());
                  nuevo.setMisiones(misionFactory.crearMisionesEstandar());
                  repository.save(nuevo);
                  return nuevo;
                });
    return DonanteRegistradoDTO.desde(donante);
  }

  public void procesarDonacion(NuevaDonacionRequest request) {

    EventoDonacion evento =
        EventoDonacion.builder()
            .categorias(request.categorias())
            .cantidadBienes(request.cantidadBienes())
            .fecha(request.fecha())
            .build();

    DonanteIncentivos donante =
        repository
            .findById(request.donanteId())
            .orElseThrow(() -> new RecursoNoEncontradoException(request.donanteId()));

    /* TODO: Revisar si registrar el donante debería ser responsabilidad de este caso de uso
    DonanteIncentivos donante =
      repository
          .findById(request.donanteId())
          .orElseGet(
              () -> {
                registrarDonante(new RegistrarDonanteRequest(request.donanteId(), request.));
                return obtenerDonante(donanteId);
              });

    */
    Set<String> misionesCompletadasAntes =
        donante.getMisiones().stream()
            .filter(Mision::isCompletada)
            .map(Mision::getNombre)
            .collect(Collectors.toSet());

    donante.registrarDonacion(evento);

    donante.getMisiones().stream()
        .filter(Mision::isCompletada)
        .filter(m -> !misionesCompletadasAntes.contains(m.getNombre()))
        .forEach(
            mision -> {
              Insignia insignia = mision.getInsignia();
              String recompensa = insignia != null ? insignia.getNombre() : "Sin recompensa";
              notificacionesClient.notificarMisionCumplida(
                  donante.getId(), mision.getNombre(), recompensa);
              // Disparar flujo n8n para publicar la insignia ganada
              if (insignia != null) {
                n8nClient.publicarInsigniaGanada(
                    donante.getId(),
                    "Donante " + donante.getNombre(),
                    insignia.getNombre(),
                    insignia.getDescripcion());
              }
            });

    if (donante.intentarAscenso()) {
      notificacionesClient.notificarAscensoCategoria(
          donante.getId(), donante.getCategoria().name());
    }

    repository.save(donante);
  }

  public void procesarDonacionExitosa(DonacionExitosaRequest request) {
    DonanteIncentivos donante = obtenerDonante(request.donanteId());

    Set<String> misionesCompletadasAntes =
        donante.getMisiones().stream()
            .filter(Mision::isCompletada)
            .map(Mision::getNombre)
            .collect(Collectors.toSet());

    donante.registrarDonacionExitosa(request.organizacionId());

    donante.getMisiones().stream()
        .filter(Mision::isCompletada)
        .filter(m -> !misionesCompletadasAntes.contains(m.getNombre()))
        .forEach(
            mision -> {
              Insignia insignia = mision.getInsignia();
              String recompensa = insignia != null ? insignia.getNombre() : "Sin recompensa";
              notificacionesClient.notificarMisionCumplida(
                  donante.getId(), mision.getNombre(), recompensa);
              // Disparar flujo n8n para publicar la insignia ganada
              if (insignia != null) {
                n8nClient.publicarInsigniaGanada(
                    donante.getId(),
                    "Donante #" + donante.getId(),
                    insignia.getNombre(),
                    insignia.getDescripcion());
              }
            });

    if (donante.intentarAscenso()) {
      notificacionesClient.notificarAscensoCategoria(
          donante.getId(), donante.getCategoria().name());
    }

    repository.save(donante);
  }

  public DonanteIncentivos obtenerDonante(UUID donanteId) {
    return repository
        .findById(donanteId)
        .orElseThrow(
            () -> new BusinessStateException(ErrorCatalog.DONANTE_INCENTIVOS_NO_ENCONTRADO));
  }

  public MetricasDonanteDTO obtenerMetricas(UUID donanteId) {
    DonanteIncentivos donante = obtenerDonante(donanteId);
    Integer posicion = rankingService.obtenerPosicionDonante(donanteId).orElse(null);

    int misionesCompletadas =
        (int) donante.getMisiones().stream().filter(Mision::isCompletada).count();

    Map<String, Long> evolucion =
        donante.getMetricas().donacionesPorPeriodo().entrySet().stream()
            .collect(Collectors.toMap(e -> e.getKey().toString(), Map.Entry::getValue));

    return MetricasDonanteDTO.desde(donante, posicion, misionesCompletadas, evolucion);
  }

  public List<MisionDTO> obtenerMisiones(UUID donanteId) {
    return obtenerDonante(donanteId).getMisiones().stream().map(MisionDTO::desde).toList();
  }

  public List<InsigniaDTO> obtenerInsignias(UUID donanteId) {
    return obtenerDonante(donanteId).getInsignias().stream().map(InsigniaDTO::desde).toList();
  }

  public void configurarVisibilidadInsignia(
      UUID donanteId, String nombreInsignia, boolean visible) {
    DonanteIncentivos donante = obtenerDonante(donanteId);
    donante.getInsignias().stream()
        .filter(i -> i.getNombre().equals(nombreInsignia))
        .findFirst()
        .orElseThrow(() -> new BusinessStateException(ErrorCatalog.INSIGNIA_NO_ENCONTRADA))
        .setVisible(visible);
    repository.save(donante);
  }

  public void darDeBaja(UUID donanteId) {
    DonanteIncentivos donante = obtenerDonante(donanteId);
    repository.delete(donante);
  }

  public ResumenSistemaDTO obtenerResumenSistema() {
    List<DonanteIncentivos> todos = repository.findAll();
    YearMonth mesActual = YearMonth.now(ZoneId.systemDefault());
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
    return repository.findAll();
  }
}
