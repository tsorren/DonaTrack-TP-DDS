package grupo5.donaciones.services.impl;

import grupo5.donaciones.dto.donaciones.outputs.ResumenAdminDTO;
import grupo5.donaciones.dto.donaciones.outputs.ResumenAdminDTO.Donantes;
import grupo5.donaciones.dto.donaciones.outputs.ResumenAdminDTO.EntregadasEnElMes;
import grupo5.donaciones.dto.donaciones.outputs.ResumenAdminDTO.Proxima;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.TipoEstadoDonacion;
import grupo5.donaciones.models.entities.propuestas.EstadoPropuesta;
import grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository;
import grupo5.donaciones.models.repositories.IDonantesRepository;
import grupo5.donaciones.models.repositories.IEntidadesBeneficiariasRepository;
import grupo5.donaciones.models.repositories.IPropuestasRepository;
import grupo5.donaciones.models.repositories.listados.PorVencer;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Las cifras del inicio del admin (R11-01): una consulta de conteo por cifra, ninguna lista. El mes
 * en curso y "por vencer" salen del reloj del servicio.
 */
@Service
public class ResumenAdminService {

  /** Cuántos días adelante mira "por vencer". */
  public static final int DIAS_POR_VENCER = 7;

  private final IDonacionesIndependientesRepository donacionesIndependientes;
  private final IDonantesRepository donantes;
  private final IEntidadesBeneficiariasRepository entidades;
  private final IPropuestasRepository propuestas;
  private final Clock reloj;

  public ResumenAdminService(
      IDonacionesIndependientesRepository donacionesIndependientes,
      IDonantesRepository donantes,
      IEntidadesBeneficiariasRepository entidades,
      IPropuestasRepository propuestas,
      Clock reloj) {
    this.donacionesIndependientes = donacionesIndependientes;
    this.donantes = donantes;
    this.entidades = entidades;
    this.propuestas = propuestas;
    this.reloj = reloj;
  }

  public ResumenAdminDTO calcular() {
    LocalDate hoy = LocalDate.now(reloj);
    YearMonth mes = YearMonth.from(hoy);
    return new ResumenAdminDTO(
        porEstado(),
        new EntregadasEnElMes(
            mes.toString(),
            donacionesIndependientes.contarQuePasaronA(
                TipoEstadoDonacion.ENTREGADA,
                mes.atDay(1).atStartOfDay(),
                mes.plusMonths(1).atDay(1).atStartOfDay())),
        porVencer(hoy),
        new Donantes(donantes.count(), donantes.contarActivos()),
        entidades.contarActivas(),
        propuestas.contarEnEstado(EstadoPropuesta.PENDIENTE),
        OffsetDateTime.now(reloj));
  }

  /** Los 8 estados siempre presentes, en el orden del ciclo de vida, con 0 si no hay. */
  private Map<String, Long> porEstado() {
    Map<TipoEstadoDonacion, Long> conteo = donacionesIndependientes.contarPorEstado();
    Map<String, Long> porEstado = new LinkedHashMap<>();
    for (TipoEstadoDonacion estado : TipoEstadoDonacion.values()) {
      porEstado.put(estado.name(), conteo.getOrDefault(estado, 0L));
    }
    return porEstado;
  }

  private ResumenAdminDTO.PorVencer porVencer(LocalDate hoy) {
    PorVencer porVencer =
        donacionesIndependientes.buscarPorVencer(hoy, hoy.plusDays(DIAS_POR_VENCER));
    Proxima proxima = null;
    if (porVencer.proximaId() != null) {
      String descripcion =
          donacionesIndependientes
              .findById(porVencer.proximaId())
              .map(DonacionIndependiente::getDescripcion)
              .map(String::trim)
              .orElse(null);
      proxima = new Proxima(porVencer.proximaId(), descripcion, porVencer.proximaVence());
    }
    return new ResumenAdminDTO.PorVencer(DIAS_POR_VENCER, porVencer.cantidad(), proxima);
  }
}
