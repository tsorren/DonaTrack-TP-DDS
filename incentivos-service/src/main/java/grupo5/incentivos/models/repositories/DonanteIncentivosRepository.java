package grupo5.incentivos.models.repositories;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.repositories.CrudRepositoryEnMemoria;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!postgres")
public class DonanteIncentivosRepository extends CrudRepositoryEnMemoria<DonanteIncentivos>
    implements IDonanteIncentivosRepository {
  @Override
  public Optional<DonanteIncentivos> findByIdPersona(UUID idPersona) {
    if (idPersona == null) {
      return Optional.empty();
    }
    return storage.values().stream()
        .filter(donante -> idPersona.equals(donante.getIdPersona()))
        .findFirst();
  }

  @Override
  public boolean actualizarNombre(UUID donanteId, String nombre) {
    Optional<DonanteIncentivos> donante = findById(donanteId);
    donante.ifPresent(
        d -> {
          d.cambiarNombre(nombre);
          save(d);
        });
    return donante.isPresent();
  }

  @Override
  public boolean actualizarVisibilidadInsignia(
      UUID donanteId, String nombreInsignia, boolean visible) {
    Optional<DonanteIncentivos> donante = findById(donanteId);
    if (donante.isEmpty()) {
      return false;
    }
    try {
      donante.get().configurarVisibilidadInsignia(nombreInsignia, visible);
    } catch (BusinessStateException e) {
      if (e.getError() == ErrorCatalog.INSIGNIA_NO_ENCONTRADA) {
        return false;
      }
      throw e;
    }
    save(donante.get());
    return true;
  }

  @Override
  public boolean eliminarPorId(UUID donanteId) {
    Optional<DonanteIncentivos> donante = findById(donanteId);
    donante.ifPresent(this::delete);
    return donante.isPresent();
  }
}
