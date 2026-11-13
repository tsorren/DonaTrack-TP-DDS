package grupo5.donaciones.services.impl;

import grupo5.donaciones.dto.donaciones.outputs.ContadoresPublicosDTO;
import grupo5.donaciones.models.entities.donacionesIndependientes.TipoEstadoDonacion;
import grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository;
import grupo5.donaciones.models.repositories.IDonantesRepository;
import grupo5.donaciones.models.repositories.IEntidadesBeneficiariasRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Contadores públicos de la portada (HB-02, RF-01). Cada cifra es una consulta de conteo en la base
 * (R11-01): ninguna recorre una lista.
 */
@Service
public class ContadoresPublicosService {

  private final IDonantesRepository donantesRepository;
  private final IDonacionesIndependientesRepository donacionesIndependientesRepository;
  private final IEntidadesBeneficiariasRepository entidadesRepository;
  private final Clock reloj;

  @Autowired
  public ContadoresPublicosService(
      IDonantesRepository donantesRepository,
      IDonacionesIndependientesRepository donacionesIndependientesRepository,
      IEntidadesBeneficiariasRepository entidadesRepository) {
    this(
        donantesRepository,
        donacionesIndependientesRepository,
        entidadesRepository,
        Clock.systemDefaultZone());
  }

  ContadoresPublicosService(
      IDonantesRepository donantesRepository,
      IDonacionesIndependientesRepository donacionesIndependientesRepository,
      IEntidadesBeneficiariasRepository entidadesRepository,
      Clock reloj) {
    this.donantesRepository = donantesRepository;
    this.donacionesIndependientesRepository = donacionesIndependientesRepository;
    this.entidadesRepository = entidadesRepository;
    this.reloj = reloj;
  }

  public ContadoresPublicosDTO calcular() {
    return new ContadoresPublicosDTO(
        donantesRepository.contarActivosConDonaciones(),
        donacionesIndependientesRepository.contarEnEstado(TipoEstadoDonacion.ENTREGADA),
        entidadesRepository.contarActivas(),
        OffsetDateTime.now(reloj));
  }
}
