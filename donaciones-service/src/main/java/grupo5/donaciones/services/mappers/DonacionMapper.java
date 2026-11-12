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
import grupo5.donaciones.services.IFotosBienesService;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class DonacionMapper {

  private final DireccionMapper direccionMapper;
  private final IDonantesRepository donantesRepository;
  private final IPersonasRepository personasRepository;
  private final PersonaMapper personaMapper;
  private final IFotosBienesService fotosBienes;

  public DonacionMapper(
      DireccionMapper direccionMapper,
      IDonantesRepository donantesRepository,
      IPersonasRepository personasRepository,
      PersonaMapper personaMapper,
      IFotosBienesService fotosBienes) {
    this.direccionMapper = direccionMapper;
    this.donantesRepository = donantesRepository;
    this.personasRepository = personasRepository;
    this.personaMapper = personaMapper;
    this.fotosBienes = fotosBienes;
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
    if (Boolean.FALSE.equals(dto.mostrarNombre())) {
      donacion.ocultarNombre();
    }

    if (dto.items() != null) {
      dto.items().forEach(item -> donacion.agregarItem(toItemEntity(item)));
    }

    return donacion;
  }

  public DonacionOutputDTO toOutputDTO(Donacion donacion) {
    if (donacion == null) {
      return null;
    }
    DonanteResumenDTO donanteResumen = null;
    if (donacion.getDonanteId() != null) {
      donanteResumen =
          donantesRepository
              .findById(donacion.getDonanteId())
              .map(
                  donante ->
                      resumen(
                          donante,
                          donante.personaId() == null
                              ? null
                              : personasRepository.findById(donante.personaId()).orElse(null)))
              .orElse(null);
    }
    return toOutputDTO(donacion, donanteResumen);
  }

  /** Una página: los donantes y sus personas se leen juntos, no uno por fila (R11-01). */
  public List<DonacionOutputDTO> toOutputDTOs(List<Donacion> donaciones) {
    List<UUID> donanteIds =
        donaciones.stream().map(Donacion::getDonanteId).filter(Objects::nonNull).toList();
    Map<UUID, Donante> donantes =
        donantesRepository.findAllById(donanteIds).stream()
            .collect(Collectors.toMap(Donante::getId, Function.identity(), (a, b) -> a));
    Map<UUID, Persona> personas =
        personasRepository.findAllById(donantes.keySet()).stream()
            .collect(Collectors.toMap(Persona::getId, Function.identity(), (a, b) -> a));
    return donaciones.stream()
        .map(
            d -> {
              Donante donante = d.getDonanteId() == null ? null : donantes.get(d.getDonanteId());
              DonanteResumenDTO resumen =
                  donante == null ? null : resumen(donante, personas.get(donante.personaId()));
              return toOutputDTO(d, resumen);
            })
        .toList();
  }

  private DonanteResumenDTO resumen(Donante donante, Persona persona) {
    PersonaOutputDTO personaDTO = persona == null ? null : personaMapper.toOutputDTO(persona);
    return new DonanteResumenDTO(donante.getId(), donante.personaId(), personaDTO);
  }

  private DonacionOutputDTO toOutputDTO(Donacion donacion, DonanteResumenDTO donanteResumen) {
    List<ItemDonacionOutputDTO> items =
        donacion.getItems().stream().map(this::toItemOutputDTO).toList();

    List<CambioEstadoOutputDTO> historial =
        donacion.getHistorialEstados().stream()
            .map(DonacionMapper::toCambioEstadoOutputDTO)
            .toList();

    return new DonacionOutputDTO(
        donacion.getId(),
        donanteResumen,
        items,
        donacion.getDescripcion(),
        donacion.getFecha(),
        (donacion.getDepositoRecepcion() != null
                && donacion.getDepositoRecepcion().direccion() != null)
            ? direccionMapper.toOutputDTO(donacion.getDepositoRecepcion().direccion())
            : null,
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

  /** La foto se devuelve como URL temporal de descarga: en el bien se guarda la referencia. */
  private ItemDonacionOutputDTO toItemOutputDTO(ItemDonacion item) {
    return new ItemDonacionOutputDTO(
        item.bien().descripcion(),
        fotosBienes.urlParaMostrar(item.bien().fotoUrl()),
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
