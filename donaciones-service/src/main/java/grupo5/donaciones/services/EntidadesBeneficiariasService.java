package grupo5.donaciones.services;

import grupo5.common.errors.RecursoNoEncontradoException;
import grupo5.donaciones.dto.entidades.ActualizarEntidadBeneficiariaDTO;
import grupo5.donaciones.dto.entidades.CrearEntidadBeneficiariaDTO;
import grupo5.donaciones.dto.entidades.EntidadBeneficiariaDTO;
import grupo5.donaciones.repositories.EntidadesBeneficiariasRepository;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

@Service
public class EntidadesBeneficiariasService {

  private final EntidadesBeneficiariasRepository entidadesBeneficiariasRepository;
  private final AtomicLong secuenciaIds = new AtomicLong(1);

  public EntidadesBeneficiariasService(
      EntidadesBeneficiariasRepository entidadesBeneficiariasRepository) {
    this.entidadesBeneficiariasRepository = entidadesBeneficiariasRepository;
  }

  public List<EntidadBeneficiariaDTO> buscarTodas() {
    return entidadesBeneficiariasRepository.buscarTodas();
  }

  public EntidadBeneficiariaDTO buscarPorId(Long id) {
    return entidadesBeneficiariasRepository
        .buscarPorId(id)
        .orElseThrow(
            () ->
                new RecursoNoEncontradoException(
                    "No existe una entidad beneficiaria con id " + id));
  }

  public EntidadBeneficiariaDTO crear(CrearEntidadBeneficiariaDTO dto) {
    EntidadBeneficiariaDTO entidadBeneficiaria =
        new EntidadBeneficiariaDTO(
            secuenciaIds.getAndIncrement(),
            dto.razonSocial(),
            dto.direccionCompleta(),
            dto.telefono(),
            dto.correosRepresentantes());

    return entidadesBeneficiariasRepository.guardar(entidadBeneficiaria);
  }

  public EntidadBeneficiariaDTO actualizar(Long id, ActualizarEntidadBeneficiariaDTO dto) {
    buscarPorId(id);

    EntidadBeneficiariaDTO entidadBeneficiariaActualizada =
        new EntidadBeneficiariaDTO(
            id,
            dto.razonSocial(),
            dto.direccionCompleta(),
            dto.telefono(),
            dto.correosRepresentantes());

    return entidadesBeneficiariasRepository.guardar(entidadBeneficiariaActualizada);
  }

  public void eliminar(Long id) {
    buscarPorId(id);
    entidadesBeneficiariasRepository.eliminarPorId(id);
  }
}
