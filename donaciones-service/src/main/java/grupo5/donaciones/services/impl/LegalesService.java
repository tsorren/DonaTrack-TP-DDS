package grupo5.donaciones.services.impl;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.dto.personas.LegalesDTO;
import grupo5.donaciones.models.entities.personas.AceptacionLegales;
import grupo5.donaciones.models.repositories.IAceptacionesLegalesRepository;
import grupo5.donaciones.models.repositories.IPersonasRepository;
import grupo5.donaciones.services.ILegalesService;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LegalesService implements ILegalesService {

  private final IAceptacionesLegalesRepository aceptaciones;
  private final IPersonasRepository personas;
  private final int versionVigente;

  public LegalesService(
      IAceptacionesLegalesRepository aceptaciones,
      IPersonasRepository personas,
      @Value("${donatrack.legales.version-vigente:1}") int versionVigente) {
    this.aceptaciones = aceptaciones;
    this.personas = personas;
    this.versionVigente = versionVigente;
  }

  @Override
  public LegalesDTO consultar(UUID personaId) {
    exigirPersona(personaId);
    return aDTO(aceptaciones.ultimaDe(personaId).orElse(null));
  }

  @Override
  public LegalesDTO aceptar(UUID personaId, int version) {
    exigirPersona(personaId);
    if (version != versionVigente) {
      throw new ValidationException(ErrorCatalog.LEGALES_VERSION_NO_VIGENTE);
    }
    AceptacionLegales ultima = aceptaciones.ultimaDe(personaId).orElse(null);
    if (ultima != null && ultima.version() == version) {
      return aDTO(ultima);
    }
    // ponytail: leer y después insertar. Dos aceptaciones simultáneas de la misma versión
    // chocan con el UNIQUE de V19 y una falla; capturar el choque y releer si pasa en la práctica.
    return aDTO(
        aceptaciones.save(
            new AceptacionLegales(personaId, version, LocalDateTime.now(ZoneId.systemDefault()))));
  }

  private void exigirPersona(UUID personaId) {
    if (!personas.existsById(personaId)) {
      throw new RecursoNoEncontradoException(personaId);
    }
  }

  private LegalesDTO aDTO(AceptacionLegales ultima) {
    if (ultima == null) {
      return new LegalesDTO(null, null, versionVigente, false);
    }
    return new LegalesDTO(
        ultima.version(), ultima.aceptadaEn(), versionVigente, ultima.version() == versionVigente);
  }
}
