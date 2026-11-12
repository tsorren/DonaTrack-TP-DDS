package grupo5.donaciones.services.impl;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.dto.Pagina;
import grupo5.donaciones.dto.entidadBeneficiaria.EntidadBeneficiariaInputDTO;
import grupo5.donaciones.dto.entidadBeneficiaria.EntidadBeneficiariaOutputDTO;
import grupo5.donaciones.dto.publico.EntidadPublicaDTO;
import grupo5.donaciones.models.entities.beneficiarios.EntidadBeneficiaria;
import grupo5.donaciones.models.entities.personas.Persona;
import grupo5.donaciones.models.repositories.IEntidadesBeneficiariasRepository;
import grupo5.donaciones.models.repositories.INecesidadesRepository;
import grupo5.donaciones.models.repositories.IPersonasRepository;
import grupo5.donaciones.models.repositories.listados.CriterioEntidades;
import grupo5.donaciones.models.repositories.listados.Tramo;
import grupo5.donaciones.services.IEntidadBeneficiariaService;
import grupo5.donaciones.services.ParametrosListado;
import grupo5.donaciones.services.ResultadoRegistro;
import grupo5.donaciones.services.mappers.EntidadBeneficiariaMapper;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EntidadBeneficiariaService implements IEntidadBeneficiariaService {
  public static final int TAMANIO_PAGINA_POR_DEFECTO = 20;
  public static final int TAMANIO_PAGINA_MAXIMO = 100;

  private final IEntidadesBeneficiariasRepository repository;
  private final IPersonasRepository personasRepository;
  private final INecesidadesRepository necesidadesRepository;
  private final EntidadBeneficiariaMapper mapper;
  private final CancelacionDeEntregas cancelacionDeEntregas;

  public EntidadBeneficiariaService(
      IEntidadesBeneficiariasRepository repository,
      IPersonasRepository personasRepository,
      INecesidadesRepository necesidadesRepository,
      EntidadBeneficiariaMapper mapper,
      CancelacionDeEntregas cancelacionDeEntregas) {

    this.repository = repository;
    this.personasRepository = personasRepository;
    this.necesidadesRepository = necesidadesRepository;
    this.mapper = mapper;
    this.cancelacionDeEntregas = cancelacionDeEntregas;
  }

  @Override
  public ResultadoRegistro<EntidadBeneficiariaOutputDTO> crearEntidad(
      EntidadBeneficiariaInputDTO input) {
    Persona persona =
        personasRepository
            .findById(input.juridicaId())
            .orElseThrow(() -> new RecursoNoEncontradoException(input.juridicaId()));
    if (persona.estaAnonimizada()) {
      throw new BusinessStateException(ErrorCatalog.PERSONA_ANONIMIZADA_NO_OPERABLE);
    }
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

  @Override
  public Pagina<EntidadBeneficiariaOutputDTO> buscar(String nombre, Integer page, Integer size) {
    Tramo<EntidadBeneficiaria> tramo = repository.buscar(criterio(nombre, false, page, size));
    return new Pagina<>(mapper.toOutputDTOs(tramo.elementos()), (int) tramo.total());
  }

  @Override
  public Pagina<EntidadPublicaDTO> buscarPublicas(String nombre, Integer page, Integer size) {
    Tramo<EntidadBeneficiaria> tramo = repository.buscar(criterio(nombre, true, page, size));
    return new Pagina<>(
        mapper.toOutputDTOs(tramo.elementos()).stream()
            .filter(e -> e.juridica() != null)
            .map(
                e ->
                    new EntidadPublicaDTO(e.id(), e.juridica().razonSocial(), e.juridica().rubro()))
            .toList(),
        (int) tramo.total());
  }

  /** Sin {@code page} ni {@code size}, la página 0 de 20 (R11-01): nunca la lista completa. */
  private static CriterioEntidades criterio(
      String nombre, boolean soloActivas, Integer page, Integer size) {
    ParametrosListado listado = new ParametrosListado(page, size, nombre, null);
    return new CriterioEntidades(listado.texto(), soloActivas, listado.paginado());
  }

  /** También lo usa {@link ArchivoDonantesService} para las filas con error del padrón. */
  static void validarPaginado(Integer page, Integer size) {
    if ((page != null && page < 0)
        || (size != null && (size < 1 || size > TAMANIO_PAGINA_MAXIMO))) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }
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
  @Transactional
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
  @Transactional
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
                cancelacionDeEntregas.cancelarLasQueNoSalieron(necesidad.getId());
              });
    }
  }
}
