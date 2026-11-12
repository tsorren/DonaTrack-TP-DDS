package grupo5.donaciones.services.impl;

import grupo5.donaciones.dto.donaciones.outputs.EvolucionAdminDTO;
import grupo5.donaciones.dto.donaciones.outputs.EvolucionAdminDTO.Categoria;
import grupo5.donaciones.dto.donaciones.outputs.EvolucionAdminDTO.Mes;
import grupo5.donaciones.dto.donaciones.outputs.EvolucionAdminDTO.Otras;
import grupo5.donaciones.models.entities.donacionesIndependientes.TipoEstadoDonacion;
import grupo5.donaciones.models.repositories.ICategoriasRepository;
import grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Los gráficos del inicio del admin (R11-42): cuatro conteos con {@code GROUP BY} en la base, sin
 * traer ninguna lista de donaciones. Los meses salen del reloj del servicio, como el resumen; si la
 * primera donación es de hace menos de 12 meses, empiezan en el suyo (antes no había plataforma).
 */
@Service
public class EvolucionAdminService {

  /** Cuántos meses mira, contando el actual. */
  public static final int MESES = 12;

  /** Cuántas categorías van con su nombre; el resto se suma en "otras". */
  public static final int CATEGORIAS = 8;

  private final IDonacionesIndependientesRepository donacionesIndependientes;
  private final ICategoriasRepository categorias;
  private final Clock reloj;

  public EvolucionAdminService(
      IDonacionesIndependientesRepository donacionesIndependientes,
      ICategoriasRepository categorias,
      Clock reloj) {
    this.donacionesIndependientes = donacionesIndependientes;
    this.categorias = categorias;
    this.reloj = reloj;
  }

  public EvolucionAdminDTO calcular() {
    OffsetDateTime calculadoEn = OffsetDateTime.now(reloj);
    Optional<LocalDateTime> primera = donacionesIndependientes.primeraFechaRegistro();
    if (primera.isEmpty()) {
      return new EvolucionAdminDTO(null, null, List.of(), List.of(), null, calculadoEn);
    }
    YearMonth hasta = YearMonth.now(reloj);
    YearMonth desde = hasta.minusMonths(MESES - 1L);
    YearMonth dePrimera = YearMonth.from(primera.get());
    if (dePrimera.isAfter(desde)) {
      desde = dePrimera.isAfter(hasta) ? hasta : dePrimera;
    }
    LocalDateTime inicio = desde.atDay(1).atStartOfDay();
    LocalDateTime fin = hasta.plusMonths(1).atDay(1).atStartOfDay();
    List<Categoria> todas = porCategoria(inicio, fin);
    List<Categoria> primeras = todas.stream().limit(CATEGORIAS).toList();
    return new EvolucionAdminDTO(
        desde.toString(),
        hasta.toString(),
        meses(desde, hasta),
        primeras,
        otras(todas.subList(primeras.size(), todas.size())),
        calculadoEn);
  }

  private List<Mes> meses(YearMonth desde, YearMonth hasta) {
    Map<YearMonth, Long> registradas =
        donacionesIndependientes.contarRegistradasPorMes(desde, hasta);
    Map<YearMonth, Long> entregadas =
        donacionesIndependientes.contarQuePasaronAPorMes(
            TipoEstadoDonacion.ENTREGADA, desde, hasta);
    List<Mes> meses = new ArrayList<>();
    for (YearMonth mes = desde; !mes.isAfter(hasta); mes = mes.plusMonths(1)) {
      meses.add(
          new Mes(
              mes.toString(), registradas.getOrDefault(mes, 0L), entregadas.getOrDefault(mes, 0L)));
    }
    return meses;
  }

  /** Todas las del catálogo, también las que dieron 0, de más a menos registradas y por nombre. */
  private List<Categoria> porCategoria(LocalDateTime inicio, LocalDateTime fin) {
    Map<UUID, Long> registradas =
        donacionesIndependientes.contarRegistradasPorCategoria(inicio, fin);
    Map<UUID, Long> entregadas =
        donacionesIndependientes.contarQuePasaronAPorCategoria(
            TipoEstadoDonacion.ENTREGADA, inicio, fin);
    return categorias.findAll().stream()
        .map(
            c ->
                new Categoria(
                    c.getId(),
                    c.getNombre(),
                    registradas.getOrDefault(c.getId(), 0L),
                    entregadas.getOrDefault(c.getId(), 0L)))
        .sorted(
            Comparator.comparingLong(Categoria::registradas)
                .reversed()
                .thenComparing(Categoria::nombre, String.CASE_INSENSITIVE_ORDER))
        .toList();
  }

  private static Otras otras(List<Categoria> resto) {
    if (resto.isEmpty()) {
      return null;
    }
    return new Otras(
        resto.size(),
        resto.stream().mapToLong(Categoria::registradas).sum(),
        resto.stream().mapToLong(Categoria::entregadas).sum());
  }
}
