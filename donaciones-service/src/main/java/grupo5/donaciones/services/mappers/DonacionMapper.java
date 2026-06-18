package grupo5.donaciones.services.mappers;

import grupo5.donaciones.dto.donaciones.inputs.DonacionInputDTO;
import grupo5.donaciones.dto.donaciones.inputs.ItemDonacionInputDTO;
import grupo5.donaciones.dto.donaciones.outputs.CambioEstadoOutputDTO;
import grupo5.donaciones.dto.donaciones.outputs.DonacionOutputDTO;
import grupo5.donaciones.dto.donaciones.outputs.ItemDonacionOutputDTO;
import grupo5.donaciones.models.entities.donaciones.*;
import grupo5.donaciones.models.entities.donantes.Donante;
import grupo5.donaciones.models.entities.personas.Persona;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DonacionMapper {

  private final DireccionMapper direccionMapper;

  public DonacionMapper(DireccionMapper direccionMapper) {
    this.direccionMapper = direccionMapper;
  }

  public Donacion toEntity(DonacionInputDTO dto, Persona persona) {
    if (dto == null) {
      return null;
    }

    Donante donante = new Donante(persona);
    Deposito deposito =
        new Deposito(dto.nombreDeposito(), direccionMapper.toEntity(dto.direccion()));

    Donacion donacion = new Donacion(donante, deposito);
    donacion.setDescripcion(dto.descripcion());
    donacion.setFecha(LocalDateTime.now(ZoneId.systemDefault()));

    if (dto.items() != null) {
      dto.items().forEach(item -> donacion.agregarItem(toItemEntity(item)));
    }

    return donacion;
  }

  public DonacionOutputDTO toOutputDTO(Donacion donacion) {
    if (donacion == null) {
      return null;
    }

    List<ItemDonacionOutputDTO> items =
        donacion.getItems().stream().map(this::toItemOutputDTO).toList();

    List<CambioEstadoOutputDTO> historial = // me da error (this::toCambioEstadoOutputDTO)  :(
        donacion.getHistorialEstados().stream().map(this::toCambioEstadoOutputDTO).toList();

    return new DonacionOutputDTO(
        donacion.getId(),
        donacion.getDonante().getPersona().getId(),
        items,
        donacion.getDescripcion(),
        donacion.getFecha(),
        direccionMapper.toOutputDTO(donacion.getDepositoRecepcion().getDireccion()),
        donacion.getEstadoActual(),
        historial);
  }

  private ItemDonacion toItemEntity(ItemDonacionInputDTO dto) {
    Bien bien =
        new Bien(dto.descripcionBien(), dto.fotoUrl(), dto.fechaVencimiento(), dto.estadoBien());
    return new ItemDonacion(bien, dto.cantidad());
  }

  private ItemDonacionOutputDTO toItemOutputDTO(ItemDonacion item) {
    return new ItemDonacionOutputDTO(
        item.getBien().getDescripcion(),
        item.getBien().getFotoUrl(),
        item.getBien().getFechaVencimiento(),
        item.getBien().getEstado(),
        item.getCantidad());
  }

  private CambioEstadoOutputDTO toCambioEstadoOutputDTO(CambioEstadoDonacion cambio) {
    return new CambioEstadoOutputDTO(
        cambio.getEstadoAnterior(), cambio.getEstadoNuevo(), cambio.getTimestamp());
  }
}
