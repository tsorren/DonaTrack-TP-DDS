package grupo5.donaciones.services.impl;

import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.donaciones.dto.comunicaciones.EventoDonanteRegistradoDTO;
import grupo5.donaciones.dto.comunicaciones.RegistrarDonanteRequest;
import grupo5.donaciones.dto.donantes.DonanteInputDTO;
import grupo5.donaciones.dto.donantes.DonanteOutputDTO;
import grupo5.donaciones.infrastructure.clients.IncentivosFeignClient;
import grupo5.donaciones.infrastructure.clients.NotificacionesFeignClient;
import grupo5.donaciones.models.entities.donantes.Donante;
import grupo5.donaciones.models.entities.personas.Humana;
import grupo5.donaciones.models.entities.personas.Juridica;
import grupo5.donaciones.models.entities.personas.Persona;
import grupo5.donaciones.models.repositories.IDonantesRepository;
import grupo5.donaciones.services.IDonantesService;
import grupo5.donaciones.services.mappers.DonanteMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class DonantesService implements IDonantesService {
  private final IDonantesRepository donantesRepository;
  private final DonanteMapper donanteMapper;
  private final IncentivosFeignClient incentivosFeignClient;
  private final NotificacionesFeignClient notificacionesFeignClient;

  public DonantesService(
      IDonantesRepository donantesRepository,
      DonanteMapper donanteMapper,
      IncentivosFeignClient incentivosFeignClient,
      NotificacionesFeignClient notificacionesFeignClient) {
    this.donantesRepository = donantesRepository;
    this.donanteMapper = donanteMapper;
    this.incentivosFeignClient = incentivosFeignClient;
    this.notificacionesFeignClient = notificacionesFeignClient;
  }

  @Override
  public DonanteOutputDTO crearDonante(DonanteInputDTO input) {
    Donante donanteDominio = donanteMapper.toEntity(input);
    Donante guardado = donantesRepository.save(donanteDominio);

    if (guardado.getPersona() != null) {
      String nombre = obtenerNombrePersona(guardado.getPersona());
      incentivosFeignClient.registrarDonante(
          guardado.getId(),
          new RegistrarDonanteRequest(guardado.getId(), guardado.getPersona().getId(), nombre));

      String credenciales =
          "Usuario: "
              + guardado.getPersona().getId()
              + " / Password: "
              + UUID.randomUUID().toString().substring(0, 8);
      notificacionesFeignClient.enviarEvento(
          new EventoDonanteRegistradoDTO(
              guardado.getPersona().getId(), LocalDateTime.now(), credenciales));
    }

    return donanteMapper.toOutputDTO(guardado);
  }

  private String obtenerNombrePersona(Persona persona) {
    if (persona instanceof Humana humana) {
      return humana.getNombre() + " " + humana.getApellido();
    } else if (persona instanceof Juridica juridica) {
      return juridica.getRazonSocial();
    }
    return "Anónimo";
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
