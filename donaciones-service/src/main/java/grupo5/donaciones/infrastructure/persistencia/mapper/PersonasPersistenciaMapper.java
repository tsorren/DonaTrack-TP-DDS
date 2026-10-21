package grupo5.donaciones.infrastructure.persistencia.mapper;

import grupo5.donaciones.infrastructure.persistencia.entities.personas.CorreoEntity;
import grupo5.donaciones.infrastructure.persistencia.entities.personas.HumanaEntity;
import grupo5.donaciones.infrastructure.persistencia.entities.personas.JuridicaEntity;
import grupo5.donaciones.infrastructure.persistencia.entities.personas.MedioDeContactoEntity;
import grupo5.donaciones.infrastructure.persistencia.entities.personas.PersonaEntity;
import grupo5.donaciones.infrastructure.persistencia.entities.personas.TelefonoEntity;
import grupo5.donaciones.models.entities.personas.Correo;
import grupo5.donaciones.models.entities.personas.Humana;
import grupo5.donaciones.models.entities.personas.Juridica;
import grupo5.donaciones.models.entities.personas.MedioDeContacto;
import grupo5.donaciones.models.entities.personas.Persona;
import grupo5.donaciones.models.entities.personas.Telefono;
import grupo5.donaciones.models.entities.personas.TipoTelefono;
import grupo5.donaciones.models.entities.ubicaciones.Direccion;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PersonasPersistenciaMapper {

  private final DireccionesPersistenciaMapper direcciones;

  public PersonasPersistenciaMapper(DireccionesPersistenciaMapper direcciones) {
    this.direcciones = direcciones;
  }

  public Persona toDomain(PersonaEntity entity) {
    if (entity instanceof HumanaEntity humana) {
      return humanaToDomain(humana);
    }
    if (entity instanceof JuridicaEntity juridica) {
      return juridicaToDomain(juridica);
    }
    throw new IllegalStateException("Tipo de persona desconocido: " + entity.getClass());
  }

  public HumanaEntity toEntity(Humana domain, HumanaEntity existing) {
    HumanaEntity entity = existing != null ? existing : new HumanaEntity();
    aplicarCamposComunes(domain, entity);
    entity.setNombre(domain.getNombre());
    entity.setApellido(domain.getApellido());
    entity.setGenero(domain.getGenero());
    entity.setFechaNacimiento(domain.getFechaNacimiento());
    return entity;
  }

  /** No toca los representantes: el adapter los sincroniza porque la FK vive del lado de humana. */
  public JuridicaEntity toEntity(Juridica domain, JuridicaEntity existing) {
    JuridicaEntity entity = existing != null ? existing : new JuridicaEntity();
    aplicarCamposComunes(domain, entity);
    entity.setRazonSocial(domain.getRazonSocial());
    entity.setTipoJuridico(domain.getTipo());
    entity.setRubro(domain.getRubro());
    return entity;
  }

  public Humana humanaToDomain(HumanaEntity entity) {
    return new Humana(
        entity.getId(),
        entity.getTipoDocumento(),
        entity.getNroDocumento(),
        direccionToDomain(entity),
        mediosToDomain(entity),
        entity.getNombre(),
        entity.getApellido(),
        entity.getGenero(),
        entity.getFechaNacimiento());
  }

  private Juridica juridicaToDomain(JuridicaEntity entity) {
    List<Humana> representantes =
        entity.getRepresentantes().stream().map(this::humanaToDomain).toList();
    return new Juridica(
        entity.getId(),
        entity.getTipoDocumento(),
        entity.getNroDocumento(),
        direccionToDomain(entity),
        mediosToDomain(entity),
        representantes,
        entity.getRazonSocial(),
        entity.getTipoJuridico(),
        entity.getRubro());
  }

  private Direccion direccionToDomain(PersonaEntity entity) {
    return entity.getDireccion() == null ? null : direcciones.toDomain(entity.getDireccion());
  }

  private List<MedioDeContacto> mediosToDomain(PersonaEntity entity) {
    return entity.getMediosDeContacto().stream().map(this::medioToDomain).toList();
  }

  private MedioDeContacto medioToDomain(MedioDeContactoEntity entity) {
    MedioDeContacto medio;
    if (entity instanceof CorreoEntity correoEntity) {
      Correo correo = new Correo();
      correo.setDireccionCorreo(correoEntity.getDireccionCorreo());
      medio = correo;
    } else if (entity instanceof TelefonoEntity telefonoEntity) {
      Telefono telefono = new Telefono();
      telefono.setCaracteristica(telefonoEntity.getCaracteristica());
      telefono.setCodigoArea(telefonoEntity.getCodigoArea());
      telefono.setNumero(telefonoEntity.getNumero());
      telefono.setTipo(telefonoEntity.getTipo());
      medio = telefono;
    } else {
      throw new IllegalStateException("Medio de contacto desconocido: " + entity.getClass());
    }
    medio.setEsPredeterminado(entity.isEsPredeterminado());
    return medio;
  }

  private void aplicarCamposComunes(Persona domain, PersonaEntity entity) {
    entity.setId(domain.getId());
    entity.setTipoDocumento(domain.getTipoDocumento());
    entity.setNroDocumento(domain.getDocumento());
    entity.setDireccion(
        domain.getDireccion() == null ? null : direcciones.resolver(domain.getDireccion()));
    reemplazarMedios(domain, entity);
  }

  // Un medio de contacto no tiene identidad ni restricciones únicas: se reemplazan todos y
  // orphanRemoval borra las filas viejas.
  private void reemplazarMedios(Persona domain, PersonaEntity entity) {
    entity.getMediosDeContacto().clear();
    for (MedioDeContacto medio : domain.getMediosDeContacto()) {
      MedioDeContactoEntity nuevo = medioToEntity(medio);
      nuevo.setPersona(entity);
      entity.getMediosDeContacto().add(nuevo);
    }
  }

  private MedioDeContactoEntity medioToEntity(MedioDeContacto medio) {
    MedioDeContactoEntity entity;
    if (medio instanceof Correo correo) {
      CorreoEntity correoEntity = new CorreoEntity();
      correoEntity.setDireccionCorreo(correo.getDireccionCorreo());
      entity = correoEntity;
    } else if (medio instanceof Telefono telefono) {
      TelefonoEntity telefonoEntity = new TelefonoEntity();
      telefonoEntity.setCaracteristica(telefono.getCaracteristica());
      telefonoEntity.setCodigoArea(telefono.getCodigoArea());
      telefonoEntity.setNumero(telefono.getNumero());
      telefonoEntity.setTipo(
          telefono.getTipo() != null ? telefono.getTipo() : TipoTelefono.ESTANDAR);
      entity = telefonoEntity;
    } else {
      throw new IllegalStateException("Medio de contacto desconocido: " + medio.getClass());
    }
    entity.setEsPredeterminado(Boolean.TRUE.equals(medio.getEsPredeterminado()));
    return entity;
  }
}
