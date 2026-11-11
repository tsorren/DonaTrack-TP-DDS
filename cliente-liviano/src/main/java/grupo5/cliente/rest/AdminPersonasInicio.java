package grupo5.cliente.rest;

import grupo5.cliente.rest.DonacionesApi.Donacion;
import grupo5.cliente.vistas.AdminEntidadesVista;
import grupo5.cliente.vistas.AdminEntidadesVista.Entidad;
import grupo5.cliente.vistas.AdminInicioVista;
import grupo5.cliente.vistas.AdminInicioVista.Cifras;
import grupo5.cliente.vistas.AdminInicioVista.Decisiones;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.util.LinkedMultiValueMap;

/**
 * El inicio del admin con lo que dan Donaciones y Logística: las solicitudes, las donaciones del
 * depósito que vencen en 7 días, las propuestas pendientes y las cifras, contando listas. Lo que no
 * se puede saber (cuándo llegó la solicitud más antigua, qué le falta mandar, la entidad y la
 * unidad de una propuesta) no se escribe.
 */
final class AdminPersonasInicio {

  private static final DateTimeFormatter FECHA =
      DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", Comun.ES_AR);
  private static final DateTimeFormatter MES = DateTimeFormatter.ofPattern("MMMM", Comun.ES_AR);
  private static final String EN_DEPOSITO = "ENDEPOSITO";
  private static final int DIAS_PARA_VENCER = 7;

  private final DonacionesApi donaciones;
  private final LogisticaApi logistica;

  AdminPersonasInicio(DonacionesApi donaciones, LogisticaApi logistica) {
    this.donaciones = donaciones;
    this.logistica = logistica;
  }

  /** Arma el inicio con las entidades ya leídas y la cuenta de donantes activos. */
  AdminInicioVista inicio(
      LocalDateTime ahora, String al, AdminEntidadesVista entidades, long donantes) {
    LocalDate hoy = ahora.toLocalDate();
    List<String> pendientes =
        entidades.todas().stream()
            .filter(e -> "pendiente".equals(e.estado()))
            .map(Entidad::nombre)
            .toList();
    List<Donacion> todas = donaciones.buscar(new LinkedMultiValueMap<>());
    List<Donacion> vencen =
        todas.stream()
            .filter(d -> EN_DEPOSITO.equals(Comun.normalizado(d.estadoActual())))
            .filter(d -> vence(d, hoy) != null)
            .sorted(Comparator.comparing(d -> vence(d, hoy)))
            .toList();
    List<String> propuestas =
        donaciones.propuestas().stream()
            .filter(p -> "PENDIENTE".equals(p.estado()) && p.necesidad() != null)
            .map(p -> propuesta(p.necesidad()))
            .toList();
    Decisiones decisiones =
        new Decisiones(
            pendientes.size(),
            pendientes,
            null,
            vencen.size(),
            vencen.isEmpty() ? null : primera(vencen.get(0), hoy),
            propuestas.size(),
            propuestas);
    List<LogisticaApi.Camion> camiones = logistica.camiones(null);
    YearMonth mes = YearMonth.from(hoy);
    Cifras cifras =
        new Cifras(
            contar(todas, EN_DEPOSITO),
            contar(todas, Comun.ASIGNADA) + contar(todas, Comun.LISTA),
            contar(todas, Comun.TRASLADO),
            todas.stream()
                .filter(d -> Comun.ENTREGADA.equals(Comun.normalizado(d.estadoActual())))
                .map(d -> Comun.desde(d, Comun.ENTREGADA))
                .filter(cuando -> cuando != null && YearMonth.from(cuando).equals(mes))
                .count(),
            MES.format(hoy),
            entidades.activas(),
            donantes,
            camiones.stream().filter(c -> "EN_RUTA".equals(c.estado())).count(),
            camiones.stream().filter(c -> "DISPONIBLE".equals(c.estado())).count());
    String fecha = FECHA.format(hoy);
    return new AdminInicioVista(
        Character.toUpperCase(fecha.charAt(0)) + fecha.substring(1),
        resumen(decisiones),
        decisiones,
        al,
        cifras);
  }

  private static long contar(List<Donacion> donaciones, String estado) {
    return donaciones.stream()
        .filter(d -> estado.equals(Comun.normalizado(d.estadoActual())))
        .count();
  }

  /** La fecha de vencimiento más cercana de sus ítems, si cae entre hoy y dentro de 7 días. */
  private static LocalDate vence(Donacion d, LocalDate hoy) {
    if (d.items() == null) {
      return null;
    }
    return d.items().stream()
        .filter(i -> i != null && i.bien() != null && i.bien().bien() != null)
        .map(i -> i.bien().bien().fechaVencimiento())
        .filter(Objects::nonNull)
        .filter(f -> !f.isBefore(hoy) && !f.isAfter(hoy.plusDays(DIAS_PARA_VENCER)))
        .min(Comparator.naturalOrder())
        .orElse(null);
  }

  private static String primera(Donacion d, LocalDate hoy) {
    LocalDate fecha = vence(d, hoy);
    long dias = ChronoUnit.DAYS.between(hoy, fecha);
    String cuando =
        switch ((int) dias) {
          case 0 -> "vence hoy";
          case 1 -> "vence el " + Comun.DIA.format(fecha) + ", en 1 día";
          default -> "vence el " + Comun.DIA.format(fecha) + ", en " + dias + " días";
        };
    String que = d.descripcion() == null || d.descripcion().isBlank() ? "una" : d.descripcion();
    return "La primera, " + que + ", " + cuando + ".";
  }

  private static String propuesta(DonacionesApi.NecesidadResumen necesidad) {
    String que = necesidad.descripcion() == null ? "Una necesidad" : necesidad.descripcion();
    return necesidad.cantidadNecesitada() == null
        ? que + "."
        : que + ", " + necesidad.cantidadNecesitada() + ".";
  }

  /** La frase de la cabecera, o null si no hay nada que decidir. */
  static String resumen(Decisiones d) {
    List<String> esperan = new ArrayList<>();
    if (d.entidades() > 0) {
      esperan.add(d.entidades() == 1 ? "1 entidad" : d.entidades() + " entidades");
    }
    if (d.propuestas() > 0) {
      esperan.add(
          d.propuestas() == 1
              ? "1 propuesta de asignación"
              : d.propuestas() + " propuestas de asignación");
    }
    List<String> frases = new ArrayList<>();
    if (!esperan.isEmpty()) {
      boolean una = d.entidades() + d.propuestas() == 1;
      frases.add(
          "Hoy "
              + (una ? "espera" : "esperan")
              + " tu decisión "
              + String.join(" y ", esperan)
              + ".");
    }
    if (d.vencen() > 0) {
      frases.add(
          d.vencen() == 1
              ? "1 donación vence en los próximos 7 días."
              : d.vencen() + " donaciones vencen en los próximos 7 días.");
    }
    return frases.isEmpty() ? null : String.join(" ", frases);
  }
}
