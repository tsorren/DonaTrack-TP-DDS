package grupo5.cliente.rest;

import grupo5.cliente.puertos.RankingPuerto;
import grupo5.cliente.vistas.RankingVista;
import grupo5.cliente.vistas.RankingVista.Mes;
import grupo5.cliente.vistas.RankingVista.Puesto;
import grupo5.cliente.vistas.Tramo;
import java.time.Clock;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

/**
 * El ranking de Incentivos: una página del historial (R11-43, de a {@value RankingVista#POR_PAGINA}
 * meses, del más reciente al más viejo) da el selector de meses y los datos. Si el mes pedido no
 * está en esa página, se pide solo ({@code /ranking/{periodo}}); si no existe, el primero de la
 * página.
 */
// @Primary solo para el análisis estático (S9352): la propiedad ya deja un único adaptador activo.
@Component
@Primary
@ConditionalOnProperty(name = "cliente.datos.ranking", havingValue = "rest")
class RankingRest implements RankingPuerto {

  private static final Locale ES_AR = Locale.forLanguageTag("es-AR");
  private static final ZoneId ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");
  private static final DateTimeFormatter MES_Y_ANIO =
      DateTimeFormatter.ofPattern("MMMM 'de' yyyy", ES_AR);
  private static final DateTimeFormatter MES = DateTimeFormatter.ofPattern("MMMM", ES_AR);
  private static final DateTimeFormatter CALCULADO =
      DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy, HH:mm", ES_AR).withZone(ARGENTINA);

  private final IncentivosApi incentivos;
  private final Clock reloj;

  RankingRest(IncentivosApi incentivos, Clock reloj) {
    this.incentivos = incentivos;
    this.reloj = reloj;
  }

  @Override
  public RankingVista ranking() {
    return ranking(null, 1);
  }

  @Override
  public RankingVista ranking(String mes, int pagina) {
    Tramo<IncentivosApi.RankingMensual> tramo =
        Paginas.pedir(
            pagina, RankingVista.POR_PAGINA, p -> incentivos.historial(p, RankingVista.POR_PAGINA));
    List<Mes> meses =
        tramo.filas().stream()
            .sorted(Comparator.comparing(IncentivosApi.RankingMensual::periodo).reversed())
            .map(RankingRest::mes)
            .toList();
    return new RankingVista(
        meses,
        YearMonth.now(reloj.withZone(ARGENTINA)).format(MES),
        elegido(mes, meses),
        tramo.total(),
        tramo.pagina(),
        tramo.paginas());
  }

  /**
   * El mes pedido: de la página si está; si no, pedido aparte. Sin pedido, con una forma que no es
   * {@code 2026-09} o un mes que no se calculó, el primero de la página (o ninguno).
   */
  private Mes elegido(String pedido, List<Mes> meses) {
    Mes primero = meses.isEmpty() ? null : meses.getFirst();
    if (pedido == null || !pedido.matches("\\d{4}-(0[1-9]|1[0-2])")) {
      return primero;
    }
    Optional<Mes> enLaPagina = meses.stream().filter(m -> m.clave().equals(pedido)).findFirst();
    if (enLaPagina.isPresent()) {
      return enLaPagina.get();
    }
    try {
      IncentivosApi.RankingMensual otro = incentivos.rankingDe(pedido);
      return otro == null ? primero : mes(otro);
    } catch (HttpClientErrorException.NotFound noSeCalculo) {
      return primero;
    }
  }

  private static Mes mes(IncentivosApi.RankingMensual ranking) {
    String nombre = YearMonth.parse(ranking.periodo()).format(MES_Y_ANIO);
    return new Mes(
        ranking.periodo(),
        Character.toUpperCase(nombre.charAt(0)) + nombre.substring(1),
        ranking.calculadoEn() == null ? null : CALCULADO.format(ranking.calculadoEn()),
        ranking.entradas().stream()
            .sorted(Comparator.comparingInt(IncentivosApi.Entrada::posicion))
            .map(entrada -> new Puesto(entrada.nombreDonante(), entrada.misionesCompletadas()))
            .toList());
  }
}
