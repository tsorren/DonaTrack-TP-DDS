package grupo5.notificaciones.infrastructure.persistencia.mappers;

import grupo5.notificaciones.infrastructure.persistencia.entities.CorreoEntity;
import grupo5.notificaciones.infrastructure.persistencia.entities.MedioDeContactoEntity;
import grupo5.notificaciones.infrastructure.persistencia.entities.PersonaEntity;
import grupo5.notificaciones.infrastructure.persistencia.entities.TelefonoEntity;
import grupo5.notificaciones.models.entities.personas.Correo;
import grupo5.notificaciones.models.entities.personas.MedioDeContacto;
import grupo5.notificaciones.models.entities.personas.Persona;
import grupo5.notificaciones.models.entities.personas.Telefono;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PersonaPersistenciaMapper {

  public PersonaEntity toEntity(Persona domain) {
    return toEntity(domain, null);
  }

  public PersonaEntity toEntity(Persona domain, PersonaEntity existing) {
    if (domain == null) return null;
    PersonaEntity entity = existing != null ? existing : new PersonaEntity();
    entity.setId(domain.getId());
    entity.setDenominacion(domain.getDenominacion());
    entity.setTipoPersona(domain.getTipoPersona());

    if (existing == null) {
      List<MedioDeContactoEntity> mediosEntities = new ArrayList<>();
      for (MedioDeContacto medio : domain.getMediosDeContacto()) {
        if (medio instanceof Correo correo) {
          CorreoEntity ce = new CorreoEntity();
          ce.setDireccionCorreo(correo.getDireccionCorreo());
          ce.setEsPredeterminado(correo.getEsPredeterminado());
          mediosEntities.add(ce);
        } else if (medio instanceof Telefono tel) {
          TelefonoEntity te = new TelefonoEntity();
          te.setCaracteristica(tel.getCaracteristica());
          te.setCodigoArea(tel.getCodigoArea());
          te.setNumero(tel.getNumero());
          te.setTipo(tel.getTipo());
          te.setEsPredeterminado(tel.getEsPredeterminado());
          mediosEntities.add(te);
        }
      }
      entity.setMediosDeContacto(mediosEntities);
      return entity;
    }

    // Sincronización diferencial in-place sin clear() (ADR 20260902)
    List<MedioDeContactoEntity> currentMedios = entity.getMediosDeContacto();
    if (currentMedios == null) {
      currentMedios = new ArrayList<>();
      entity.setMediosDeContacto(currentMedios);
    }

    // 1. Eliminar medios que ya no están en el dominio
    currentMedios.removeIf(
        existingMedio -> !isMedioPresentInDomain(existingMedio, domain.getMediosDeContacto()));

    // 2. Actualizar existentes o agregar nuevos
    for (MedioDeContacto medio : domain.getMediosDeContacto()) {
      if (medio instanceof Correo correo) {
        CorreoEntity ce = findExistingCorreo(currentMedios, correo.getDireccionCorreo());
        if (ce != null) {
          ce.setEsPredeterminado(correo.getEsPredeterminado());
        } else {
          CorreoEntity nuevo = new CorreoEntity();
          nuevo.setDireccionCorreo(correo.getDireccionCorreo());
          nuevo.setEsPredeterminado(correo.getEsPredeterminado());
          currentMedios.add(nuevo);
        }
      } else if (medio instanceof Telefono tel) {
        TelefonoEntity te = findExistingTelefono(currentMedios, tel);
        if (te != null) {
          te.setEsPredeterminado(tel.getEsPredeterminado());
        } else {
          TelefonoEntity nuevo = new TelefonoEntity();
          nuevo.setCaracteristica(tel.getCaracteristica());
          nuevo.setCodigoArea(tel.getCodigoArea());
          nuevo.setNumero(tel.getNumero());
          nuevo.setTipo(tel.getTipo());
          nuevo.setEsPredeterminado(tel.getEsPredeterminado());
          currentMedios.add(nuevo);
        }
      }
    }

    return entity;
  }

  public Persona toDomain(PersonaEntity entity) {
    if (entity == null) return null;

    List<MedioDeContacto> mediosDomain = new ArrayList<>();

    for (MedioDeContactoEntity me : entity.getMediosDeContacto()) {
      if (me instanceof CorreoEntity ce) {
        Correo c = new Correo();
        c.setDireccionCorreo(ce.getDireccionCorreo());
        if (Boolean.TRUE.equals(ce.getEsPredeterminado())) {
          c.marcarComoPredeterminado();
        }
        mediosDomain.add(c);
      } else if (me instanceof TelefonoEntity te) {
        Telefono t = new Telefono();
        t.setCaracteristica(te.getCaracteristica());
        t.setCodigoArea(te.getCodigoArea());
        t.setNumero(te.getNumero());
        t.setTipo(te.getTipo());
        if (Boolean.TRUE.equals(te.getEsPredeterminado())) {
          t.marcarComoPredeterminado();
        }
        mediosDomain.add(t);
      }
    }
    return new Persona(
        entity.getId(), mediosDomain, entity.getDenominacion(), entity.getTipoPersona());
  }

  private static boolean isMedioPresentInDomain(
      MedioDeContactoEntity entityMedio, List<MedioDeContacto> domainMedios) {
    if (domainMedios == null) return false;
    if (entityMedio instanceof CorreoEntity ce) {
      return domainMedios.stream()
          .filter(Correo.class::isInstance)
          .map(Correo.class::cast)
          .anyMatch(c -> matchesCorreo(ce.getDireccionCorreo(), c.getDireccionCorreo()));
    } else if (entityMedio instanceof TelefonoEntity te) {
      return domainMedios.stream()
          .filter(Telefono.class::isInstance)
          .map(Telefono.class::cast)
          .anyMatch(t -> matchesTelefono(te, t));
    }
    return false;
  }

  private static CorreoEntity findExistingCorreo(
      List<MedioDeContactoEntity> medios, String direccionCorreo) {
    if (medios == null || direccionCorreo == null) return null;
    return medios.stream()
        .filter(CorreoEntity.class::isInstance)
        .map(CorreoEntity.class::cast)
        .filter(c -> matchesCorreo(c.getDireccionCorreo(), direccionCorreo))
        .findFirst()
        .orElse(null);
  }

  private static TelefonoEntity findExistingTelefono(
      List<MedioDeContactoEntity> medios, Telefono tel) {
    if (medios == null || tel == null) return null;
    return medios.stream()
        .filter(TelefonoEntity.class::isInstance)
        .map(TelefonoEntity.class::cast)
        .filter(t -> matchesTelefono(t, tel))
        .findFirst()
        .orElse(null);
  }

  private static boolean matchesCorreo(String email1, String email2) {
    if (email1 == null || email2 == null) return false;
    return email1.trim().equalsIgnoreCase(email2.trim());
  }

  private static boolean matchesTelefono(TelefonoEntity te, Telefono tel) {
    if (te == null || tel == null) return false;
    return java.util.Objects.equals(te.getCaracteristica(), tel.getCaracteristica())
        && java.util.Objects.equals(te.getCodigoArea(), tel.getCodigoArea())
        && java.util.Objects.equals(te.getNumero(), tel.getNumero())
        && java.util.Objects.equals(te.getTipo(), tel.getTipo());
  }
}
