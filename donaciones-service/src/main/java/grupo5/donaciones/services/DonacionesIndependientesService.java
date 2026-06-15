package grupo5.donaciones.services;

import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.donaciones.dto.donacionesIndependientes.CambioEstadoDonacionIndependienteRequestDTO;
import grupo5.donaciones.dto.donacionesIndependientes.DonacionIndependienteResponseDTO;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
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

    switch (request.estado().toUpperCase()) {
      case "ASIGNADA" -> donacion.asignar(actor);
      case "VENCIDA" -> donacion.vencer(actor);
      case "EN_TRASLADO" -> donacion.planificarRuta(actor);
      case "RECORRIDO" -> donacion.iniciarRecorrido(actor);
      case "ENTREGADA" -> donacion.confirmarEntrega(actor);
      case "ENTREGA_FALLIDA" -> donacion.registrarFalla(request.justificacion(), actor);
      case "EN_DEPOSITO" -> donacion.retornar(actor);
      default -> throw new IllegalArgumentException("Estado inválido: " + request.estado());
    }

    repositorio.save(id, donacion);
    return toDTO(donacion);
  }

  private DonacionIndependienteResponseDTO toDTO(DonacionIndependiente donacion) {
    return new DonacionIndependienteResponseDTO(
        donacion.getId(),
        donacion.getEstadoActual().getClass().getSimpleName(),
        donacion.getHistorial().stream()
            .map(c -> c.getEstado().getClass().getSimpleName())
            .toList());
  }
}
