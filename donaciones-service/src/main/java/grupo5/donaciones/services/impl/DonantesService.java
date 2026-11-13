package grupo5.donaciones.services.impl;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.dto.Pagina;
import grupo5.donaciones.dto.comunicaciones.EventoDonanteDadoDeBajaV1;
import grupo5.donaciones.dto.comunicaciones.EventoDonanteRegistradoV1;
import grupo5.donaciones.dto.donantes.DonanteInputDTO;
import grupo5.donaciones.dto.donantes.DonanteOutputDTO;
import grupo5.donaciones.dto.donantes.FiltroDonantes;
import grupo5.donaciones.models.entities.donantes.Donante;
import grupo5.donaciones.models.entities.personas.Persona;
import grupo5.donaciones.models.repositories.IDonantesRepository;
import grupo5.donaciones.models.repositories.IPersonasRepository;
import grupo5.donaciones.models.repositories.listados.CriterioDonantes;
import grupo5.donaciones.models.repositories.listados.OrdenDonantes;
import grupo5.donaciones.models.repositories.listados.Tramo;
import grupo5.donaciones.services.EstadoRegistroDonante;
import grupo5.donaciones.services.IDonacionesEventPublisher;
import grupo5.donaciones.services.IDonantesService;
import grupo5.donaciones.services.ParametrosListado;
import grupo5.donaciones.services.ResultadoRegistro;
import grupo5.donaciones.services.mappers.DonanteMapper;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class DonantesService implements IDonantesService {
  private final IDonantesRepository donantesRepository;
  private final DonanteMapper donanteMapper;
  private final IPersonasRepository personasRepository;
  private final IDonacionesEventPublisher eventPublisher;

  public DonantesService(
      IDonantesRepository donantesRepository,
      DonanteMapper donanteMapper,
      IPersonasRepository personasRepository,
      IDonacionesEventPublisher eventPublisher) {
    this.donantesRepository = donantesRepository;
    this.donanteMapper = donanteMapper;
    this.personasRepository = personasRepository;
    this.eventPublisher = eventPublisher;
  }

  @Override
  public ResultadoRegistro<DonanteOutputDTO> crearDonante(DonanteInputDTO input) {
    Persona persona =
        personasRepository
            .findById(input.idPersona())
            .orElseThrow(() -> new RecursoNoEncontradoException(input.idPersona()));
    if (persona.estaAnonimizada()) {
      throw new BusinessStateException(ErrorCatalog.PERSONA_ANONIMIZADA_NO_OPERABLE);
    }

    Optional<Donante> existente = donantesRepository.findById(input.idPersona());
    if (existente.isPresent()) {
      Donante donante = existente.get();
      if (donante.reactivar()) {
        donantesRepository.save(donante);
        publicarDonanteRegistrado(donante);
      }
      return new ResultadoRegistro<>(donanteMapper.toOutputDTO(donante), false);
    }

    Donante guardado = donantesRepository.save(donanteMapper.toEntity(input));
    publicarDonanteRegistrado(guardado);
    return new ResultadoRegistro<>(donanteMapper.toOutputDTO(guardado), true);
  }

  private void publicarDonanteRegistrado(Donante donante) {
    Persona persona =
        personasRepository
            .findById(donante.personaId())
            .orElseThrow(() -> new RecursoNoEncontradoException(donante.personaId()));

    String nombre = persona.getNombreCompleto();
    String credenciales =
        "Usuario: "
            + persona.getId()
            + " / Password: "
            + UUID.randomUUID().toString().substring(0, 8);

    eventPublisher.publicarDonanteRegistrado(
        new EventoDonanteRegistradoV1(
            donante.getId(),
            persona.getId(),
            nombre,
            LocalDateTime.now(ZoneId.systemDefault()),
            credenciales));
  }

  @Override
  public Pagina<DonanteOutputDTO> buscar(FiltroDonantes filtro, ParametrosListado listado) {
    // Filtrar por canal es para contactar: un donante de baja o una persona anonimizada no aplican
    String canal = filtro.canal() == null || filtro.canal().isBlank() ? null : filtro.canal();
    if (filtro.altaDesde() != null
        && filtro.altaHasta() != null
        && filtro.altaDesde().isAfter(filtro.altaHasta())) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }
    Tramo<Donante> tramo =
        donantesRepository.buscar(
            new CriterioDonantes(
                listado.texto(),
                filtro.tipo(),
                filtro.tipoJuridico(),
                filtro.activo(),
                filtro.categoriaId(),
                canal,
                listado.orden(OrdenDonantes.class, OrdenDonantes.NUEVOS),
                listado.paginado(),
                filtro.altaDesde(),
                filtro.altaHasta()));
    return new Pagina<>(donanteMapper.toOutputDTOs(tramo.elementos()), (int) tramo.total());
  }

  @Override
  public DonanteOutputDTO obtenerPorId(UUID id) {
    Donante donante =
        donantesRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(id));
    return donanteMapper.toOutputDTO(donante);
  }

  @Override
  public DonanteOutputDTO actualizarPreferencias(UUID id, boolean mostrarNombre) {
    Donante donante =
        donantesRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(id));
    donante.mostrarNombre(mostrarNombre);
    return donanteMapper.toOutputDTO(donantesRepository.save(donante));
  }

  @Override
  public EstadoRegistroDonante registrarSiNoExiste(UUID personaId) {
    Optional<Donante> existente = donantesRepository.findById(personaId);
    if (existente.isPresent()) {
      return existente.get().estaActivo()
          ? EstadoRegistroDonante.YA_REGISTRADO
          : EstadoRegistroDonante.DADO_DE_BAJA;
    }
    crearDonante(new DonanteInputDTO(personaId));
    return EstadoRegistroDonante.CREADO;
  }

  @Override
  public boolean estaDadoDeBaja(UUID personaId) {
    return donantesRepository.findById(personaId).map(d -> !d.estaActivo()).orElse(false);
  }

  @Override
  public void eliminarDonante(UUID id) {
    Donante donante =
        donantesRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(id));
    darDeBaja(donante);
  }

  @Override
  public void darDeBajaSiExiste(UUID personaId) {
    donantesRepository.findById(personaId).ifPresent(this::darDeBaja);
  }

  private void darDeBaja(Donante donante) {
    if (donante.darDeBaja()) {
      donantesRepository.save(donante);
      eventPublisher.publicarDonanteDadoDeBaja(
          new EventoDonanteDadoDeBajaV1(
              donante.getId(), donante.personaId(), LocalDateTime.now(ZoneId.systemDefault())));
    }
  }
}
