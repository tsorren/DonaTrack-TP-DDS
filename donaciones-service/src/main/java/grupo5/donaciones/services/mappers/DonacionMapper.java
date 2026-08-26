package grupo5.donaciones.services.mappers;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.dto.donaciones.inputs.DonacionInputDTO;
import grupo5.donaciones.dto.donaciones.inputs.ItemDonacionInputDTO;
import grupo5.donaciones.dto.donaciones.outputs.CambioEstadoOutputDTO;
import grupo5.donaciones.dto.donaciones.outputs.DonacionOutputDTO;
import grupo5.donaciones.dto.donaciones.outputs.DonanteResumenDTO;
import grupo5.donaciones.dto.donaciones.outputs.ItemDonacionOutputDTO;
import grupo5.donaciones.dto.personas.PersonaOutputDTO;
import grupo5.donaciones.models.entities.donaciones.Bien;
import grupo5.donaciones.models.entities.donaciones.CambioEstadoDonacion;
import grupo5.donaciones.models.entities.donaciones.Deposito;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donaciones.ItemDonacion;
import grupo5.donaciones.models.entities.donantes.Donante;
import grupo5.donaciones.models.entities.personas.Persona;
import grupo5.donaciones.models.repositories.IDonantesRepository;
import grupo5.donaciones.models.repositories.IPersonasRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class DonacionMapper {

  private final DireccionMapper direccionMapper;
  private final IDonantesRepository donantesRepository;
  private final IPersonasRepository personasRepository;
  private final PersonaMapper personaMapper;

  public DonacionMapper(
      DireccionMapper direccionMapper,
      IDonantesRepository donantesRepository,
      IPersonasRepository personasRepository,
      PersonaMapper personaMapper) {
    this.direccionMapper = direccionMapper;
    this.donantesRepository = donantesRepository;
    this.personasRepository = personasRepository;
    this.personaMapper = personaMapper;
  }

  public Donacion toEntity(DonacionInputDTO dto, Persona persona) {
    return toEntity(dto, persona.getId());
  }

  public Donacion toEntity(DonacionInputDTO dto, Donante donante) {
    if (donante == null) {
      throw new ValidationException(ErrorCatalog.DONACION_SIN_DONANTE);
    }
    return toEntity(dto, donante.getId());
  }

  public Donacion toEntity(DonacionInputDTO dto, UUID donanteId) {
    if (dto == null) {
      return null;
    }

    Deposito deposito =
        new Deposito(dto.nombreDeposito(), direccionMapper.toEntity(dto.direccion()));

    Donacion donacion = new Donacion(donanteId, deposito, dto.descripcion(), dto.fecha());

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

    List<CambioEstadoOutputDTO> historial =
        donacion.getHistorialEstados().stream()
            .map(DonacionMapper::toCambioEstadoOutputDTO)
            .toList();

    DonanteResumenDTO donanteResumen = null;
    if (donacion.getDonanteId() != null) {
      donanteResumen =
          donantesRepository
              .findById(donacion.getDonanteId())
              .map(
                  donante -> {
                    PersonaOutputDTO personaDTO = null;
                    if (donante.personaId() != null) {
                      personaDTO =
                          personasRepository
                              .findById(donante.personaId())
                              .map(personaMapper::toOutputDTO)
                              .orElse(null);
                    }
                    return new DonanteResumenDTO(donante.getId(), donante.personaId(), personaDTO);
                  })
              .orElse(null);
    }

    return new DonacionOutputDTO(
        donacion.getId(),
        donanteResumen,
        items,
        donacion.getDescripcion(),
        donacion.getFecha(),
        direccionMapper.toOutputDTO(donacion.getDepositoRecepcion().direccion()),
        donacion.getEstadoActual(),
        historial);
  }

  private static ItemDonacion toItemEntity(ItemDonacionInputDTO dto) {
    Bien bien =
        new Bien(
            dto.descripcionBien(),
            dto.fotoUrl(),
            dto.fechaVencimiento(),
            dto.estadoBien(),
            dto.pesoUnitario(),
            dto.volumenUnitario());
    return new ItemDonacion(bien, dto.cantidad());
  }

  private static ItemDonacionOutputDTO toItemOutputDTO(ItemDonacion item) {
    return new ItemDonacionOutputDTO(
        item.bien().descripcion(),
        item.bien().fotoUrl(),
        item.bien().fechaVencimiento(),
        item.bien().estado(),
        item.bien().pesoUnitario(),
        item.bien().volumenUnitario(),
        item.cantidad());
  }

  private static CambioEstadoOutputDTO toCambioEstadoOutputDTO(CambioEstadoDonacion cambio) {
    return new CambioEstadoOutputDTO(
        cambio.getEstadoAnterior(), cambio.getEstadoNuevo(), cambio.getTimestamp());
  }
}
