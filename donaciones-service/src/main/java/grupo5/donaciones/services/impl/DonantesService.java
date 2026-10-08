package grupo5.donaciones.services.impl;

import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.donaciones.dto.comunicaciones.EventoDonanteDadoDeBajaV1;
import grupo5.donaciones.dto.comunicaciones.EventoDonanteRegistradoV1;
import grupo5.donaciones.dto.donantes.DonanteInputDTO;
import grupo5.donaciones.dto.donantes.DonanteOutputDTO;
import grupo5.donaciones.models.entities.donantes.Donante;
import grupo5.donaciones.models.entities.personas.Persona;
import grupo5.donaciones.models.repositories.IDonantesRepository;
import grupo5.donaciones.models.repositories.IPersonasRepository;
import grupo5.donaciones.services.EstadoRegistroDonante;
import grupo5.donaciones.services.IDonacionesEventPublisher;
import grupo5.donaciones.services.IDonantesService;
import grupo5.donaciones.services.ResultadoRegistro;
import grupo5.donaciones.services.mappers.DonanteMapper;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
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
  public List<DonanteOutputDTO> listarDonantesPorContacto(String canal) {
    List<Donante> donantes = donantesRepository.findAll();

    if (canal == null || canal.isBlank()) {
      return donantes.stream().map(donanteMapper::toOutputDTO).toList();
    }

    return donantes.stream()
        .map(donanteMapper::toOutputDTO)
        .filter(
            d ->
                d.persona() != null
                    && d.persona().mediosDeContacto() != null
                    && d.persona().mediosDeContacto().stream()
                        .anyMatch(
                            medio -> {
                              if (medio == null) return false;
                              String tipoMedio =
                                  medio
                                      .getClass()
                                      .getSimpleName()
                                      .replace("OutputDTO", "")
                                      .toUpperCase();
                              return canal.equalsIgnoreCase(tipoMedio);
                            }))
        .toList();
  }

  @Override
  public DonanteOutputDTO obtenerPorId(UUID id) {
    Donante donante =
        donantesRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(id));
    return donanteMapper.toOutputDTO(donante);
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
