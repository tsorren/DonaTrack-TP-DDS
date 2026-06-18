package grupo5.notificaciones.services.mappers;

import grupo5.notificaciones.dto.MedioDeContactoReplicaDTO;
import grupo5.notificaciones.models.entities.personas.*;
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
              new Telefono(), dto.caracteristica(), dto.codigoArea(), dto.numero());
          case "WHATSAPP" -> populateTelefono(
              new WhatsApp(), dto.caracteristica(), dto.codigoArea(), dto.numero());
          default -> throw new IllegalArgumentException(
              "Tipo de medio de contacto no soportado: " + dto.tipo());
        };
    medio.setEsPredeterminado(dto.esPredeterminado());
    return medio;
  }

  public MedioDeContactoReplicaDTO toReplicaDTO(MedioDeContacto entity) {
    if (entity == null) {
      return null;
    }
    return switch (entity) {
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
      default -> throw new IllegalArgumentException(
          "Tipo de medio de contacto no soportado: " + entity.getClass().getSimpleName());
    };
  }

  private Telefono populateTelefono(
      Telefono tel, String caracteristica, String codigoArea, String numero) {
    tel.setCaracteristica(caracteristica);
    tel.setCodigoArea(codigoArea);
    tel.setNumero(numero);
    return tel;
  }
}
