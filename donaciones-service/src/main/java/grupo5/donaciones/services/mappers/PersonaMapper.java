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
    String tipo = obtenerTipoPersona(fila);
    String nombreRazonSocial = obtenerNombreRazonSocial(fila);

    Persona persona =
        "JURIDICA".equals(tipo)
            ? crearPersonaJuridica(nombreRazonSocial)
            : crearPersonaHumana(fila, nombreRazonSocial);

    asignarDocumento(persona, fila);
    asignarMediosDeContacto(persona, fila);
    return persona;
  }

  private static String obtenerTipoPersona(Map<String, String> fila) {
    String tipo = obtenerValor(fila, "TIPO_PERSONA", "TipoPersona", "tipoPersona", "tipo_persona");
    return (tipo == null || tipo.isBlank()) ? "HUMANA" : tipo.trim().toUpperCase();
  }

  private static String obtenerNombreRazonSocial(Map<String, String> fila) {
    return obtenerValor(
        fila,
        "NOMBRE_RAZON_SOCIAL",
        "Nombre/Razón Social",
        "Nombre/Razon Social",
        "Nombre / Razón Social",
        "RAZON_SOCIAL",
        "Razon Social",
        "NOMBRE",
        "Nombre");
  }

  private static Persona crearPersonaJuridica(String nombreRazonSocial) {
    String razonSocial =
        (nombreRazonSocial != null && !nombreRazonSocial.isBlank())
            ? nombreRazonSocial
            : "Empresa S.A.";
    Humana representanteDefault =
        new Humana("Representante", "Legal", LocalDate.now(ZoneId.of("UTC")));
    return PersonaFactory.crearJuridica(
        razonSocial, TipoJuridico.EMPRESA, "Rubro CSV", representanteDefault);
  }

  private static Persona crearPersonaHumana(Map<String, String> fila, String nombreRazonSocial) {
    String[] nombreApellido = resolverNombreYApellido(fila, nombreRazonSocial);
    LocalDate fecha = parsearFechaNacimiento(fila);
    return PersonaFactory.crearHumana(nombreApellido[0], nombreApellido[1], fecha, null);
  }

  private static String[] resolverNombreYApellido(
      Map<String, String> fila, String nombreRazonSocial) {
    String nombre = obtenerValor(fila, "NOMBRE", "Nombre", "nombre");
    String apellido = obtenerValor(fila, "APELLIDO", "Apellido", "apellido");

    if ((nombre == null || nombre.isBlank())
        && (nombreRazonSocial != null && !nombreRazonSocial.isBlank())) {
      int lastSpace = nombreRazonSocial.lastIndexOf(' ');
      if (lastSpace > 0) {
        nombre = nombreRazonSocial.substring(0, lastSpace).trim();
        apellido = nombreRazonSocial.substring(lastSpace + 1).trim();
      } else {
        nombre = nombreRazonSocial;
        apellido = "-";
      }
    }

    String nombreFinal = (nombre != null && !nombre.isBlank()) ? nombre : "Donante";
    String apellidoFinal = (apellido != null && !apellido.isBlank()) ? apellido : "Anonimo";
    return new String[] {nombreFinal, apellidoFinal};
  }

  private static LocalDate parsearFechaNacimiento(Map<String, String> fila) {
    String fechaStr = obtenerValor(fila, "FECHA_NACIMIENTO", "FechaNacimiento", "Fecha Nacimiento");
    if (fechaStr != null && !fechaStr.isBlank()) {
      return LocalDate.parse(fechaStr.trim());
    }
    return null;
  }

  private static void asignarDocumento(Persona persona, Map<String, String> fila) {
    String tipoDocStr =
        obtenerValor(
            fila, "TIPO_DOCUMENTO", "TIPO_DOC", "TipoDoc", "tipoDoc", "TipoDoc.", "Tipo Documento");
    String documento =
        obtenerValor(fila, DOCUMENTO_KEY, "Documento", "documento", "Doc", "Nro Documento");

    if (documento == null || documento.isBlank()) {
      return;
    }

    TipoDocumento tipoDoc = parsearTipoDocumento(tipoDocStr);
    persona.actualizarDocumento(tipoDoc, documento.trim());
  }

  private static TipoDocumento parsearTipoDocumento(String tipoDocStr) {
    if (tipoDocStr == null || tipoDocStr.isBlank()) {
      return null;
    }
    try {
      return TipoDocumento.valueOf(tipoDocStr.trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private static void asignarMediosDeContacto(Persona persona, Map<String, String> fila) {
    persona.limpiarMediosDeContacto();

    String email = obtenerValor(fila, "EMAIL", "Email", "email", "Correo", "correo");
    if (email != null && !email.isBlank()) {
      Correo correo = new Correo();
      correo.setDireccionCorreo(email.trim());
      correo.setEsPredeterminado(true);
      persona.agregarMedioDeContacto(correo);
    }

    String telephone =
        obtenerValor(fila, "TELEFONO", "Teléfono", "Telefono", "telefono", "teléfono", "Celular");
    if (telephone != null && !telephone.isBlank()) {
      Telefono tel = new Telefono();
      tel.setNumero(telephone.trim());
      if (persona.getMediosDeContacto().isEmpty()) {
        tel.setEsPredeterminado(true);
      }
      persona.agregarMedioDeContacto(tel);
    }
  }

  private static String obtenerValor(Map<String, String> fila, String... claves) {
    if (fila == null) return null;
    for (String clave : claves) {
      if (fila.containsKey(clave) && fila.get(clave) != null && !fila.get(clave).isBlank()) {
        return fila.get(clave).trim();
      }
    }
    return null;
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
      case Correo c ->
          new MedioDeContactoReplicaDTO(
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
      default ->
          throw new IllegalArgumentException(
              "Medio de contacto no soportado: " + m.getClass().getSimpleName());
    };
  }
}
