package grupo5.donaciones.services;

import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.donaciones.dto.NecesidadDTO;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import grupo5.donaciones.models.entities.necesidades.NecesidadExtraordinaria;
import grupo5.donaciones.models.entities.necesidades.NecesidadRecurrente;
import grupo5.donaciones.models.repositories.INecesidadesRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NecesidadesService implements INecesidadesService {

  @Autowired private INecesidadesRepository necesidadRepository;

  @Override
  public NecesidadDTO guardar(NecesidadDTO dto) {
    Necesidad necesidadDominio = convertirDTOANecesidad(dto);
    dto.setEstaSatisfecha(necesidadDominio.estaSatisfecha());
    necesidadRepository.save(dto.getId(), dto);
    return dto;
  }

  @Override
  public NecesidadDTO obtenerPorId(UUID id) {
    return necesidadRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(id));
  }

  public List<NecesidadDTO> listarConFiltros(UUID entidadId, String tipo) {
    List<NecesidadDTO> todas = necesidadRepository.findAll();

    return todas.stream()
        .filter(
            n ->
                entidadId == null
                    || (n.getEntidadId() != null && n.getEntidadId().equals(entidadId)))
        .filter(n -> tipo == null || tipo.equalsIgnoreCase(n.getTipo()))
        .collect(Collectors.toList());
  }

  // MAPPER INPUT (dto -> dominio)
  private Necesidad convertirDTOANecesidad(NecesidadDTO dto) {

    String nombreSubcat =
        dto.getSubcategoriaNombre() != null ? dto.getSubcategoriaNombre() : "General";
    Subcategoria subcategoriaReal = new Subcategoria(null, nombreSubcat);

    if ("RECURRENTE".equalsIgnoreCase(dto.getTipo())) {
      java.time.Period frecuenciaJava =
          java.time.Period.between(dto.getFechaInicio(), dto.getFechaFin());
      return new NecesidadRecurrente(
          subcategoriaReal,
          dto.getCantidadNecesitada(),
          dto.getDescripcion(),
          frecuenciaJava,
          dto.getFechaInicio());
    } else {
      return new NecesidadExtraordinaria(
          subcategoriaReal, dto.getCantidadNecesitada(), dto.getDescripcion());
    }
  }
}
