package grupo5.donaciones.services;

import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.donaciones.dto.donantes.DonanteInputDTO;
import grupo5.donaciones.dto.donantes.DonanteOutputDTO;
import grupo5.donaciones.models.entities.donantes.Donante;
import grupo5.donaciones.models.repositories.IDonantesRepository;
import grupo5.donaciones.services.mappers.DonanteMapper;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class DonantesService implements IDonantesService {
  private final IDonantesRepository donantesRepository;
  private final DonanteMapper donanteMapper;

  public DonantesService(IDonantesRepository donantesRepository, DonanteMapper donanteMapper) {
    this.donantesRepository = donantesRepository;
    this.donanteMapper = donanteMapper;
  }

  @Override
  public DonanteOutputDTO crearDonante(DonanteInputDTO input) {
    Donante donanteDominio = donanteMapper.toEntity(input);

    Donante guardado = donantesRepository.save(donanteDominio);
    return donanteMapper.toOutputDTO(guardado);
  }

  @Override
  public List<DonanteOutputDTO> listarDonantesPorContacto(String canal) {

    List<Donante> donantes = donantesRepository.findAll();

    if (canal == null || canal.isBlank()) {
      return donantes.stream()
          .map(donanteMapper::toOutputDTO)
          .collect(java.util.stream.Collectors.toList());
    }

    return donantes.stream()
        .map(donanteMapper::toOutputDTO) // Convertimos cada Donante en DonanteOutputDTO
        .filter(d -> d.getCanalContacto() != null && canal.equalsIgnoreCase(d.getCanalContacto()))
        .collect(java.util.stream.Collectors.toList());
  }

  @Override
  public DonanteOutputDTO obtenerPorId(UUID id) {
    Donante donante =
        donantesRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(id));
    return donanteMapper.toOutputDTO(donante);
  }

  @Override
  public DonanteOutputDTO actualizarCanal(UUID id, DonanteInputDTO dto) {
    Donante donante =
        donantesRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(id));

    donanteMapper.updateEntity(donante, dto);

    Donante donanteActualizado = donantesRepository.save(donante);

    return donanteMapper.toOutputDTO(donanteActualizado);
  }
}
