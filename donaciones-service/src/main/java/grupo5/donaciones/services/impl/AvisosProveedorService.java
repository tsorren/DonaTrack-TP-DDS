package grupo5.donaciones.services.impl;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.dto.comunicaciones.EventoEntregaExitosa;
import grupo5.donaciones.dto.comunicaciones.EventoEntregaFallida;
import grupo5.donaciones.dto.comunicaciones.EventoRutaAsignada;
import grupo5.donaciones.dto.comunicaciones.EventoRutaIniciada;
import grupo5.donaciones.dto.logistica.AvisoProveedorRequestDTO;
import grupo5.donaciones.models.entities.logistica.SolicitudEntrega;
import grupo5.donaciones.models.repositories.ISolicitudesEntregaRepository;
import grupo5.donaciones.services.IAvisosProveedorService;
import grupo5.donaciones.services.logistica.IProcesadorEventosLogistica;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Camino de vuelta de los proveedores HTTP. Traduce el aviso al evento que ya usa el camino AMQP y
 * lo entrega al {@link ProcesadorEventosLogistica}, con la misma idempotencia. Antes verifica que
 * cada donación esté asignada al proveedor que avisa, según el registro de solicitudes del broker.
 */
@Service
public class AvisosProveedorService implements IAvisosProveedorService {

  private static final String ORIGEN_HTTP = "http:";

  private final ISolicitudesEntregaRepository solicitudesRepository;
  private final IProcesadorEventosLogistica procesador;

  public AvisosProveedorService(
      ISolicitudesEntregaRepository solicitudesRepository, IProcesadorEventosLogistica procesador) {
    this.solicitudesRepository = solicitudesRepository;
    this.procesador = procesador;
  }

  @Override
  public void registrarAviso(String proveedorId, AvisoProveedorRequestDTO aviso) {
    if (proveedorId == null || proveedorId.isBlank() || aviso == null || aviso.tipo() == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    String origen = ORIGEN_HTTP + proveedorId;
    LocalDateTime ahora = LocalDateTime.now();

    switch (aviso.tipo()) {
      case RUTA_ASIGNADA -> {
        UUID rutaId = exigir(aviso.rutaId());
        UUID donacionId = exigir(aviso.donacionIndependienteId());
        verificarPertenencia(proveedorId, List.of(donacionId));
        procesador.procesarRutaAsignada(new EventoRutaAsignada(rutaId, donacionId, ahora), origen);
      }
      case RUTA_INICIADA -> {
        UUID rutaId = exigir(aviso.rutaId());
        List<UUID> donaciones = exigirLista(aviso.donacionesIndependientesIds());
        verificarPertenencia(proveedorId, donaciones);
        procesador.procesarRutaIniciada(
            new EventoRutaIniciada(
                rutaId, null, aviso.patenteCamion(), donaciones, ahora, aviso.urlMapa()),
            origen);
      }
      case ENTREGA_EXITOSA -> {
        UUID entregaId = exigir(aviso.entregaId());
        UUID donacionId = exigir(aviso.donacionIndependienteId());
        verificarPertenencia(proveedorId, List.of(donacionId));
        procesador.procesarEntregaExitosa(
            new EventoEntregaExitosa(entregaId, donacionId, null, aviso.patenteCamion(), ahora),
            origen);
      }
      case ENTREGA_FALLIDA -> {
        UUID entregaId = exigir(aviso.entregaId());
        UUID donacionId = exigir(aviso.donacionIndependienteId());
        verificarPertenencia(proveedorId, List.of(donacionId));
        procesador.procesarEntregaFallida(
            new EventoEntregaFallida(
                entregaId,
                donacionId,
                aviso.justificacion(),
                ahora,
                Boolean.TRUE.equals(aviso.replanificable())),
            origen);
      }
    }
  }

  /**
   * Todas las donaciones deben estar asignadas a este proveedor; si no, no se aplica ninguna. Vale
   * la solicitud activa de la donación o, si no hay, la más reciente aunque esté {@code FALLIDA}:
   * si falló tras envíos inciertos, el proveedor quizás sí tiene la entrega y sus avances no deben
   * perderse. Una solicitud que falló por rechazos no tiene proveedor actual, así que no aplica.
   */
  private void verificarPertenencia(String proveedorId, List<UUID> donaciones) {
    for (UUID donacionId : donaciones) {
      boolean asignada =
          solicitudesRepository
              .findActivaPorDonacion(donacionId)
              .or(() -> solicitudesRepository.findMasRecientePorDonacion(donacionId))
              .map(SolicitudEntrega::getProveedorActual)
              .filter(proveedorId::equals)
              .isPresent();
      if (!asignada) {
        throw new RecursoNoEncontradoException(donacionId);
      }
    }
  }

  private static UUID exigir(UUID valor) {
    if (valor == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }
    return valor;
  }

  private static List<UUID> exigirLista(List<UUID> valores) {
    if (valores == null || valores.isEmpty() || valores.stream().anyMatch(Objects::isNull)) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }
    return valores;
  }
}
