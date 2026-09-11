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
    if (domain == null) return null;
    PersonaEntity entity = new PersonaEntity();
    entity.setId(domain.getId());
    entity.setDenominacion(domain.getDenominacion());
    entity.setTipoPersona(domain.getTipoPersona());

    List<MedioDeContactoEntity> mediosEntities = new ArrayList<>();

    for (MedioDeContacto medio : domain.getMediosDeContacto()) {

      if (medio instanceof Correo correo) {
        CorreoEntity ce = new CorreoEntity();
        ce.setEsPredeterminado(correo.getEsPredeterminado());
        ce.setDireccionCorreo(correo.getDireccionCorreo());
        mediosEntities.add(ce);
      } else if (medio instanceof Telefono tel) {
        TelefonoEntity te = new TelefonoEntity();
        te.setEsPredeterminado(tel.getEsPredeterminado());
        te.setCaracteristica(tel.getCaracteristica());
        te.setCodigoArea(tel.getCodigoArea());
        te.setNumero(tel.getNumero());
        te.setTipo(tel.getTipo());
        mediosEntities.add(te);
      }
    }
    entity.setMediosDeContacto(mediosEntities);
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
}
