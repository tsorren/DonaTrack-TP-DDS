package grupo5.donaciones.services.mappers;

import grupo5.donaciones.dto.direcciones.DireccionInputDTO;
import grupo5.donaciones.dto.direcciones.DireccionOutputDTO;
import grupo5.donaciones.models.entities.personas.Direccion;
import grupo5.donaciones.models.entities.personas.Localidad;
import grupo5.donaciones.models.entities.personas.Pais;
import grupo5.donaciones.models.entities.personas.Provincia;
import org.springframework.stereotype.Component;

@Component
public class DireccionMapper {

  public Direccion toEntity(DireccionInputDTO dto) {
    if (dto == null) {
      return null;
    }
    Pais pais = new Pais();
    pais.setNombre(dto.pais());

    Provincia provincia = new Provincia();
    provincia.setNombre(dto.provincia());
    provincia.setPais(pais);

    Localidad localidad = new Localidad();
    localidad.setNombre(dto.localidad());
    localidad.setProvincia(provincia);

    return new Direccion(
        dto.calle(), dto.altura(), dto.piso(), dto.departamento(), dto.codigoPostal(), localidad);
  }

  public DireccionOutputDTO toOutputDTO(Direccion entity) {
    if (entity == null) {
      return null;
    }
    String localidadNombre =
        entity.getLocalidad() != null ? entity.getLocalidad().getNombre() : null;
    String provinciaNombre =
        (entity.getLocalidad() != null && entity.getLocalidad().getProvincia() != null)
            ? entity.getLocalidad().getProvincia().getNombre()
            : null;
    String paisNombre =
        (entity.getLocalidad() != null
                && entity.getLocalidad().getProvincia() != null
                && entity.getLocalidad().getProvincia().getPais() != null)
            ? entity.getLocalidad().getProvincia().getPais().getNombre()
            : null;

    return new DireccionOutputDTO(
        entity.getCalle(),
        entity.getAltura(),
        entity.getPiso(),
        entity.getDepartamento(),
        entity.getCodigoPostal(),
        localidadNombre,
        provinciaNombre,
        paisNombre);
  }
}
