package grupo5.donaciones.services.impl;

import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.donaciones.dto.comunicaciones.EventoDonanteRegistradoDTO;
import grupo5.donaciones.dto.comunicaciones.RegistrarDonanteRequest;
import grupo5.donaciones.dto.donantes.DonanteInputDTO;
import grupo5.donaciones.dto.donantes.DonanteOutputDTO;
import grupo5.donaciones.infrastructure.clients.IncentivosFeignClient;
import grupo5.donaciones.infrastructure.clients.NotificacionesFeignClient;
import grupo5.donaciones.models.entities.donantes.Donante;
import grupo5.donaciones.models.entities.personas.Persona;
import grupo5.donaciones.models.repositories.IDonantesRepository;
import grupo5.donaciones.models.repositories.IPersonasRepository;
import grupo5.donaciones.services.IDonantesService;
import grupo5.donaciones.services.mappers.DonanteMapper;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class DonantesService implements IDonantesService {
  private final IDonantesRepository donantesRepository;
  private final DonanteMapper donanteMapper;
  private final IncentivosFeignClient incentivosFeignClient;
  private final NotificacionesFeignClient notificacionesFeignClient;
  private final IPersonasRepository personasRepository;

  public DonantesService(
      IDonantesRepository donantesRepository,
      DonanteMapper donanteMapper,
      IncentivosFeignClient incentivosFeignClient,
      NotificacionesFeignClient notificacionesFeignClient,
      IPersonasRepository personasRepository) {
    this.donantesRepository = donantesRepository;
    this.donanteMapper = donanteMapper;
    this.incentivosFeignClient = incentivosFeignClient;
    this.notificacionesFeignClient = notificacionesFeignClient;
    this.personasRepository = personasRepository;
  }

  @Override
  public DonanteOutputDTO crearDonante(DonanteInputDTO input) {
    Donante donanteDominio = donanteMapper.toEntity(input);
    Donante guardado = donantesRepository.save(donanteDominio);

    if (guardado.personaId() != null) {
      Persona persona =
          personasRepository
              .findById(guardado.personaId())
              .orElseThrow(() -> new RecursoNoEncontradoException(guardado.personaId()));

      String nombre = persona.getNombreCompleto();
      incentivosFeignClient.registrarDonante(
          guardado.getId(), new RegistrarDonanteRequest(guardado.getId(), persona.getId(), nombre));

      String credenciales =
          "Usuario: "
              + persona.getId()
              + " / Password: "
              + UUID.randomUUID().toString().substring(0, 8);
      notificacionesFeignClient.enviarEvento(
          new EventoDonanteRegistradoDTO(
              persona.getId(), LocalDateTime.now(ZoneId.systemDefault()), credenciales));
    }

    return donanteMapper.toOutputDTO(guardado);
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
  public void eliminarDonante(UUID id) {
    Donante donante =
        donantesRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(id));
    donantesRepository.delete(donante);
    incentivosFeignClient.darDeBaja(id);
  }
}
