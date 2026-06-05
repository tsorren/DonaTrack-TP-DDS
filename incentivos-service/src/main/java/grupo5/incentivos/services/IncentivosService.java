package grupo5.incentivos.services;

import grupo5.incentivos.infrastructure.NotificacionesClient;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import grupo5.incentivos.models.entities.donante.EventoDonacion;
import grupo5.incentivos.models.entities.insignias.Insignia;
import grupo5.incentivos.models.entities.misiones.Mision;
import grupo5.incentivos.models.repositories.DonanteIncentivosRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class IncentivosService {

  private static final Logger log = LoggerFactory.getLogger(IncentivosService.class);

  private final DonanteIncentivosRepository repository;
  private final MisionFactory misionFactory;
  private final NotificacionesClient notificacionesClient;

  public IncentivosService(
      DonanteIncentivosRepository repository,
      MisionFactory misionFactory,
      NotificacionesClient notificacionesClient) {
    this.repository = repository;
    this.misionFactory = misionFactory;
    this.notificacionesClient = notificacionesClient;
  }

  public DonanteIncentivos registrarDonante(Long donanteId, String nombreUsuario) {
    return repository
        .buscarPorId(donanteId)
        .orElseGet(
            () -> {
              DonanteIncentivos nuevo = new DonanteIncentivos(donanteId);
              nuevo.setMisiones(misionFactory.crearMisionesEstandar());
              repository.guardar(nuevo);
              log.info("Donante {} registrado en sistema de incentivos.", donanteId);
              return nuevo;
            });
  }

  public void procesarDonacion(Long donanteId, String nombreUsuario, EventoDonacion evento) {
    DonanteIncentivos donante =
        repository
            .buscarPorId(donanteId)
            .orElseGet(
                () -> {
                  log.info(
                      "Donante {} no existia en incentivos, registrando automaticamente",
                      donanteId);
                  return registrarDonante(donanteId, nombreUsuario);
                });

    List<Mision> misionesAntesDeEvento =
        donante.getMisiones().stream().filter(Mision::isCompletada).toList();

    donante.registrarDonacion(evento);

    donante.getMisiones().stream()
        .filter(Mision::isCompletada)
        .filter(m -> !misionesAntesDeEvento.contains(m))
        .forEach(
            mision -> {
              log.info("Donante {} completo la mision '{}'", donanteId, mision.getNombre());
              Insignia insignia = mision.getInsignia();
              String recompensa = insignia != null ? insignia.getNombre() : "Sin recompensa";

              notificacionesClient.notificarMisionCumplida(
                  donanteId, mision.getNombre(), recompensa);
            });

    String categoriaAntes = donante.getCategoria().name();
    if (donante.intentarAscenso()) {
      log.info(
          "Donante {} ascendio de {} a {}",
          donanteId,
          categoriaAntes,
          donante.getCategoria().name());
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

  public void configurarVisibilidadInsignia(
      Long donanteId, String nombreInsignia, boolean visible) {
    DonanteIncentivos donante = obtenerDonante(donanteId);
    donante.getInsignias().stream()
        .filter(i -> i.getNombre().equals(nombreInsignia))
        .findFirst()
        .ifPresent(i -> i.setVisible(visible));
    repository.guardar(donante);
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
