package grupo5.donaciones.services.mappers;

import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.dto.donaciones.inputs.DonacionInputDTO;
import grupo5.donaciones.dto.donaciones.inputs.ItemDonacionInputDTO;
import grupo5.donaciones.dto.donaciones.outputs.CambioEstadoOutputDTO;
import grupo5.donaciones.dto.donaciones.outputs.DonacionOutputDTO;
import grupo5.donaciones.dto.donaciones.outputs.ItemDonacionOutputDTO;
import grupo5.donaciones.models.entities.donaciones.*;
import grupo5.donaciones.models.entities.donantes.Donante;
import grupo5.donaciones.models.entities.personas.Persona;
import grupo5.donaciones.models.repositories.IDonantesRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class DonacionMapper {

  private final DireccionMapper direccionMapper;
  private final IDonantesRepository donantesRepository;

  public DonacionMapper(DireccionMapper direccionMapper, IDonantesRepository donantesRepository) {
    this.direccionMapper = direccionMapper;
    this.donantesRepository = donantesRepository;
  }

  public Donacion toEntity(DonacionInputDTO dto, Persona persona) {
    return toEntity(dto, persona.getId());
  }

  public Donacion toEntity(DonacionInputDTO dto, Donante donante) {
    if (donante == null) {
      throw new ValidationException(grupo5.common.exceptions.ErrorCatalog.DONACION_SIN_DONANTE);
    }
    return toEntity(dto, donante.getId());
  }

  public Donacion toEntity(DonacionInputDTO dto, UUID donanteId) {
    if (dto == null) {
      return null;
    }

    Deposito deposito =
        new Deposito(dto.nombreDeposito(), direccionMapper.toEntity(dto.direccion()));

    Donacion donacion =
        new Donacion(
            donanteId, deposito, dto.descripcion(), LocalDateTime.now(ZoneId.systemDefault()));

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
        donacion.getItems().stream().map(DonacionMapper::toItemOutputDTO).toList();

    List<CambioEstadoOutputDTO> historial = // me da error (this::toCambioEstadoOutputDTO)  :(
        donacion.getHistorialEstados().stream()
            .map(DonacionMapper::toCambioEstadoOutputDTO)
            .toList();

    UUID personaId =
        donacion.getDonanteId() != null
            ? donantesRepository
                .findById(donacion.getDonanteId())
                .map(Donante::personaId)
                .orElse(null)
            : null;

    return new DonacionOutputDTO(
        donacion.getId(),
        personaId,
        items,
        donacion.getDescripcion(),
        donacion.getFecha(),
        direccionMapper.toOutputDTO(donacion.getDepositoRecepcion().direccion()),
        donacion.getEstadoActual(),
        historial);
  }

  private static ItemDonacion toItemEntity(ItemDonacionInputDTO dto) {
    Bien bien =
        new Bien(dto.descripcionBien(), dto.fotoUrl(), dto.fechaVencimiento(), dto.estadoBien());
    return new ItemDonacion(bien, dto.cantidad());
  }

  private static ItemDonacionOutputDTO toItemOutputDTO(ItemDonacion item) {
    return new ItemDonacionOutputDTO(
        item.bien().descripcion(),
        item.bien().fotoUrl(),
        item.bien().fechaVencimiento(),
        item.bien().estado(),
        item.cantidad());
  }

  private static CambioEstadoOutputDTO toCambioEstadoOutputDTO(CambioEstadoDonacion cambio) {
    return new CambioEstadoOutputDTO(
        cambio.getEstadoAnterior(), cambio.getEstadoNuevo(), cambio.getTimestamp());
  }
}
