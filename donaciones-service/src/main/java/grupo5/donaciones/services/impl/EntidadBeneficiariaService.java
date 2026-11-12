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
import grupo5.donaciones.services.IEntidadBeneficiariaService;
import grupo5.donaciones.services.ResultadoRegistro;
import grupo5.donaciones.services.mappers.EntidadBeneficiariaMapper;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
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

  public List<EntidadBeneficiariaOutputDTO> obtenerTodas() {
    return repository.findAll().stream().map(mapper::toOutputDTO).toList();
  }

  @Override
  public Pagina<EntidadBeneficiariaOutputDTO> buscar(String nombre, Integer page, Integer size) {
    validarPaginado(page, size);
    return buscarEn(obtenerTodas(), nombre, page, size);
  }

  @Override
  public Pagina<EntidadPublicaDTO> buscarPublicas(String nombre, Integer page, Integer size) {
    validarPaginado(page, size);
    List<EntidadBeneficiariaOutputDTO> activas =
        obtenerTodas().stream().filter(e -> e.activo() && e.juridica() != null).toList();
    Pagina<EntidadBeneficiariaOutputDTO> pagina = buscarEn(activas, nombre, page, size);
    return new Pagina<>(
        pagina.elementos().stream()
            .map(
                e ->
                    new EntidadPublicaDTO(e.id(), e.juridica().razonSocial(), e.juridica().rubro()))
            .toList(),
        pagina.total());
  }

  /** También lo usa {@link ArchivoDonantesService} para las filas con error del padrón. */
  static void validarPaginado(Integer page, Integer size) {
    if ((page != null && page < 0)
        || (size != null && (size < 1 || size > TAMANIO_PAGINA_MAXIMO))) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }
  }

  /** Filtra por nombre y pagina; {@code page} y {@code size} ya validados. */
  private static Pagina<EntidadBeneficiariaOutputDTO> buscarEn(
      List<EntidadBeneficiariaOutputDTO> entidades, String nombre, Integer page, Integer size) {
    boolean paginado = page != null || size != null;
    int pagina = page == null ? 0 : page;
    int tamanio = size == null ? TAMANIO_PAGINA_POR_DEFECTO : size;

    // ponytail: filtra y pagina en memoria sobre findAll(); pasar a una consulta paginada cuando
    // el volumen lo pida.
    String buscado = nombre == null ? null : nombre.toLowerCase(Locale.ROOT);
    List<EntidadBeneficiariaOutputDTO> filtradas =
        entidades.stream()
            .filter(
                e ->
                    buscado == null
                        || (razonSocial(e) != null
                            && razonSocial(e).toLowerCase(Locale.ROOT).contains(buscado)))
            .toList();
    if (!paginado) {
      return new Pagina<>(filtradas, filtradas.size());
    }
    // Sin paginado se respeta el orden de siempre; paginando hace falta un orden estable.
    List<EntidadBeneficiariaOutputDTO> tramo =
        filtradas.stream()
            .sorted(
                Comparator.comparing(
                        EntidadBeneficiariaService::razonSocial,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                    .thenComparing(EntidadBeneficiariaOutputDTO::id))
            .skip((long) pagina * tamanio)
            .limit(tamanio)
            .toList();
    return new Pagina<>(tramo, filtradas.size());
  }

  private static String razonSocial(EntidadBeneficiariaOutputDTO entidad) {
    return entidad.juridica() == null ? null : entidad.juridica().razonSocial();
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
