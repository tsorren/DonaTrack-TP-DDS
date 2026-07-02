package grupo5.donaciones.services.mappers;

import grupo5.donaciones.dto.direcciones.DireccionInputDTO;
import grupo5.donaciones.dto.direcciones.DireccionOutputDTO;
import grupo5.donaciones.models.entities.ubicaciones.Direccion;
import grupo5.donaciones.models.entities.ubicaciones.Localidad;
import grupo5.donaciones.models.entities.ubicaciones.Pais;
import grupo5.donaciones.models.entities.ubicaciones.Provincia;
import org.springframework.stereotype.Component;

@Component
public class DireccionMapper {

  public Direccion toEntity(DireccionInputDTO dto) {
    if (dto == null) {
      return null;
    }
    Pais pais = new Pais(dto.pais());
    Provincia provincia = new Provincia(dto.provincia(), pais);
    Localidad localidad = new Localidad(dto.localidad(), provincia);

    return new Direccion(
        dto.calle(), dto.altura(), dto.piso(), dto.departamento(), dto.codigoPostal(), localidad);
  }

  public DireccionOutputDTO toOutputDTO(Direccion entity) {
    if (entity == null) {
      return null;
    }
    String localidadNombre = entity.localidad() != null ? entity.localidad().nombre() : null;
    String provinciaNombre =
        (entity.localidad() != null && entity.localidad().provincia() != null)
            ? entity.localidad().provincia().nombre()
            : null;
    String paisNombre =
        (entity.localidad() != null
                && entity.localidad().provincia() != null
                && entity.localidad().provincia().pais() != null)
            ? entity.localidad().provincia().pais().nombre()
            : null;

    return new DireccionOutputDTO(
        entity.calle(),
        entity.altura(),
        entity.piso(),
        entity.departamento(),
        entity.codigoPostal(),
        localidadNombre,
        provinciaNombre,
        paisNombre);
  }
}
