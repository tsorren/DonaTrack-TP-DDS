package grupo5.donaciones.services.mappers;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.dto.comunicaciones.MedioDeContactoReplicaDTO;
import grupo5.donaciones.dto.comunicaciones.PersonaReplicaDTO;
import grupo5.donaciones.dto.personas.HumanaInputDTO;
import grupo5.donaciones.dto.personas.HumanaOutputDTO;
import grupo5.donaciones.dto.personas.JuridicaInputDTO;
import grupo5.donaciones.dto.personas.JuridicaOutputDTO;
import grupo5.donaciones.dto.personas.PersonaInputDTO;
import grupo5.donaciones.dto.personas.PersonaOutputDTO;
import grupo5.donaciones.models.entities.personas.Correo;
import grupo5.donaciones.models.entities.personas.Humana;
import grupo5.donaciones.models.entities.personas.Juridica;
import grupo5.donaciones.models.entities.personas.MedioDeContacto;
import grupo5.donaciones.models.entities.personas.Persona;
import grupo5.donaciones.models.entities.personas.Telefono;
import grupo5.donaciones.models.entities.personas.TipoDocumento;
import grupo5.donaciones.models.entities.personas.TipoJuridico;
import grupo5.donaciones.models.entities.personas.TipoPersona;
import grupo5.donaciones.models.entities.personas.TipoTelefono;
import grupo5.donaciones.models.entities.personas.factories.PersonaFactory;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PersonaMapper {

  private static final String DOCUMENTO_KEY = "DOCUMENTO";

  private final DireccionMapper direccionMapper;
  private final MedioDeContactoMapper medioDeContactoMapper;

  public PersonaMapper(
      DireccionMapper direccionMapper, MedioDeContactoMapper medioDeContactoMapper) {
    this.direccionMapper = direccionMapper;
    this.medioDeContactoMapper = medioDeContactoMapper;
  }

  public Persona toEntity(PersonaInputDTO input) {
    if (input == null) {
      return null;
    }

    return switch (input) {
      case HumanaInputDTO h -> toHumanaEntity(h);
      case JuridicaInputDTO j -> toJuridicaEntity(j);
    };
  }

  private Juridica toJuridicaEntity(JuridicaInputDTO j) {
    Humana representante =
        (j.representantes() != null && !j.representantes().isEmpty())
            ? toHumanaEntity(j.representantes().getFirst())
            : null;
    TipoJuridico tipo = j.tipoJuridico() != null ? j.tipoJuridico() : TipoJuridico.ONG;
    Juridica juridica =
        PersonaFactory.crearJuridica(j.razonSocial(), tipo, j.rubro(), representante);
    populateCommonFields(juridica, j);
    if (j.representantes() != null && j.representantes().size() > 1) {
      for (int i = 1; i < j.representantes().size(); i++) {
        juridica.agregarRepresentante(toHumanaEntity(j.representantes().get(i)));
      }
    }
    return juridica;
  }

  private Humana toHumanaEntity(HumanaInputDTO h) {
    Humana humana =
        PersonaFactory.crearHumana(h.nombre(), h.apellido(), h.fechaNacimiento(), h.genero());
    populateCommonFields(humana, h);
    return humana;
  }

  private void populateCommonFields(Persona persona, PersonaInputDTO input) {
    persona.actualizarDocumento(input.tipoDocumento(), input.documento());
    persona.actualizarDireccion(direccionMapper.toEntity(input.direccion()));
    if (input.mediosDeContacto() != null) {
      persona.limpiarMediosDeContacto();
      input.mediosDeContacto().stream()
          .map(medioDeContactoMapper::toEntity)
          .forEach(persona::agregarMedioDeContacto);
    }
  }

  public void updateEntity(Persona entity, PersonaInputDTO input) {
    if (entity == null || input == null) {
      return;
    }

    updateTypeSpecificFields(entity, input);
    updateCommonFields(entity, input);
  }

  private void updateTypeSpecificFields(Persona entity, PersonaInputDTO input) {
    switch (entity) {
      case Humana h -> {
        if (input instanceof HumanaInputDTO hi) {
          h.actualizar(hi.nombre(), hi.apellido(), hi.fechaNacimiento(), hi.genero());
        } else {
          throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
        }
      }
      case Juridica j -> {
        if (input instanceof JuridicaInputDTO ji) {
          TipoJuridico tipo = ji.tipoJuridico() != null ? ji.tipoJuridico() : j.getTipo();
          j.actualizar(ji.razonSocial(), tipo, ji.rubro());
          j.limpiarRepresentantes();
          if (ji.representantes() != null) {
            ji.representantes()
                .forEach(repInput -> j.agregarRepresentante(toHumanaEntity(repInput)));
          }
        } else {
          throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
        }
      }
      default -> throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }
  }

  private void updateCommonFields(Persona entity, PersonaInputDTO input) {
    entity.actualizarDocumento(input.tipoDocumento(), input.documento());
    entity.actualizarDireccion(direccionMapper.toEntity(input.direccion()));
    entity.limpiarMediosDeContacto();
    if (input.mediosDeContacto() != null) {
      input.mediosDeContacto().stream()
          .map(medioDeContactoMapper::toEntity)
          .forEach(entity::agregarMedioDeContacto);
    }
  }

  private HumanaOutputDTO toHumanaOutputDTO(Humana h) {
    return new HumanaOutputDTO(
        TipoPersona.HUMANA,
        h.getId(),
        h.getTipoDocumento(),
        h.getDocumento(),
        direccionMapper.toOutputDTO(h.getDireccion()),
        h.getMediosDeContacto().stream().map(medioDeContactoMapper::toOutputDTO).toList(),
        h.getNombre(),
        h.getApellido(),
        h.getGenero(),
        h.getFechaNacimiento());
  }

  private JuridicaOutputDTO toJuridicaOutputDTO(Juridica j) {
    List<HumanaOutputDTO> representantes =
        j.getRepresentantes().stream().map(this::toHumanaOutputDTO).toList();

    return new JuridicaOutputDTO(
        TipoPersona.JURIDICA,
        j.getId(),
        j.getTipoDocumento(),
        j.getDocumento(),
        direccionMapper.toOutputDTO(j.getDireccion()),
        j.getMediosDeContacto().stream().map(medioDeContactoMapper::toOutputDTO).toList(),
        j.getRazonSocial(),
        j.getTipo(),
        j.getRubro(),
        representantes);
  }

  public PersonaOutputDTO toOutputDTO(Persona entity) {
    if (entity == null) {
      return null;
    }
    return switch (entity) {
      case Humana h -> toHumanaOutputDTO(h);
      case Juridica j -> toJuridicaOutputDTO(j);
    };
  }

  public Persona mapToPersona(Map<String, String> fila) {
    String tipo = fila.getOrDefault("TIPO_PERSONA", "HUMANA").toUpperCase();
    Persona persona;

    if ("JURIDICA".equals(tipo)) {
      String razonSocial = fila.get("RAZON_SOCIAL");
      Humana representanteDefault =
          new Humana("Representante", "Legal", LocalDate.now(ZoneId.systemDefault()));
      persona =
          PersonaFactory.crearJuridica(
              razonSocial, TipoJuridico.EMPRESA, "Rubro CSV", representanteDefault);
    } else {
      String nombre = fila.get("NOMBRE");
      String apellido = fila.get("APELLIDO");
      LocalDate fecha = null;
      String fechaStr = fila.get("FECHA_NACIMIENTO");
      if (fechaStr != null && !fechaStr.isBlank()) {
        fecha = LocalDate.parse(fechaStr);
      }

      persona = PersonaFactory.crearHumana(nombre, apellido, fecha, null);
    }

    if (fila.containsKey("TIPO_DOCUMENTO") && fila.containsKey(DOCUMENTO_KEY)) {
      String tipoDocStr = fila.get("TIPO_DOCUMENTO");
      if (tipoDocStr != null && !tipoDocStr.isBlank()) {
        persona.actualizarDocumento(
            TipoDocumento.valueOf(tipoDocStr.toUpperCase()), fila.get(DOCUMENTO_KEY));
      } else {
        persona.actualizarDocumento(null, fila.get(DOCUMENTO_KEY));
      }
    }

    String email = fila.get("EMAIL");
    if (email != null && !email.isBlank()) {
      Correo correo = new Correo();
      correo.setDireccionCorreo(email.trim());
      persona.agregarMedioDeContacto(correo);
    }

    String telephone = fila.get("TELEFONO");
    if (telephone != null && !telephone.isBlank()) {
      Telefono tel = new Telefono();
      tel.setNumero(telephone.trim());
      persona.agregarMedioDeContacto(tel);
    }

    return persona;
  }

  public PersonaReplicaDTO toReplicaDTO(Persona p) {
    if (p == null) {
      return null;
    }
    String denominacion =
        switch (p) {
          case Humana h -> h.getNombre() + " " + h.getApellido();
          case Juridica j -> j.getRazonSocial();
        };

    List<MedioDeContactoReplicaDTO> medios =
        p.getMediosDeContacto().stream().map(PersonaMapper::toMedioReplicaDTO).toList();

    return new PersonaReplicaDTO(p.getId(), denominacion, p.getTipoPersona(), medios);
  }

  private static MedioDeContactoReplicaDTO toMedioReplicaDTO(MedioDeContacto m) {
    return switch (m) {
      case Correo c -> new MedioDeContactoReplicaDTO(
          "CORREO", c.getEsPredeterminado(), c.getDireccionCorreo(), null, null, null);
      case Telefono t -> {
        String tipoStr = t.getTipo() == TipoTelefono.WHATSAPP ? "WHATSAPP" : "TELEFONO";
        yield new MedioDeContactoReplicaDTO(
            tipoStr,
            t.getEsPredeterminado(),
            null,
            t.getCaracteristica(),
            t.getCodigoArea(),
            t.getNumero());
      }
      default -> throw new IllegalArgumentException(
          "Medio de contacto no soportado: " + m.getClass().getSimpleName());
    };
  }
}
