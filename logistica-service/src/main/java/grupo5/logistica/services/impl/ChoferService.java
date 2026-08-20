package grupo5.logistica.services.impl;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.dto.choferes.CambioEstadoChoferRequestDTO;
import grupo5.logistica.dto.choferes.ChoferRequestDTO;
import grupo5.logistica.dto.choferes.ChoferResponseDTO;
import grupo5.logistica.models.entities.choferes.Chofer;
import grupo5.logistica.models.entities.choferes.EstadoChofer;
import grupo5.logistica.models.repositories.IChoferesRepository;
import grupo5.logistica.services.IChoferesService;
import grupo5.logistica.services.mappers.ChoferMapper;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ChoferService implements IChoferesService {

  private final IChoferesRepository choferesRepository;
  private final ChoferMapper choferMapper;

  public ChoferService(IChoferesRepository choferesRepository, ChoferMapper choferMapper) {
    this.choferesRepository = choferesRepository;
    this.choferMapper = choferMapper;
  }

  @Override
  public ChoferResponseDTO crear(ChoferRequestDTO request) {
    Chofer chofer = choferMapper.toDomain(request);
    choferesRepository.save(chofer);
    return choferMapper.toResponseDTO(chofer);
  }

  @Override
  public List<ChoferResponseDTO> consultarTodos() {
    return choferesRepository.findAll().stream()
        .filter(c -> c.getEstado() != EstadoChofer.DESHABILITADO)
        .map(choferMapper::toResponseDTO)
        .toList();
  }

  @Override
  public ChoferResponseDTO consultarPorId(UUID id) {
    return choferMapper.toResponseDTO(buscarChoferActivo(id));
  }

  @Override
  public ChoferResponseDTO cambiarEstado(UUID id, CambioEstadoChoferRequestDTO request) {
    // Sin filtro de activo: el dominio valida si la transición es válida, incluyendo
    // DESHABILITADO -> DISPONIBLE (habilitar).
    Chofer chofer =
        choferesRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(id));

    // INICIO LOGICA DE NEGOCIO
    switch (request.estado()) {
      case DISPONIBLE -> chofer.habilitar();
      case DESHABILITADO -> chofer.deshabilitar();
      case EN_RUTA -> throw new ValidationException(ErrorCatalog.ESTADO_CHOFER_TRANSICION_INVALIDA);
    }

    // FIN LOGICA DE NEGOCIO
    choferesRepository.save(chofer);
    return choferMapper.toResponseDTO(chofer);
  }

  @Override
  public void darDeBaja(UUID id) {
    Chofer chofer = buscarChoferActivo(id);
    chofer.deshabilitar();
    choferesRepository.save(chofer);
  }

  private Chofer buscarChoferActivo(UUID id) {
    Chofer chofer =
        choferesRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(id));

    if (chofer.getEstado() == EstadoChofer.DESHABILITADO) {
      throw new RecursoNoEncontradoException(id);
    }

    return chofer;
  }
}
