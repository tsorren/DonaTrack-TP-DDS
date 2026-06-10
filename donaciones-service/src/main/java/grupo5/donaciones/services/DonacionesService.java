package grupo5.donaciones.services;

import grupo5.common.errors.RecursoNoEncontradoException;
import grupo5.donaciones.dto.donaciones.ActualizarDonacionDTO;
import grupo5.donaciones.dto.donaciones.CrearDonacionDTO;
import grupo5.donaciones.dto.donaciones.DonacionDTO;
import grupo5.donaciones.repositories.DonacionesRepository;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

@Service
public class DonacionesService {

  private static final String ESTADO_INICIAL = "EN_DEPOSITO";

  private final DonacionesRepository donacionesRepository;
  private final AtomicLong secuenciaIds = new AtomicLong(1);

  public DonacionesService(DonacionesRepository donacionesRepository) {
    this.donacionesRepository = donacionesRepository;
  }

  public List<DonacionDTO> buscarTodas() {
    return donacionesRepository.buscarTodas();
  }

  public DonacionDTO buscarPorId(Long id) {
    return donacionesRepository
        .buscarPorId(id)
        .orElseThrow(() -> new RecursoNoEncontradoException("No existe una donación con id " + id));
  }

  public DonacionDTO crear(CrearDonacionDTO dto) {
    DonacionDTO donacion =
        new DonacionDTO(
            secuenciaIds.getAndIncrement(),
            dto.donanteId(),
            dto.descripcion(),
            ESTADO_INICIAL,
            dto.bienes());

    return donacionesRepository.guardar(donacion);
  }

  public DonacionDTO actualizar(Long id, ActualizarDonacionDTO dto) {
    DonacionDTO donacionActual = buscarPorId(id);

    DonacionDTO donacionActualizada =
        new DonacionDTO(
            id, dto.donanteId(), dto.descripcion(), donacionActual.estado(), dto.bienes());

    return donacionesRepository.guardar(donacionActualizada);
  }

  public void eliminar(Long id) {
    buscarPorId(id);
    donacionesRepository.eliminarPorId(id);
  }
}
