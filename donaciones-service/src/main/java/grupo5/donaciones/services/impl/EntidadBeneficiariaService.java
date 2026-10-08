package grupo5.donaciones.services.impl;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.dto.entidadBeneficiaria.EntidadBeneficiariaInputDTO;
import grupo5.donaciones.dto.entidadBeneficiaria.EntidadBeneficiariaOutputDTO;
import grupo5.donaciones.models.entities.beneficiarios.EntidadBeneficiaria;
import grupo5.donaciones.models.entities.personas.Persona;
import grupo5.donaciones.models.repositories.IEntidadesBeneficiariasRepository;
import grupo5.donaciones.models.repositories.INecesidadesRepository;
import grupo5.donaciones.models.repositories.IPersonasRepository;
import grupo5.donaciones.services.IEntidadBeneficiariaService;
import grupo5.donaciones.services.ResultadoRegistro;
import grupo5.donaciones.services.mappers.EntidadBeneficiariaMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class EntidadBeneficiariaService implements IEntidadBeneficiariaService {
  private final IEntidadesBeneficiariasRepository repository;
  private final IPersonasRepository personasRepository;
  private final INecesidadesRepository necesidadesRepository;
  private final EntidadBeneficiariaMapper mapper;

  public EntidadBeneficiariaService(
      IEntidadesBeneficiariasRepository repository,
      IPersonasRepository personasRepository,
      INecesidadesRepository necesidadesRepository,
      EntidadBeneficiariaMapper mapper) {

    this.repository = repository;
    this.personasRepository = personasRepository;
    this.necesidadesRepository = necesidadesRepository;
    this.mapper = mapper;
  }

  @Override
  public ResultadoRegistro<EntidadBeneficiariaOutputDTO> crearEntidad(
      EntidadBeneficiariaInputDTO input) {
    Persona persona =
        personasRepository
            .findById(input.juridicaId())
            .orElseThrow(() -> new RecursoNoEncontradoException(input.juridicaId()));
    EntidadBeneficiaria.validarApta(persona);

    Optional<EntidadBeneficiaria> existente = repository.findById(persona.getId());
    if (existente.isPresent()) {
      EntidadBeneficiaria entidad = existente.get();
      if (entidad.reactivar()) {
        repository.save(entidad);
      }
      return new ResultadoRegistro<>(mapper.toOutputDTO(entidad), false);
    }

    EntidadBeneficiaria guardada = repository.save(new EntidadBeneficiaria(persona.getId()));
    return new ResultadoRegistro<>(mapper.toOutputDTO(guardada), true);
  }

  public EntidadBeneficiariaOutputDTO obtenerEntidad(UUID id) {

    EntidadBeneficiaria entidad =
        repository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(id));

    return mapper.toOutputDTO(entidad);
  }

  public List<EntidadBeneficiariaOutputDTO> obtenerTodas() {
    return repository.findAll().stream().map(mapper::toOutputDTO).toList();
  }

  @Override
  public EntidadBeneficiariaOutputDTO actualizarEntidad(
      UUID id, EntidadBeneficiariaInputDTO input) {
    if (!id.equals(input.juridicaId())) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }

    EntidadBeneficiaria entidad =
        repository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(id));
    Persona persona =
        personasRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(id));
    EntidadBeneficiaria.validarApta(persona);

    if (!entidad.estaActivo()) {
      throw new BusinessStateException(ErrorCatalog.ENTIDAD_BENEFICIARIA_INACTIVA);
    }

    return mapper.toOutputDTO(entidad);
  }

  @Override
  public void eliminarEntidad(UUID id) {
    EntidadBeneficiaria entidad =
        repository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(id));
    darDeBaja(entidad);
  }

  @Override
  public boolean esEntidadActiva(UUID juridicaId) {
    return repository.findById(juridicaId).map(EntidadBeneficiaria::estaActivo).orElse(false);
  }

  @Override
  public void darDeBajaSiExiste(UUID juridicaId) {
    repository.findById(juridicaId).ifPresent(this::darDeBaja);
  }

  private void darDeBaja(EntidadBeneficiaria entidad) {
    if (entidad.darDeBaja()) {
      repository.save(entidad);
      necesidadesRepository
          .buscarNecesidadesPorEntidad(entidad.getId())
          .forEach(
              necesidad -> {
                necesidad.desactivar();
                necesidadesRepository.save(necesidad);
              });
    }
  }
}
