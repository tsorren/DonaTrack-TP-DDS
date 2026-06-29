package grupo5.donaciones.services.mappers;

import grupo5.donaciones.dto.propuestas.FragmentacionDTO;
import grupo5.donaciones.dto.propuestas.NecesidadResumenDTO;
import grupo5.donaciones.dto.propuestas.PropuestaDTO;
import grupo5.donaciones.models.entities.propuestas.Propuesta;
import grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository;
import grupo5.donaciones.models.repositories.INecesidadesRepository;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PropuestaMapper {

  private final INecesidadesRepository necesidadRepository;
  private final IDonacionesIndependientesRepository donacionRepository;

  public PropuestaMapper(
      INecesidadesRepository necesidadRepository,
      IDonacionesIndependientesRepository donacionRepository) {
    this.necesidadRepository = necesidadRepository;
    this.donacionRepository = donacionRepository;
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
                          n.getId(), n.getDescripcion(), n.getCantidadNecesitada()))
              .orElse(null);
    }

    List<FragmentacionDTO> fragmentacionesDTO = List.of();
    if (propuesta.getPosiblesFragmentaciones() != null) {
      fragmentacionesDTO =
          propuesta.getPosiblesFragmentaciones().stream()
              .map(
                  f -> {
                    String desc =
                        donacionRepository
                            .findById(f.getDonacionOriginalId())
                            .map(d -> d.getDescripcion())
                            .orElse("null");
                    return new FragmentacionDTO(
                        f.getDonacionOriginalId(), f.getCantidadNecesaria(), desc);
                  })
              .toList();
    }

    return new PropuestaDTO(
        propuesta.getId(), propuesta.getEstado(), necesidadDTO, fragmentacionesDTO);
  }
}
