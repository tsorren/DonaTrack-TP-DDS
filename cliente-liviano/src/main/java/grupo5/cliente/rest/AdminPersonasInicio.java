package grupo5.cliente.rest;

import grupo5.cliente.vistas.AdminInicioVista;
import grupo5.cliente.vistas.AdminInicioVista.Cifras;
import grupo5.cliente.vistas.AdminInicioVista.Decisiones;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * El inicio del admin con lo que dan Donaciones y Logística: las solicitudes (con el nombre que ya
 * traen), las propuestas pendientes y las cifras. Desde R11-01 las cifras y lo que vence son
 * números de {@code GET /api/donaciones/resumen}: ninguna lista se trae para contarla. Lo que no se
 * puede saber (cuándo llegó la solicitud más antigua, qué le falta mandar, la entidad y la unidad
 * de una propuesta) no se escribe.
 */
final class AdminPersonasInicio {

  private static final DateTimeFormatter FECHA =
      DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", Comun.ES_AR);
  private static final DateTimeFormatter MES = DateTimeFormatter.ofPattern("MMMM", Comun.ES_AR);

  private final DonacionesApi donaciones;
  private final LogisticaApi logistica;

  AdminPersonasInicio(DonacionesApi donaciones, LogisticaApi logistica) {
    this.donaciones = donaciones;
    this.logistica = logistica;
  }

  /** Arma el inicio con los nombres de las entidades que piden sumarse. */
  AdminInicioVista inicio(LocalDateTime ahora, String al, List<String> pendientes) {
    LocalDate hoy = ahora.toLocalDate();
    DonacionesApi.Resumen resumen = donaciones.resumen();
    Map<String, Long> porEstado =
        resumen.donacionesPorEstado() == null ? Map.of() : resumen.donacionesPorEstado();
    DonacionesApi.PorVencer porVencer = resumen.porVencer();
    long vencen = porVencer == null ? 0 : numero(porVencer.cantidad());
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
            (int) vencen,
            vencen == 0 || porVencer.proxima() == null ? null : primera(porVencer.proxima(), hoy),
            propuestas.size(),
            propuestas);
    List<LogisticaApi.Camion> camiones = logistica.camiones(null);
    DonacionesApi.EntregadasEnElMes entregadas = resumen.entregadasEnElMes();
    Cifras cifras =
        new Cifras(
            contar(porEstado, "EN_DEPOSITO"),
            contar(porEstado, "ASIGNACION_REALIZADA") + contar(porEstado, "LISTA_PARA_ENTREGAR"),
            contar(porEstado, "EN_TRASLADO") + contar(porEstado, "EN_PUERTA"),
            entregadas == null ? 0 : numero(entregadas.cantidad()),
            MES.format(hoy),
            numero(resumen.entidadesActivas()),
            resumen.donantes() == null ? 0 : numero(resumen.donantes().activos()),
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

  private static long contar(Map<String, Long> porEstado, String estado) {
    return numero(porEstado.get(estado));
  }

  private static long numero(Long valor) {
    return valor == null ? 0 : valor;
  }

  /** "La primera, Leche en polvo, vence el 13 de noviembre, en 2 días." */
  private static String primera(DonacionesApi.Proxima proxima, LocalDate hoy) {
    LocalDate fecha = proxima.fechaVencimiento();
    if (fecha == null) {
      return null;
    }
    long dias = ChronoUnit.DAYS.between(hoy, fecha);
    String cuando =
        switch ((int) dias) {
          case 0 -> "vence hoy";
          case 1 -> "vence el " + Comun.DIA.format(fecha) + ", en 1 día";
          default -> "vence el " + Comun.DIA.format(fecha) + ", en " + dias + " días";
        };
    return proxima.descripcion() == null || proxima.descripcion().isBlank()
        ? "La primera " + cuando + "."
        : "La primera, " + proxima.descripcion() + ", " + cuando + ".";
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
