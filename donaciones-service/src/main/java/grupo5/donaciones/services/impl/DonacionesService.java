package grupo5.donaciones.services.impl;

import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.donaciones.dto.donaciones.inputs.DonacionInputDTO;
import grupo5.donaciones.dto.donaciones.outputs.DonacionOutputDTO;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.personas.Persona;
import grupo5.donaciones.models.repositories.IDonacionesRepository;
import grupo5.donaciones.models.repositories.IPersonasRepository;
import grupo5.donaciones.services.IDonacionesService;
import grupo5.donaciones.services.mappers.DonacionMapper;
import org.springframework.stereotype.Service;

@Service
public class DonacionesService implements IDonacionesService {

  private final IDonacionesRepository donacionesRepository;
  private final IPersonasRepository personasRepository;
  private final DonacionMapper mapper;

  public DonacionesService(
      IDonacionesRepository donacionesRepository,
      IPersonasRepository personasRepository,
      DonacionMapper mapper) {
    this.donacionesRepository = donacionesRepository;
    this.personasRepository = personasRepository;
    this.mapper = mapper;
  }

  @Override
  public DonacionOutputDTO cargarDonacion(DonacionInputDTO dto) {
    Persona persona =
        personasRepository
            .findById(dto.idDonante())
            .orElseThrow(() -> new RecursoNoEncontradoException(dto.idDonante()));

    Donacion donacion = mapper.toEntity(dto, persona);

    donacionesRepository.save(donacion);

    // TODO: procesarDonacion(donacion) - pendiente

    return mapper.toOutputDTO(donacion);
  }
}
