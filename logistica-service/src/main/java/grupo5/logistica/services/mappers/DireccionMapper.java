package grupo5.logistica.services.mappers;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.dto.rutas.DireccionDTO;
import grupo5.logistica.models.entities.rutas.direccion.CoordenadasGeograficas;
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
    CoordenadasGeograficas coordenadas = mapearCoordenadas(dto.latitud(), dto.longitud());

    return new Direccion(
        dto.calle(),
        dto.altura(),
        dto.piso(),
        dto.departamento(),
        dto.codigoPostal(),
        localidad,
        coordenadas);
  }

  public DireccionDTO toResponseDTO(Direccion direccion) {
    if (direccion == null) {
      return null;
    }

    CoordenadasGeograficas coords = direccion.coordenadas();
    return new DireccionDTO(
        direccion.calle(),
        direccion.altura(),
        direccion.piso(),
        direccion.departamento(),
        direccion.codigoPostal(),
        direccion.localidad().nombre(),
        direccion.localidad().provincia().nombre(),
        direccion.localidad().provincia().pais().nombre(),
        coords != null ? coords.latitud() : null,
        coords != null ? coords.longitud() : null);
  }

  private static CoordenadasGeograficas mapearCoordenadas(Double latitud, Double longitud) {
    if (latitud == null && longitud == null) {
      return null;
    }
    if (latitud == null || longitud == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }
    return new CoordenadasGeograficas(latitud, longitud);
  }
}
