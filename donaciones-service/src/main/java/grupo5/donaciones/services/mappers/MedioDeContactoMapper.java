package grupo5.donaciones.services.mappers;

import grupo5.donaciones.dto.mediosDeContacto.CorreoInputDTO;
import grupo5.donaciones.dto.mediosDeContacto.CorreoOutputDTO;
import grupo5.donaciones.dto.mediosDeContacto.MedioDeContactoInputDTO;
import grupo5.donaciones.dto.mediosDeContacto.MedioDeContactoOutputDTO;
import grupo5.donaciones.dto.mediosDeContacto.TelefonoInputDTO;
import grupo5.donaciones.dto.mediosDeContacto.TelefonoOutputDTO;
import grupo5.donaciones.dto.mediosDeContacto.WhatsAppInputDTO;
import grupo5.donaciones.dto.mediosDeContacto.WhatsAppOutputDTO;
import grupo5.donaciones.models.entities.personas.Correo;
import grupo5.donaciones.models.entities.personas.MedioDeContacto;
import grupo5.donaciones.models.entities.personas.Telefono;
import grupo5.donaciones.models.entities.personas.TipoTelefono;
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
          case TelefonoInputDTO t ->
              populateTelefono(
                  new Telefono(),
                  t.caracteristica(),
                  t.codigoArea(),
                  t.numero(),
                  TipoTelefono.ESTANDAR);
          case WhatsAppInputDTO w ->
              populateTelefono(
                  new Telefono(),
                  w.caracteristica(),
                  w.codigoArea(),
                  w.numero(),
                  TipoTelefono.WHATSAPP);
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
      case Telefono t -> {
        if (t.getTipo() == TipoTelefono.WHATSAPP) {
          yield new WhatsAppOutputDTO(
              t.getEsPredeterminado(), t.getCaracteristica(), t.getCodigoArea(), t.getNumero());
        } else {
          yield new TelefonoOutputDTO(
              t.getEsPredeterminado(), t.getCaracteristica(), t.getCodigoArea(), t.getNumero());
        }
      }
      default ->
          throw new IllegalArgumentException(
              "Tipo de medio de contacto no soportado: " + entity.getClass().getSimpleName());
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
