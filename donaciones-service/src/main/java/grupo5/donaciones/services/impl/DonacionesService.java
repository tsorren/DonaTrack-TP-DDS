package grupo5.donaciones.services.impl;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.dto.Pagina;
import grupo5.donaciones.dto.donaciones.FiltroDonaciones;
import grupo5.donaciones.dto.donaciones.inputs.DonacionInputDTO;
import grupo5.donaciones.dto.donaciones.outputs.DonacionOutputDTO;
import grupo5.donaciones.infrastructure.ProcesadorDeDonaciones;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donantes.Donante;
import grupo5.donaciones.models.repositories.IDonacionesRepository;
import grupo5.donaciones.models.repositories.IDonantesRepository;
import grupo5.donaciones.models.repositories.listados.CriterioDonaciones;
import grupo5.donaciones.models.repositories.listados.OrdenPorFecha;
import grupo5.donaciones.models.repositories.listados.Tramo;
import grupo5.donaciones.services.IDonacionesService;
import grupo5.donaciones.services.IFotosBienesService;
import grupo5.donaciones.services.ParametrosListado;
import grupo5.donaciones.services.mappers.DonacionMapper;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class DonacionesService implements IDonacionesService {

  private final IDonacionesRepository donacionesRepository;
  private final IDonantesRepository donantesRepository;
  private final DonacionMapper mapper;
  private final ProcesadorDeDonaciones procesadorDonaciones;
  private final ApplicationEventPublisher eventPublisher;
  private final IFotosBienesService fotosBienes;

  public DonacionesService(
      IDonacionesRepository donacionesRepository,
      IDonantesRepository donantesRepository,
      DonacionMapper mapper,
      ProcesadorDeDonaciones procesadorDonaciones,
      ApplicationEventPublisher eventPublisher,
      IFotosBienesService fotosBienes) {
    this.donacionesRepository = donacionesRepository;
    this.donantesRepository = donantesRepository;
    this.mapper = mapper;
    this.procesadorDonaciones = procesadorDonaciones;
    this.eventPublisher = eventPublisher;
    this.fotosBienes = fotosBienes;
  }

  @Override
  public DonacionOutputDTO cargarDonacion(DonacionInputDTO dto) {
    Donante donante =
        donantesRepository
            .findById(dto.idDonante())
            .orElseThrow(() -> new RecursoNoEncontradoException(dto.idDonante()));
    if (!donante.estaActivo()) {
      throw new BusinessStateException(ErrorCatalog.DONANTE_INACTIVO);
    }

    if (dto.items() != null) {
      dto.items().forEach(item -> fotosBienes.validarFoto(item.fotoUrl()));
    }

    Donacion donacion = mapper.toEntity(dto, donante);

    donacionesRepository.save(donacion);
    donacion.getDomainEvents().forEach(eventPublisher::publishEvent);
    donacion.clearDomainEvents();

    procesadorDonaciones.procesar(donacion);

    return mapper.toOutputDTO(donacion);
  }

  @Override
  public Pagina<DonacionOutputDTO> buscar(FiltroDonaciones filtro, ParametrosListado listado) {
    if (filtro.desde() != null
        && filtro.hasta() != null
        && filtro.desde().isAfter(filtro.hasta())) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }
    Tramo<Donacion> tramo =
        donacionesRepository.buscar(
            new CriterioDonaciones(
                filtro.donanteId(),
                filtro.desde(),
                filtro.hasta(),
                listado.texto(),
                listado.orden(OrdenPorFecha.class, OrdenPorFecha.RECIENTES),
                listado.paginado()));
    return new Pagina<>(mapper.toOutputDTOs(tramo.elementos()), (int) tramo.total());
  }

  @Override
  public DonacionOutputDTO obtenerDonacion(UUID id) {
    Donacion donacion =
        donacionesRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(id));
    return mapper.toOutputDTO(donacion);
  }
}
