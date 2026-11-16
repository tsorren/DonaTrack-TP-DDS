package grupo5.donaciones.services.impl;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.config.AdminConstantes;
import grupo5.donaciones.dto.Pagina;
import grupo5.donaciones.dto.personas.FiltroPersonas;
import grupo5.donaciones.dto.personas.JuridicaInputDTO;
import grupo5.donaciones.dto.personas.PersonaInputDTO;
import grupo5.donaciones.dto.personas.PersonaOutputDTO;
import grupo5.donaciones.models.entities.beneficiarios.EntidadBeneficiaria;
import grupo5.donaciones.models.entities.personas.Juridica;
import grupo5.donaciones.models.entities.personas.Persona;
import grupo5.donaciones.models.entities.personas.TipoJuridico;
import grupo5.donaciones.models.repositories.IPersonasRepository;
import grupo5.donaciones.models.repositories.listados.CriterioPersonas;
import grupo5.donaciones.models.repositories.listados.OrdenPorNombre;
import grupo5.donaciones.models.repositories.listados.Tramo;
import grupo5.donaciones.services.IDonantesService;
import grupo5.donaciones.services.IEntidadBeneficiariaService;
import grupo5.donaciones.services.IPersonasService;
import grupo5.donaciones.services.ParametrosListado;
import grupo5.donaciones.services.mappers.PersonaMapper;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PersonasService implements IPersonasService {

  private final IPersonasRepository repository;
  private final PersonaMapper mapper;
  private final NotificacionesAsyncService notificacionesAsyncService;
  private final IDonantesService donantesService;
  private final IEntidadBeneficiariaService entidadBeneficiariaService;

  public PersonasService(
      IPersonasRepository repository,
      PersonaMapper mapper,
      NotificacionesAsyncService notificacionesAsyncService,
      IDonantesService donantesService,
      IEntidadBeneficiariaService entidadBeneficiariaService) {
    this.repository = repository;
    this.mapper = mapper;
    this.notificacionesAsyncService = notificacionesAsyncService;
    this.donantesService = donantesService;
    this.entidadBeneficiariaService = entidadBeneficiariaService;
  }

  @Override
  public PersonaOutputDTO crearPersona(PersonaInputDTO input) {
    Persona persona = mapper.toEntity(input);

    if (persona instanceof Juridica juridica) {
      juridica.getRepresentantes().forEach(repository::save);
    }

    Persona guardada = repository.save(persona);

    // Sincronizar asincrónicamente con el servicio de notificaciones
    notificacionesAsyncService.sincronizarPersona(mapper.toEventoPersonaSincronizadaV1(guardada));

    return mapper.toOutputDTO(guardada);
  }

  @Override
  public Pagina<PersonaOutputDTO> buscar(FiltroPersonas filtro, ParametrosListado listado) {
    List<UUID> ids = filtro.id() == null ? List.of() : filtro.id();
    if (ids.size() > FiltroPersonas.IDS_MAXIMOS) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }
    listado.orden(OrdenPorNombre.class, OrdenPorNombre.NOMBRE);
    Tramo<Persona> tramo =
        repository.buscar(
            new CriterioPersonas(
                listado.texto(),
                filtro.tipo(),
                filtro.tipoJuridico(),
                Set.copyOf(ids),
                listado.paginado()));
    return new Pagina<>(
        tramo.elementos().stream().map(mapper::toOutputDTO).toList(), (int) tramo.total());
  }

  @Override
  public PersonaOutputDTO actualizarPersona(UUID id, PersonaInputDTO input) {
    Persona persona =
        repository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(id));
    validarNoAnonimizada(persona);

    validarCustodiaDeEntidad(persona, input);

    mapper.updateEntity(persona, input);

    if (persona instanceof Juridica juridica) {
      juridica.getRepresentantes().forEach(repository::save);
    }

    Persona guardada = repository.save(persona);

    // Sincronizar asincrónicamente con el servicio de notificaciones
    notificacionesAsyncService.sincronizarPersona(mapper.toEventoPersonaSincronizadaV1(guardada));

    return mapper.toOutputDTO(guardada);
  }

  /** Una persona anonimizada no se edita: editarla la "des-anonimizaría" y le devolvería roles. */
  private static void validarNoAnonimizada(Persona persona) {
    if (persona.estaAnonimizada()) {
      throw new BusinessStateException(ErrorCatalog.PERSONA_ANONIMIZADA_NO_OPERABLE);
    }
  }

  /**
   * Mientras una jurídica sea entidad beneficiaria activa, el pedido no puede volverla EMPRESA ni
   * dejarla sin dirección. Se valida contra los datos resultantes y antes de modificar la persona.
   */
  private void validarCustodiaDeEntidad(Persona persona, PersonaInputDTO input) {
    if (persona instanceof Juridica juridica
        && input instanceof JuridicaInputDTO juridicaInput
        && entidadBeneficiariaService.esEntidadActiva(persona.getId())) {
      TipoJuridico tipoResultante =
          juridicaInput.tipoJuridico() != null ? juridicaInput.tipoJuridico() : juridica.getTipo();
      EntidadBeneficiaria.validarRequisitos(tipoResultante, juridicaInput.direccion() != null);
    }
  }

  @Override
  public PersonaOutputDTO actualizarParcial(UUID id, PersonaInputDTO input) {
    Persona persona =
        repository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(id));
    validarNoAnonimizada(persona);

    mapper.mergeEntity(persona, input);

    Persona guardada = repository.save(persona);

    notificacionesAsyncService.sincronizarPersona(mapper.toEventoPersonaSincronizadaV1(guardada));

    return mapper.toOutputDTO(guardada);
  }

  @Override
  public void eliminarPersona(UUID id) {
    Persona persona =
        repository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(id));

    persona.anonimizar();
    repository.save(persona);

    // Una persona anonimizada deja de operar: se dan de baja sus roles (con eventos y cascada).
    donantesService.darDeBajaSiExiste(id);
    entidadBeneficiariaService.darDeBajaSiExiste(id);

    // Sincronizar asincrónicamente con el servicio de notificaciones
    notificacionesAsyncService.sincronizarPersona(mapper.toEventoPersonaSincronizadaV1(persona));
  }

  @Override
  public UUID obtenerIdPersonaAdministradora() {
    return repository
        .findByDocumento(AdminConstantes.DOCUMENTO_ADMIN)
        .map(Persona::getId)
        .orElse(null);
  }
}
