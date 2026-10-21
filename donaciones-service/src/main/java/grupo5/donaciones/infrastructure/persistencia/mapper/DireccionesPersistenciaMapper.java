package grupo5.donaciones.infrastructure.persistencia.mapper;

import grupo5.donaciones.infrastructure.persistencia.entities.personas.DireccionEntity;
import grupo5.donaciones.infrastructure.persistencia.repositories.SpringDataDireccionRepository;
import grupo5.donaciones.models.entities.ubicaciones.Direccion;
import grupo5.donaciones.models.entities.ubicaciones.Localidad;
import grupo5.donaciones.models.entities.ubicaciones.Pais;
import grupo5.donaciones.models.entities.ubicaciones.Provincia;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** La dirección es inmutable y compartida: "guardar" es buscar la fila igual o crearla. */
@Component
public class DireccionesPersistenciaMapper {

  private final SpringDataDireccionRepository repository;

  public DireccionesPersistenciaMapper(SpringDataDireccionRepository repository) {
    this.repository = repository;
  }

  public DireccionEntity resolver(Direccion domain) {
    Localidad localidad = domain.localidad();
    Provincia provincia = localidad.provincia();
    String pais = provincia.pais().nombre();
    return repository
        .findByCalleAndAlturaAndPisoAndDepartamentoAndCodigoPostalAndLocalidadAndProvinciaAndPais(
            domain.calle(),
            domain.altura(),
            domain.piso(),
            domain.departamento(),
            domain.codigoPostal(),
            localidad.nombre(),
            provincia.nombre(),
            pais)
        .orElseGet(() -> repository.save(nueva(domain, localidad, provincia, pais)));
  }

  public Direccion toDomain(DireccionEntity entity) {
    return new Direccion(
        entity.getCalle(),
        entity.getAltura(),
        entity.getPiso(),
        entity.getDepartamento(),
        entity.getCodigoPostal(),
        new Localidad(
            entity.getLocalidad(),
            new Provincia(entity.getProvincia(), new Pais(entity.getPais()))));
  }

  private static DireccionEntity nueva(
      Direccion domain, Localidad localidad, Provincia provincia, String pais) {
    DireccionEntity entity = new DireccionEntity();
    entity.setId(UUID.randomUUID());
    entity.setCalle(domain.calle());
    entity.setAltura(domain.altura());
    entity.setPiso(domain.piso());
    entity.setDepartamento(domain.departamento());
    entity.setCodigoPostal(domain.codigoPostal());
    entity.setLocalidad(localidad.nombre());
    entity.setProvincia(provincia.nombre());
    entity.setPais(pais);
    return entity;
  }
}
