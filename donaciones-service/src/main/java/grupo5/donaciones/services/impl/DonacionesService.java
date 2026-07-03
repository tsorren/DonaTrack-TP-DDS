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
import org.springframework.stereotype.Service;

@Service
public class DonacionesService implements IDonacionesService {

  private final IDonacionesRepository donacionesRepository;
  private final IDonantesRepository donantesRepository;
  private final DonacionMapper mapper;
  private final ProcesadorDeDonaciones procesadorDonaciones;

  public DonacionesService(
      IDonacionesRepository donacionesRepository,
      IDonantesRepository donantesRepository,
      DonacionMapper mapper,
      ProcesadorDeDonaciones procesadorDonaciones) {
    this.donacionesRepository = donacionesRepository;
    this.donantesRepository = donantesRepository;
    this.mapper = mapper;
    this.procesadorDonaciones = procesadorDonaciones;
  }

  @Override
  public DonacionOutputDTO cargarDonacion(DonacionInputDTO dto) {
    Donante donante =
        donantesRepository
            .findById(dto.idDonante())
            .orElseThrow(() -> new RecursoNoEncontradoException(dto.idDonante()));

    Donacion donacion = mapper.toEntity(dto, donante);

    donacionesRepository.save(donacion);

    procesadorDonaciones.procesar(donacion);

    return mapper.toOutputDTO(donacion);
  }
}
