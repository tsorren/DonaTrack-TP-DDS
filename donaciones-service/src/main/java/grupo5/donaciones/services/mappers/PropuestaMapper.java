package grupo5.donaciones.services.mappers;

import grupo5.donaciones.dto.propuestas.GrupoFragmentacionDTO;
import grupo5.donaciones.dto.propuestas.NecesidadResumenDTO;
import grupo5.donaciones.dto.propuestas.PropuestaDTO;
import grupo5.donaciones.models.entities.propuestas.PosibleFragmentacion;
import grupo5.donaciones.models.entities.propuestas.Propuesta;
import grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository;
import grupo5.donaciones.models.repositories.INecesidadesRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PropuestaMapper {

  private final INecesidadesRepository necesidadRepository;
  private final IDonacionesIndependientesRepository donacionRepository;
  private final DonacionIndependienteMapper diMapper;

  public PropuestaMapper(
      INecesidadesRepository necesidadRepository,
      IDonacionesIndependientesRepository donacionRepository,
      DonacionIndependienteMapper diMapper) {
    this.necesidadRepository = necesidadRepository;
    this.donacionRepository = donacionRepository;
    this.diMapper = diMapper;
  }

  public PropuestaDTO toDTO(Propuesta propuesta) {
    if (propuesta == null) {
      return null;
    }

    NecesidadResumenDTO necesidadDTO = null;
    if (propuesta.getNecesidadQueSatisfaceId() != null) {
      necesidadDTO =
          necesidadRepository
              .findById(propuesta.getNecesidadQueSatisfaceId())
              .map(
                  n ->
                      new NecesidadResumenDTO(
                          n.getId(),
                          n.getDescripcion(),
                          n.getCantidadNecesitada(),
                          n.getClass().getSimpleName().replace("Necesidad", "").toUpperCase(),
                          Boolean.TRUE.equals(n.estaSatisfecha()) ? "SATISFECHA" : "PENDIENTE"))
              .orElse(null);
    }

    List<GrupoFragmentacionDTO> fragmentacionesDTO = List.of();
    if (propuesta.getPosiblesFragmentaciones() != null) {
      Map<UUID, Integer> agrupado = new LinkedHashMap<>();
      for (PosibleFragmentacion f : propuesta.getPosiblesFragmentaciones()) {
        agrupado.merge(f.getDonacionOriginalId(), f.getCantidadNecesaria(), Integer::sum);
      }

      fragmentacionesDTO =
          agrupado.entrySet().stream()
              .map(
                  entry -> {
                    UUID diId = entry.getKey();
                    Integer cantidad = entry.getValue();
                    var diDTO = donacionRepository.findById(diId).map(diMapper::toDTO).orElse(null);
                    return new GrupoFragmentacionDTO(diDTO, cantidad);
                  })
              .toList();
    }

    return new PropuestaDTO(
        propuesta.getId(),
        propuesta.getEstado(),
        propuesta.getFechaCreacion(),
        necesidadDTO,
        fragmentacionesDTO);
  }
}
