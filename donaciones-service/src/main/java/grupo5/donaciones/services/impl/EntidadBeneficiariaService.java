package grupo5.donaciones.services.impl;

import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.donaciones.dto.entidadBeneficiaria.EntidadBeneficiariaInputDTO;
import grupo5.donaciones.dto.entidadBeneficiaria.EntidadBeneficiariaOutputDTO;
import grupo5.donaciones.models.entities.beneficiarios.EntidadBeneficiaria;
import grupo5.donaciones.models.entities.personas.Juridica;
import grupo5.donaciones.models.repositories.IEntidadesBeneficiariasRepository;
import grupo5.donaciones.models.repositories.IPersonasRepository;
import grupo5.donaciones.services.IEntidadBeneficiariaService;
import grupo5.donaciones.services.mappers.EntidadBeneficiariaMapper;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class EntidadBeneficiariaService implements IEntidadBeneficiariaService {
  private final IEntidadesBeneficiariasRepository repository;
  private final IPersonasRepository personasRepository;
  private final EntidadBeneficiariaMapper mapper;

  public EntidadBeneficiariaService(
      IEntidadesBeneficiariasRepository repository,
      IPersonasRepository personasRepository,
      EntidadBeneficiariaMapper mapper) {

    this.repository = repository;
    this.personasRepository = personasRepository;
    this.mapper = mapper;
  }

  public EntidadBeneficiariaOutputDTO crearEntidad(EntidadBeneficiariaInputDTO input) {
    grupo5.donaciones.models.entities.personas.Persona persona =
        personasRepository
            .findById(input.juridicaId())
            .orElseThrow(() -> new RecursoNoEncontradoException(input.juridicaId()));

    if (!(persona instanceof Juridica)) {
      throw new grupo5.common.exceptions.ValidationException(
          grupo5.common.exceptions.ErrorCatalog.ENTIDAD_BENEFICIARIA_SIN_PERSONA_JURIDICA);
    }

    EntidadBeneficiaria guardada = repository.save(new EntidadBeneficiaria(input.juridicaId()));

    return mapper.toOutputDTO(guardada);
  }

  public EntidadBeneficiariaOutputDTO obtenerEntidad(UUID id) {

    EntidadBeneficiaria entidad =
        repository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(id));

    return mapper.toOutputDTO(entidad);
  }

  public List<EntidadBeneficiariaOutputDTO> obtenerTodas() {
    return repository.findAll().stream().map(mapper::toOutputDTO).toList();
  }
}
