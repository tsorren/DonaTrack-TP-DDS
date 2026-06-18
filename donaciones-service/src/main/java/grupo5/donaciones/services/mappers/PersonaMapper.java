package grupo5.donaciones.services.mappers;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.dto.comunicaciones.MedioDeContactoReplicaDTO;
import grupo5.donaciones.dto.comunicaciones.PersonaReplicaDTO;
import grupo5.donaciones.dto.personas.*;
import grupo5.donaciones.models.entities.personas.*;
import grupo5.donaciones.models.entities.personas.factories.PersonaFactory;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PersonaMapper {

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
      case JuridicaInputDTO j -> {
        if (j.representantes() == null || j.representantes().isEmpty()) {
          throw new ValidationException(ErrorCatalog.JURIDICA_SIN_REPRESENTANTE_INICIAL);
        }

        Humana representanteInicial = toHumanaEntity(j.representantes().getFirst());

        Juridica juridica =
            PersonaFactory.crearJuridica(
                j.razonSocial(), j.tipoJuridico(), j.rubro(), representanteInicial);

        for (int i = 1; i < j.representantes().size(); i++) {
          Humana extra = toHumanaEntity(j.representantes().get(i));
          juridica.agregarRepresentante(extra);
        }

        populateCommonFields(juridica, j);
        yield juridica;
      }
    };
  }

  public PersonaOutputDTO toOutputDTO(Persona entity) {
    if (entity == null) {
      return null;
    }

    return switch (entity) {
      case Humana h -> toHumanaOutputDTO(h);
      case Juridica j -> new JuridicaOutputDTO(
          TipoPersona.JURIDICA,
          j.getId(),
          j.getTipoDocumento(),
          j.getDocumento(),
          direccionMapper.toOutputDTO(j.getDireccion()),
          j.getMediosDeContacto().stream().map(medioDeContactoMapper::toOutputDTO).toList(),
          j.getRazonSocial(),
          j.getTipo(),
          j.getRubro(),
          j.getRepresentantes().stream().map(this::toHumanaOutputDTO).toList());
    };
  }

  public PersonaReplicaDTO toReplicaDTO(Persona persona) {
    if (persona == null) {
      return null;
    }

    String denominacion =
        switch (persona) {
          case Humana h -> h.getNombre() + " " + h.getApellido();
          case Juridica j -> j.getRazonSocial();
        };

    List<MedioDeContactoReplicaDTO> medios =
        persona.getMediosDeContacto().stream().map(PersonaMapper::mapMedioToReplica).toList();

    return new PersonaReplicaDTO(persona.getId(), denominacion, persona.getTipoPersona(), medios);
  }

  private static MedioDeContactoReplicaDTO mapMedioToReplica(MedioDeContacto medio) {
    return switch (medio) {
      case Correo c -> new MedioDeContactoReplicaDTO(
          "CORREO", c.getEsPredeterminado(), c.getDireccionCorreo(), null, null, null);
      case WhatsApp w -> new MedioDeContactoReplicaDTO(
          "WHATSAPP",
          w.getEsPredeterminado(),
          null,
          w.getCaracteristica(),
          w.getCodigoArea(),
          w.getNumero());
      case Telefono t -> new MedioDeContactoReplicaDTO(
          "TELEFONO",
          t.getEsPredeterminado(),
          null,
          t.getCaracteristica(),
          t.getCodigoArea(),
          t.getNumero());
      default -> throw new IllegalArgumentException("Medio de contacto no soportado");
    };
  }

  private Humana toHumanaEntity(HumanaInputDTO h) {
    Humana humana =
        PersonaFactory.crearHumana(h.nombre(), h.apellido(), h.fechaNacimiento(), h.genero());
    populateCommonFields(humana, h);
    return humana;
  }

  private void populateCommonFields(Persona persona, PersonaInputDTO input) {
    persona.setTipoDocumento(input.tipoDocumento());
    persona.setDocumento(input.documento());
    persona.setDireccion(direccionMapper.toEntity(input.direccion()));
    if (input.mediosDeContacto() != null) {
      input.mediosDeContacto().stream()
          .map(medioDeContactoMapper::toEntity)
          .forEach(persona::agregarMedioDeContacto);
    }
  }

  public void updateEntity(Persona entity, PersonaInputDTO input) {
    if (entity == null || input == null) {
      return;
    }

    switch (entity) {
      case Humana h -> {
        if (input instanceof HumanaInputDTO hi) {
          h.setNombre(hi.nombre());
          h.setApellido(hi.apellido());
          h.setFechaNacimiento(hi.fechaNacimiento());
          h.setGenero(hi.genero());
        } else {
          throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
        }
      }
      case Juridica j -> {
        if (input instanceof JuridicaInputDTO ji) {
          j.setRazonSocial(ji.razonSocial());
          j.setTipo(ji.tipoJuridico());
          j.setRubro(ji.rubro());
          j.getRepresentantes().clear();
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

    entity.setTipoDocumento(input.tipoDocumento());
    entity.setDocumento(input.documento());
    entity.setDireccion(direccionMapper.toEntity(input.direccion()));
    entity.getMediosDeContacto().clear();
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

  public Persona mapToPersona(Map<String, String> fila) {
    String tipo = fila.getOrDefault("TIPO_PERSONA", "HUMANA").toUpperCase();
    Persona persona;

    if ("JURIDICA".equals(tipo)) {
      String razonSocial = fila.get("RAZON_SOCIAL");
      // Crear un representante por defecto para cumplir con la validación del dominio.
      Humana representanteDefault = new Humana("Representante", "Legal", LocalDate.now());
      persona = PersonaFactory.crearJuridica(razonSocial, null, null, representanteDefault);
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

    if (fila.containsKey("TIPO_DOCUMENTO") && fila.containsKey("DOCUMENTO")) {
      String tipoDocStr = fila.get("TIPO_DOCUMENTO");
      if (tipoDocStr != null && !tipoDocStr.isBlank()) {
        persona.setTipoDocumento(TipoDocumento.valueOf(tipoDocStr.toUpperCase()));
      }
      persona.setDocumento(fila.get("DOCUMENTO"));
    }

    String email = fila.get("EMAIL");
    if (email != null && !email.isBlank()) {
      Correo correo = new Correo();
      correo.setDireccionCorreo(email.trim());
      persona.agregarMedioDeContacto(correo);
    }

    String telefono = fila.get("TELEFONO");
    if (telefono != null && !telefono.isBlank()) {
      Telefono tel = new Telefono();
      tel.setNumero(telefono.trim());
      persona.agregarMedioDeContacto(tel);
    }

    return persona;
  }
}
