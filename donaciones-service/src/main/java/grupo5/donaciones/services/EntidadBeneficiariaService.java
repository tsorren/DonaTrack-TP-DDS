package grupo5.donaciones.services;

import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.donaciones.dto.entidadBeneficiaria.EntidadBeneficiariaInputDTO;
import grupo5.donaciones.dto.entidadBeneficiaria.EntidadBeneficiariaOutputDTO;
import grupo5.donaciones.models.entities.beneficiarios.EntidadBeneficiaria;
import grupo5.donaciones.models.entities.personas.Juridica;
import grupo5.donaciones.models.repositories.IEntidadesBeneficiariasRepository;
import grupo5.donaciones.models.repositories.IPersonasRepository;
import grupo5.donaciones.services.mappers.EntidadBeneficiariaMapper;
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

    Juridica juridica =
        (Juridica)
            personasRepository
                .findById(input.juridicaId())
                .orElseThrow(() -> new RecursoNoEncontradoException(input.juridicaId()));

    EntidadBeneficiaria guardada = repository.save(new EntidadBeneficiaria(juridica));

    return mapper.toOutputDTO(guardada);
  }

  public EntidadBeneficiariaOutputDTO obtenerEntidad(UUID id) {

    EntidadBeneficiaria entidad =
        repository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(id));

    return mapper.toOutputDTO(entidad);
  }
}
