package grupo5.incentivos.services;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.incentivos.dto.*;
import grupo5.incentivos.infrastructure.N8nClient;
import grupo5.incentivos.infrastructure.NotificacionesClient;
import grupo5.incentivos.models.entities.donante.CambioCategoria;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import grupo5.incentivos.models.entities.donante.EventoDonacion;
import grupo5.incentivos.models.entities.insignias.Insignia;
import grupo5.incentivos.models.entities.misiones.Mision;
import grupo5.incentivos.models.repositories.IDonanteIncentivosRepository;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class IncentivosService implements IIncentivosService {

  private final IDonanteIncentivosRepository repository;
  private final IMisionFactory misionFactory;
  private final NotificacionesClient notificacionesClient;
  private final IRankingService rankingService;
  private final N8nClient n8nClient;

  public IncentivosService(
      IDonanteIncentivosRepository repository,
      IMisionFactory misionFactory,
      NotificacionesClient notificacionesClient,
      IRankingService rankingService,
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

  public void modificarDonante(UUID donanteId, ModificarDonanteRequest request) {
    DonanteIncentivos donante = obtenerDonante(donanteId);
    donante.setNombre(request.nombre());
    repository.save(donante);
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

    Set<UUID> misionesCompletadasAntes = misionesCompletadasActuales(donante);

    donante.registrarDonacion(evento);

    notificarYGuardar(donante, misionesCompletadasAntes);
  }

  public void procesarDonacionExitosa(DonacionExitosaRequest request) {
    DonanteIncentivos donante = obtenerDonante(request.donanteId());

    Set<UUID> misionesCompletadasAntes = misionesCompletadasActuales(donante);

    donante.registrarDonacionExitosa(request.organizacionId());

    notificarYGuardar(donante, misionesCompletadasAntes);
  }

  private Set<UUID> misionesCompletadasActuales(DonanteIncentivos donante) {
    return donante.getMisiones().stream()
        .filter(Mision::isCompletada)
        .map(Mision::getId)
        .collect(Collectors.toSet());
  }

  private void notificarYGuardar(DonanteIncentivos donante, Set<UUID> misionesCompletadasAntes) {
    donante.getMisiones().stream()
        .filter(Mision::isCompletada)
        .filter(m -> !misionesCompletadasAntes.contains(m.getId()))
        .forEach(mision -> notificarMisionCompletada(donante, mision));

    if (donante.intentarAscenso()) {
      CambioCategoria ultimoCambio = donante.getHistorialCategorias().getLast();
      notificacionesClient.notificarAscensoCategoria(
          donante.getIdPersona(),
          ultimoCambio.getNueva().name(),
          ultimoCambio.getAnterior().name());
    }

    repository.save(donante);
  }

  private void notificarMisionCompletada(DonanteIncentivos donante, Mision mision) {
    Insignia insignia = mision.getInsignia();
    String recompensa = insignia != null ? insignia.getNombre() : "Sin recompensa";
    notificacionesClient.notificarMisionCumplida(
        donante.getIdPersona(), mision.getNombre(), recompensa);
    // Disparar flujo n8n para publicar la insignia ganada
    if (insignia != null) {
      n8nClient.publicarInsigniaGanada(
          donante.getId(),
          "Donante " + donante.getNombre(),
          insignia.getNombre(),
          insignia.getDescripcion());
    }
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
    return obtenerDonante(donanteId).getInsignias().stream()
        .filter(Insignia::isVisible)
        .map(InsigniaDTO::desde)
        .toList();
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
