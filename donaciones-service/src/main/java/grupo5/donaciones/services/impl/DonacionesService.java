package grupo5.donaciones.services.impl;

import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.donaciones.dto.donaciones.inputs.DonacionInputDTO;
import grupo5.donaciones.dto.donaciones.outputs.DonacionOutputDTO;
import grupo5.donaciones.infrastructure.ProcesadorDeDonaciones;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donantes.Donante;
import grupo5.donaciones.models.repositories.IDonacionesRepository;
import grupo5.donaciones.models.repositories.IDonantesRepository;
import grupo5.donaciones.services.IDonacionesService;
import grupo5.donaciones.services.mappers.DonacionMapper;
import java.util.List;
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

  public DonacionesService(
      IDonacionesRepository donacionesRepository,
      IDonantesRepository donantesRepository,
      DonacionMapper mapper,
      ProcesadorDeDonaciones procesadorDonaciones,
      ApplicationEventPublisher eventPublisher) {
    this.donacionesRepository = donacionesRepository;
    this.donantesRepository = donantesRepository;
    this.mapper = mapper;
    this.procesadorDonaciones = procesadorDonaciones;
    this.eventPublisher = eventPublisher;
  }

  @Override
  public DonacionOutputDTO cargarDonacion(DonacionInputDTO dto) {
    Donante donante =
        donantesRepository
            .findById(dto.idDonante())
            .orElseThrow(() -> new RecursoNoEncontradoException(dto.idDonante()));

    Donacion donacion = mapper.toEntity(dto, donante);

    donacionesRepository.save(donacion);
    donacion.getDomainEvents().forEach(eventPublisher::publishEvent);
    donacion.clearDomainEvents();

    procesadorDonaciones.procesar(donacion);

    return mapper.toOutputDTO(donacion);
  }

  @Override
  public List<DonacionOutputDTO> listarDonaciones() {
    return donacionesRepository.findAll().stream().map(mapper::toOutputDTO).toList();
  }

  @Override
  public DonacionOutputDTO obtenerDonacion(UUID id) {
    Donacion donacion =
        donacionesRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(id));
    return mapper.toOutputDTO(donacion);
  }
}
