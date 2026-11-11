package grupo5.cliente.rest;

import static grupo5.cliente.rest.Comun.ARGENTINA;
import static grupo5.cliente.rest.Comun.DIA;
import static grupo5.cliente.rest.Comun.DIA_Y_HORA;
import static grupo5.cliente.rest.Comun.ES_AR;

import grupo5.cliente.puertos.DonanteInicioPuerto;
import grupo5.cliente.vistas.DonanteDonacionVista;
import grupo5.cliente.vistas.DonanteInicioVista;
import grupo5.cliente.vistas.DonanteInicioVista.Aviso;
import grupo5.cliente.vistas.EstadoEntrega;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

/**
 * El inicio del donante: sus donaciones como las arma Seguimiento (Donaciones y, de las que van en
 * curso, Logística), las tres noticias más nuevas de su historial y las sin leer de Notificaciones
 * (0 si no responde). Las entregas del mes son las confirmadas en el mes de hoy.
 */
// @Primary solo para el análisis estático (S9352): la propiedad ya deja un único adaptador activo.
@Component
@Primary
@ConditionalOnProperty(name = "cliente.datos.donante", havingValue = "rest")
class DonanteInicioRest implements DonanteInicioPuerto {

  private static final DateTimeFormatter MES = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", ES_AR);
  private static final int NOTICIAS = 3;

  private final DonacionesApi api;
  private final LogisticaApi logistica;
  private final NotificacionesApi notificaciones;
  private final Clock reloj;

  DonanteInicioRest(
      DonacionesApi api, LogisticaApi logistica, NotificacionesApi notificaciones, Clock reloj) {
    this.api = api;
    this.logistica = logistica;
    this.notificaciones = notificaciones;
    this.reloj = reloj;
  }

  @Override
  public DonanteInicioVista inicio(UUID donante) {
    LocalDateTime ahora = LocalDateTime.now(reloj.withZone(ARGENTINA));
    LocalDate hoy = ahora.toLocalDate();
    List<DonanteDonacionVista> donaciones =
        DonanteDonaciones.de(
            api, donante, DonanteSeguimientoRest.seguimientos(logistica, donante), hoy);
    String mes = hoy.format(MES);
    // "5 de octubre de 2026" termina con el mes y el año de hoy si llegó este mes.
    int delMes =
        (int)
            donaciones.stream()
                .filter(d -> d.estado() == EstadoEntrega.ENTREGADA && d.llego() != null)
                .filter(d -> d.llego().endsWith(mes))
                .count();
    List<Aviso> avisos =
        donaciones.stream()
            .flatMap(d -> IntStream.range(0, d.cartas().size()).mapToObj(i -> new Aviso(d, i)))
            .sorted(Comparator.comparing((Aviso a) -> a.d().cartas().get(a.i()).fecha()).reversed())
            .limit(NOTICIAS)
            .toList();
    // La primera donación: "1 de octubre", con el año solo si no es el de hoy.
    String desde =
        donaciones.isEmpty()
            ? hoy.format(DIA)
            : donaciones.getLast().donada().replace(" de " + hoy.getYear(), "");
    return new DonanteInicioVista(
        donaciones,
        avisos,
        sinLeer(donante),
        delMes,
        mes,
        hoy.format(DIA),
        ahora.format(DIA_Y_HORA),
        desde,
        hoy.toString(),
        hoy.minusDays(1).toString());
  }

  private long sinLeer(UUID persona) {
    try {
      NotificacionesApi.Conteo conteo = notificaciones.sinLeer(persona);
      return conteo == null || conteo.noLeidas() == null ? 0 : conteo.noLeidas();
    } catch (RestClientException sinNotificaciones) {
      return 0;
    }
  }
}
