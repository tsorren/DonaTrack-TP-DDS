package grupo5.incentivos.services;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.incentivos.dto.DonanteRegistradoDTO;
import grupo5.incentivos.dto.ModificarDonanteRequest;
import grupo5.incentivos.dto.RegistrarDonanteRequest;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import grupo5.incentivos.models.repositories.IDonanteIncentivosRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class GestionDonanteService implements IGestionDonanteService {

  private final IDonanteIncentivosRepository repository;

  public GestionDonanteService(IDonanteIncentivosRepository repository) {
    this.repository = repository;
  }

  @Override
  public DonanteRegistradoDTO registrarDonante(RegistrarDonanteRequest request) {
    DonanteIncentivos donante =
        repository
            .findById(request.idDonante())
            .orElseGet(
                () -> {
                  DonanteIncentivos nuevo =
                      new DonanteIncentivos(
                          request.idDonante(), request.idPersona(), request.nombre());
                  repository.save(nuevo);
                  return nuevo;
                });
    return DonanteRegistradoDTO.desde(donante);
  }

  @Override
  public void modificarDonante(UUID donanteId, ModificarDonanteRequest request) {
    DonanteIncentivos donante = obtenerDonante(donanteId);
    donante.cambiarNombre(request.nombre());
    repository.save(donante);
  }

  @Override
  public DonanteIncentivos obtenerDonante(UUID donanteId) {
    return repository
        .findById(donanteId)
        .orElseThrow(
            () -> new BusinessStateException(ErrorCatalog.DONANTE_INCENTIVOS_NO_ENCONTRADO));
  }

  @Override
  public void darDeBaja(UUID donanteId) {
    DonanteIncentivos donante = obtenerDonante(donanteId);
    repository.delete(donante);
  }

  @Override
  public List<DonanteIncentivos> listarTodos() {
    return repository.findAll();
  }
}
