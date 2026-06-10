package grupo5.donaciones.services;

import grupo5.common.errors.RecursoNoEncontradoException;
import grupo5.donaciones.dto.donantes.ActualizarDonanteRequest;
import grupo5.donaciones.dto.donantes.CrearDonanteRequest;
import grupo5.donaciones.dto.donantes.DonanteDTO;
import grupo5.donaciones.repositories.DonantesRepository;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

@Service
public class DonantesService {

  private final DonantesRepository donantesRepository;
  private final AtomicLong secuenciaIds = new AtomicLong(1);

  public DonantesService(DonantesRepository donantesRepository) {
    this.donantesRepository = donantesRepository;
  }

  public List<DonanteDTO> buscarTodos() {
    return donantesRepository.buscarTodos();
  }

  public DonanteDTO buscarPorId(Long id) {
    return donantesRepository
        .buscarPorId(id)
        .orElseThrow(() -> new RecursoNoEncontradoException("No existe un donante con id " + id));
  }

  public DonanteDTO crear(CrearDonanteRequest request) {
    DonanteDTO donante =
        new DonanteDTO(
            secuenciaIds.getAndIncrement(),
            request.tipoPersona(),
            request.tipoDocumento(),
            request.documento(),
            request.denominacion(),
            request.email(),
            request.telefono());

    return donantesRepository.guardar(donante);
  }

  public DonanteDTO actualizar(Long id, ActualizarDonanteRequest request) {
    buscarPorId(id);

    DonanteDTO donanteActualizado =
        new DonanteDTO(
            id,
            request.tipoPersona(),
            request.tipoDocumento(),
            request.documento(),
            request.denominacion(),
            request.email(),
            request.telefono());

    return donantesRepository.guardar(donanteActualizado);
  }

  public void eliminar(Long id) {
    buscarPorId(id);
    donantesRepository.eliminarPorId(id);
  }
}
