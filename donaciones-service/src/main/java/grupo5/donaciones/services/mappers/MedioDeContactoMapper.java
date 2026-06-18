package grupo5.donaciones.services.mappers;

import grupo5.donaciones.dto.mediosDeContacto.*;
import grupo5.donaciones.models.entities.personas.*;
import org.springframework.stereotype.Component;

@Component
public class MedioDeContactoMapper {

  public MedioDeContacto toEntity(MedioDeContactoInputDTO dto) {
    if (dto == null) {
      return null;
    }
    MedioDeContacto medio =
        switch (dto) {
          case CorreoInputDTO c -> {
            Correo correo = new Correo();
            correo.setDireccionCorreo(c.direccionCorreo());
            yield correo;
          }
          case TelefonoInputDTO t -> populateTelefono(
              new Telefono(), t.caracteristica(), t.codigoArea(), t.numero());
          case WhatsAppInputDTO w -> populateTelefono(
              new WhatsApp(), w.caracteristica(), w.codigoArea(), w.numero());
        };
    medio.setEsPredeterminado(dto.esPredeterminado());
    return medio;
  }

  public MedioDeContactoOutputDTO toOutputDTO(MedioDeContacto entity) {
    if (entity == null) {
      return null;
    }
    return switch (entity) {
      case Correo c -> new CorreoOutputDTO(c.getEsPredeterminado(), c.getDireccionCorreo());
      case WhatsApp w -> new WhatsAppOutputDTO(
          w.getEsPredeterminado(), w.getCaracteristica(), w.getCodigoArea(), w.getNumero());
      case Telefono t -> new TelefonoOutputDTO(
          t.getEsPredeterminado(), t.getCaracteristica(), t.getCodigoArea(), t.getNumero());
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
