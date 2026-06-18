package grupo5.donaciones.services.mappers;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.dto.personas.*;
import grupo5.donaciones.dto.replicas.MedioDeContactoReplicaDTO;
import grupo5.donaciones.dto.replicas.PersonaReplicaDTO;
import grupo5.donaciones.models.entities.personas.*;
import grupo5.donaciones.models.entities.personas.factories.PersonaFactory;
import java.util.List;
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
        persona.getMediosDeContacto().stream().map(this::mapMedioToReplica).toList();

    return new PersonaReplicaDTO(persona.getId(), denominacion, persona.getTipoPersona(), medios);
  }

  private MedioDeContactoReplicaDTO mapMedioToReplica(MedioDeContacto medio) {
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

    if (entity instanceof Humana h && input instanceof HumanaInputDTO hi) {
      h.setNombre(hi.nombre());
      h.setApellido(hi.apellido());
      h.setFechaNacimiento(hi.fechaNacimiento());
      h.setGenero(hi.genero());
    } else if (entity instanceof Juridica j && input instanceof JuridicaInputDTO ji) {
      j.setRazonSocial(ji.razonSocial());
      j.setTipo(ji.tipoJuridico());
      j.setRubro(ji.rubro());
      j.getRepresentantes().clear();
      if (ji.representantes() != null) {
        ji.representantes().forEach(repInput -> j.agregarRepresentante(toHumanaEntity(repInput)));
      }
    } else {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
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
}
