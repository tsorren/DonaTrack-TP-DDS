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
    // Reutilizar la entidad si existe, o crear una nueva
    PersonaEntity entity = existing != null ? existing : new PersonaEntity();
    entity.setId(domain.getId());
    entity.setDenominacion(domain.getDenominacion());
    entity.setTipoPersona(domain.getTipoPersona());
    List<MedioDeContactoEntity> mediosEntities = new ArrayList<>();
    for (MedioDeContacto medio : domain.getMediosDeContacto()) {
      if (medio instanceof Correo correo) {
        CorreoEntity ce = (CorreoEntity) findExistingCorreo(existing, correo.getDireccionCorreo());
        if (ce == null) {
          ce = new CorreoEntity();
          ce.setDireccionCorreo(correo.getDireccionCorreo());
        }
        ce.setEsPredeterminado(correo.getEsPredeterminado());
        mediosEntities.add(ce);
      } else if (medio instanceof Telefono tel) {
        TelefonoEntity te = (TelefonoEntity) findExistingTelefono(existing, tel);
        if (te == null) {
          te = new TelefonoEntity();
          te.setCaracteristica(tel.getCaracteristica());
          te.setCodigoArea(tel.getCodigoArea());
          te.setNumero(tel.getNumero());
          te.setTipo(tel.getTipo());
        }
        te.setEsPredeterminado(tel.getEsPredeterminado());
        mediosEntities.add(te);
      }
    }

    // Importante: Conservar la referencia a la colección de Hibernate
    if (existing != null) {
      entity.getMediosDeContacto().clear();
      entity.getMediosDeContacto().addAll(mediosEntities);
    } else {
      entity.setMediosDeContacto(mediosEntities);
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

  private MedioDeContactoEntity findExistingCorreo(PersonaEntity existing, String direccionCorreo) {
    if (existing == null || existing.getMediosDeContacto() == null) return null;
    return existing.getMediosDeContacto().stream()
        .filter(m -> m instanceof CorreoEntity)
        .map(m -> (CorreoEntity) m)
        .filter(c -> c.getDireccionCorreo().equalsIgnoreCase(direccionCorreo))
        .findFirst()
        .orElse(null);
  }

  private MedioDeContactoEntity findExistingTelefono(PersonaEntity existing, Telefono tel) {
    if (existing == null || existing.getMediosDeContacto() == null) return null;
    return existing.getMediosDeContacto().stream()
        .filter(m -> m instanceof TelefonoEntity)
        .map(m -> (TelefonoEntity) m)
        .filter(
            t ->
                java.util.Objects.equals(t.getCaracteristica(), tel.getCaracteristica())
                    && java.util.Objects.equals(t.getCodigoArea(), tel.getCodigoArea())
                    && java.util.Objects.equals(t.getNumero(), tel.getNumero())
                    && t.getTipo() == tel.getTipo())
        .findFirst()
        .orElse(null);
  }
}
