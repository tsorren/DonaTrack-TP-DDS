package grupo5.logistica.services.mappers;

import grupo5.logistica.dto.rutas.DireccionDTO;
import grupo5.logistica.models.entities.rutas.direccion.Direccion;
import grupo5.logistica.models.entities.rutas.direccion.Localidad;
import grupo5.logistica.models.entities.rutas.direccion.Pais;
import grupo5.logistica.models.entities.rutas.direccion.Provincia;
import org.springframework.stereotype.Component;

@Component
public class DireccionMapper {

  public Direccion toEntity(DireccionDTO dto) {
    if (dto == null) {
      return null;
    }

    Pais pais = new Pais(dto.pais());
    Provincia provincia = new Provincia(dto.provincia(), pais);
    Localidad localidad = new Localidad(dto.localidad(), provincia);

    return new Direccion(
        dto.calle(), dto.altura(), dto.piso(), dto.departamento(), dto.codigoPostal(), localidad);
  }

  public DireccionDTO toResponseDTO(Direccion direccion) {
    if (direccion == null) {
      return null;
    }

    return new DireccionDTO(
        direccion.calle(),
        direccion.altura(),
        direccion.piso(),
        direccion.departamento(),
        direccion.codigoPostal(),
        direccion.localidad().nombre(),
        direccion.localidad().provincia().nombre(),
        direccion.localidad().provincia().pais().nombre());
  }
}
