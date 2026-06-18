package grupo5.donaciones.services;

import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.donaciones.dto.donacionesIndependientes.CambioEstadoDonacionIndependienteRequestDTO;
import grupo5.donaciones.dto.donacionesIndependientes.DonacionIndependienteResponseDTO;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.TipoEstadoDonacion;
import grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class DonacionesIndependientesService implements IDonacionesIndependientesService {

  private final IDonacionesIndependientesRepository repositorio;

  public DonacionesIndependientesService(IDonacionesIndependientesRepository repositorio) {
    this.repositorio = repositorio;
  }

  @Override
  public DonacionIndependienteResponseDTO cambiarEstado(
      UUID id, CambioEstadoDonacionIndependienteRequestDTO request, String actor) {

    DonacionIndependiente donacion =
        repositorio.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(id));

    // Ver si asignacion realizada va aca o solo por el algoritmo
    // Agregar url de foto a necesidad al confirmar entrega
    switch (request.estado()) {
      case TipoEstadoDonacion.ASIGNACION_REALIZADA -> donacion.asignar(actor, null);
      case TipoEstadoDonacion.VENCIDA -> donacion.vencer(actor);
      case TipoEstadoDonacion.EN_TRASLADO -> donacion.planificarRuta(actor);
      case TipoEstadoDonacion.LISTA_PARA_ENTREGAR -> donacion.iniciarRecorrido(actor);
      case TipoEstadoDonacion.ENTREGADA -> donacion.confirmarEntrega(actor);
      case TipoEstadoDonacion.ENTREGA_FALLIDA -> donacion.registrarFalla(
          request.justificacion(), actor);
      case TipoEstadoDonacion.EN_DEPOSITO -> donacion.retornar(actor);
      default -> throw new IllegalArgumentException("Estado inválido: " + request.estado());
    }

    repositorio.save(donacion);
    return toDTO(donacion);
  }

  private static DonacionIndependienteResponseDTO toDTO(DonacionIndependiente donacion) {
    return new DonacionIndependienteResponseDTO(
        donacion.getId(),
        donacion.getEstadoActual().getClass().getSimpleName(),
        donacion.getHistorial().stream()
            .map(c -> c.getEstadoNuevo().getClass().getSimpleName())
            .toList());
  }
}
