package grupo5.cliente.rest;

import grupo5.cliente.puertos.RankingPuerto;
import grupo5.cliente.vistas.RankingVista;
import grupo5.cliente.vistas.RankingVista.Mes;
import grupo5.cliente.vistas.RankingVista.Puesto;
import java.time.Clock;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** El ranking de Incentivos: una sola llamada al historial da el selector de meses y los datos. */
@Component
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
    List<Mes> meses =
        incentivos.historial().stream()
            .sorted(Comparator.comparing(IncentivosApi.RankingMensual::periodo).reversed())
            .map(RankingRest::mes)
            .toList();
    return new RankingVista(meses, YearMonth.now(reloj.withZone(ARGENTINA)).format(MES));
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
