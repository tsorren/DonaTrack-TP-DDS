package grupo5.donaciones.services.impl;

import grupo5.donaciones.dto.donaciones.outputs.ContadoresPublicosDTO;
import grupo5.donaciones.models.entities.donacionesIndependientes.TipoEstadoDonacion;
import grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository;
import grupo5.donaciones.models.repositories.IDonacionesRepository;
import grupo5.donaciones.models.repositories.IDonantesRepository;
import grupo5.donaciones.services.IEntidadBeneficiariaService;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Contadores públicos de la landing (HB-02). */
@Service
public class ContadoresPublicosService {

  private final IDonantesRepository donantesRepository;
  private final IDonacionesRepository donacionesRepository;
  private final IDonacionesIndependientesRepository donacionesIndependientesRepository;
  private final IEntidadBeneficiariaService entidadBeneficiariaService;
  private final Clock reloj;

  @Autowired
  public ContadoresPublicosService(
      IDonantesRepository donantesRepository,
      IDonacionesRepository donacionesRepository,
      IDonacionesIndependientesRepository donacionesIndependientesRepository,
      IEntidadBeneficiariaService entidadBeneficiariaService) {
    this(
        donantesRepository,
        donacionesRepository,
        donacionesIndependientesRepository,
        entidadBeneficiariaService,
        Clock.systemDefaultZone());
  }

  ContadoresPublicosService(
      IDonantesRepository donantesRepository,
      IDonacionesRepository donacionesRepository,
      IDonacionesIndependientesRepository donacionesIndependientesRepository,
      IEntidadBeneficiariaService entidadBeneficiariaService,
      Clock reloj) {
    this.donantesRepository = donantesRepository;
    this.donacionesRepository = donacionesRepository;
    this.donacionesIndependientesRepository = donacionesIndependientesRepository;
    this.entidadBeneficiariaService = entidadBeneficiariaService;
    this.reloj = reloj;
  }

  public ContadoresPublicosDTO calcular() {
    Set<UUID> conDonaciones = donacionesRepository.idsDeDonantes();
    // ponytail: recorre todos los donantes en memoria (fila chica: id y activo); pasar a un count
    // con join si el volumen lo pide.
    long donantesActivos =
        donantesRepository.findAll().stream()
            .filter(d -> d.estaActivo() && conDonaciones.contains(d.getId()))
            .count();
    return new ContadoresPublicosDTO(
        donantesActivos,
        donacionesIndependientesRepository.contarEnEstado(TipoEstadoDonacion.ENTREGADA),
        entidadBeneficiariaService.buscarPublicas(null, 0, 1).total(),
        OffsetDateTime.now(reloj));
  }
}
