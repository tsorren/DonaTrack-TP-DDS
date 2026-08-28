package grupo5.notificaciones.services.mappers;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.notificaciones.dto.MedioDeContactoReplicaDTO;
import grupo5.notificaciones.models.entities.personas.Correo;
import grupo5.notificaciones.models.entities.personas.MedioDeContacto;
import grupo5.notificaciones.models.entities.personas.Telefono;
import grupo5.notificaciones.models.entities.personas.TipoTelefono;
import org.springframework.stereotype.Component;

@Component
public class MedioDeContactoMapper {

  public MedioDeContacto toEntity(MedioDeContactoReplicaDTO dto) {
    if (dto == null) {
      return null;
    }
    MedioDeContacto medio =
        switch (dto.tipo().toUpperCase()) {
          case "CORREO" -> {
            Correo correo = new Correo();
            correo.setDireccionCorreo(dto.direccionCorreo());
            yield correo;
          }
          case "TELEFONO" -> populateTelefono(
              new Telefono(),
              dto.caracteristica(),
              dto.codigoArea(),
              dto.numero(),
              TipoTelefono.ESTANDAR);
          case "WHATSAPP" -> populateTelefono(
              new Telefono(),
              dto.caracteristica(),
              dto.codigoArea(),
              dto.numero(),
              TipoTelefono.WHATSAPP);
          default -> throw new ValidationException(
              ErrorCatalog.MEDIO_DE_CONTACTO_TIPO_NO_SOPORTADO);
        };
    // null se trata como "no predeterminado" (Oleada 1, RF-01): un DTO sin este dato no debe dejar
    // el medio en un estado ambiguo, y coincide con el default del constructor de MedioDeContacto.
    if (Boolean.TRUE.equals(dto.esPredeterminado())) {
      medio.marcarComoPredeterminado();
    } else {
      medio.desmarcarComoPredeterminado();
    }
    return medio;
  }

  public MedioDeContactoReplicaDTO toReplicaDTO(MedioDeContacto entity) {
    if (entity == null) {
      return null;
    }
    return switch (entity) {
      case Correo c -> new MedioDeContactoReplicaDTO(
          "CORREO", c.getEsPredeterminado(), c.getDireccionCorreo(), null, null, null);
      case Telefono t -> {
        if (t.getTipo() == TipoTelefono.WHATSAPP) {
          yield new MedioDeContactoReplicaDTO(
              "WHATSAPP",
              t.getEsPredeterminado(),
              null,
              t.getCaracteristica(),
              t.getCodigoArea(),
              t.getNumero());
        } else {
          yield new MedioDeContactoReplicaDTO(
              "TELEFONO",
              t.getEsPredeterminado(),
              null,
              t.getCaracteristica(),
              t.getCodigoArea(),
              t.getNumero());
        }
      }
      default -> throw new ValidationException(ErrorCatalog.MEDIO_DE_CONTACTO_TIPO_NO_SOPORTADO);
    };
  }

  private static Telefono populateTelefono(
      Telefono tel, String caracteristica, String codigoArea, String numero, TipoTelefono tipo) {
    tel.setCaracteristica(caracteristica);
    tel.setCodigoArea(codigoArea);
    tel.setNumero(numero);
    tel.setTipo(tipo);
    return tel;
  }
}
