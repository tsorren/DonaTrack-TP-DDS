package grupo5.cliente.rest;

import static grupo5.cliente.rest.Comun.ASIGNADA;
import static grupo5.cliente.rest.Comun.ENTREGADA;
import static grupo5.cliente.rest.Comun.FALLIDA;
import static grupo5.cliente.rest.Comun.LISTA;
import static grupo5.cliente.rest.Comun.TRASLADO;
import static grupo5.cliente.rest.Comun.VENCIDA;
import static grupo5.cliente.rest.Comun.cantidad;
import static grupo5.cliente.rest.Comun.categoria;
import static grupo5.cliente.rest.Comun.desde;
import static grupo5.cliente.rest.Comun.icono;
import static grupo5.cliente.rest.Comun.normalizado;

import grupo5.cliente.puertos.RecibidasPuerto;
import grupo5.cliente.rest.DonacionesApi.Categoria;
import grupo5.cliente.vistas.RecibidasVista;
import grupo5.cliente.vistas.RecibidasVista.Recibida;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Donaciones recibidas desde Donaciones: las asignadas a la entidad, agrupadas por estado, con
 * quién donó ({@code quienDono} del listado: la entidad no puede leer la donación original). Las
 * recibidas son las entregadas en el mes en curso. Donaciones no distingue la que llegó a la puerta
 * de la que viene en camino (las dos están en traslado): todas van a "En camino" y "Esperan tu
 * confirmación" queda vacío. El pedido que cubre no llega (HB-17), ni cuántas notificaciones hay
 * sin leer.
 */
// @Primary solo para el análisis estático (S9352): la propiedad ya deja un único adaptador activo.
@Component
@Primary
@ConditionalOnProperty(name = "cliente.datos.recibidas", havingValue = "rest")
class RecibidasRest implements RecibidasPuerto {

  private static final Locale ES_AR = Locale.forLanguageTag("es-AR");
  private static final ZoneId ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");
  private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("d 'de' MMMM", ES_AR);
  private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("H:mm", ES_AR);
  private static final DateTimeFormatter MES = DateTimeFormatter.ofPattern("MMMM", ES_AR);
  private static final DateTimeFormatter AHORA =
      DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy, H:mm", ES_AR);
  private static final String RECIBIDA = "recibida";
  private static final String SALIR = "salir";
  private static final String UN_DONANTE = "un donante";

  /** El orden de lo que hay que hacer; las recibidas, al final. */
  private static final List<String> ORDEN =
      List.of("puerta", "problema", "camino", SALIR, RECIBIDA);

  private final DonacionesApi api;
  private final Clock reloj;

  RecibidasRest(DonacionesApi api, Clock reloj) {
    this.api = api;
    this.reloj = reloj;
  }

  /** Una donación con su estado normalizado, su grupo y desde cuándo está así. */
  private record Fila(DonacionesApi.Donacion d, String estado, String grupo, LocalDateTime desde) {}

  @Override
  public RecibidasVista recibidas(UUID entidad) {
    LocalDateTime ahora = LocalDateTime.now(reloj.withZone(ARGENTINA));
    YearMonth mes = YearMonth.from(ahora);
    List<Recibida> asignadas =
        api.asignadas(entidad).stream()
            .map(RecibidasRest::fila)
            .filter(f -> !RECIBIDA.equals(f.grupo()) || YearMonth.from(f.desde()).equals(mes))
            .sorted(
                Comparator.comparing((Fila f) -> ORDEN.indexOf(f.grupo()))
                    .thenComparing(Fila::desde, Comparator.reverseOrder()))
            .map(RecibidasRest::recibida)
            .toList();
    return new RecibidasVista(asignadas, ahora.format(MES), ahora.format(AHORA), null);
  }

  private static Fila fila(DonacionesApi.Donacion d) {
    String estado = normalizado(d.estadoActual());
    String grupo =
        switch (estado) {
          case ENTREGADA -> RECIBIDA;
          case FALLIDA, VENCIDA -> "problema";
          case TRASLADO -> "camino";
          default -> SALIR;
        };
    // Una en depósito con entidad todavía no tiene su cambio a asignada: vale el registro.
    String desdeQue = SALIR.equals(grupo) && !LISTA.equals(estado) ? ASIGNADA : estado;
    return new Fila(d, estado, grupo, desde(d, desdeQue));
  }

  private static Recibida recibida(Fila f) {
    DonacionesApi.Donacion d = f.d();
    Categoria categoria = categoria(d);
    boolean recibida = RECIBIDA.equals(f.grupo());
    String dono = donante(d);
    return new Recibida(
        d.id().toString(),
        d.descripcion(),
        categoria == null ? null : icono(categoria.nombre()),
        categoria == null ? null : categoria.nombre(),
        cantidad(d.cantidad(), categoria),
        dono,
        null,
        null,
        f.grupo(),
        paso(f.estado(), f.desde()),
        recibida ? String.valueOf(f.desde().getDayOfMonth()) : null,
        recibida ? f.desde().toLocalDate().toString() : null);
  }

  private static String paso(String estado, LocalDateTime desde) {
    String dia = desde.format(DIA);
    return switch (estado) {
      case ENTREGADA -> "Confirmaste la recepción el " + dia + ".";
      case FALLIDA -> "El camión no pudo llegar el " + dia + ".";
      case VENCIDA -> "Venció en el depósito el " + dia + ".";
      case TRASLADO -> "Salió del depósito el " + dia + " a las " + desde.format(HORA) + ".";
      case LISTA -> "Lista para entregar desde el " + dia + ". Espera el camión.";
      default -> "Asignada el " + dia + ". Espera el camión.";
    };
  }

  /** El nombre visible de quien donó; si lo ocultó o no llega, uno. */
  private static String donante(DonacionesApi.Donacion d) {
    String nombre = d.quienDono() == null ? null : d.quienDono().nombreVisible();
    return nombre == null || nombre.isBlank() ? UN_DONANTE : nombre.strip();
  }
}
