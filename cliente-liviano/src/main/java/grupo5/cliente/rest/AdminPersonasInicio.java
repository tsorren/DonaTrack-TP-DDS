package grupo5.cliente.rest;

import grupo5.cliente.vistas.AdminInicioVista;
import grupo5.cliente.vistas.AdminInicioVista.Camiones;
import grupo5.cliente.vistas.AdminInicioVista.Cifras;
import grupo5.cliente.vistas.AdminInicioVista.Decisiones;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.web.client.RestClientException;

/**
 * El inicio del admin por temas (R11-21) con lo que dan Donaciones y Logística: las solicitudes
 * (con el nombre que ya traen, como mucho 3), y las cifras y lo que espera, que son números de
 * {@code GET /api/donaciones/resumen}: ninguna lista se trae para contarla, tampoco las propuestas
 * (DTI-40, ítem 1). Los camiones se cuentan sobre la flota hasta que Logística dé el número; si no
 * responde, solo esa tarjeta queda sin datos.
 */
final class AdminPersonasInicio {

  private static final int NOMBRES = 3;
  private static final DateTimeFormatter FECHA =
      DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", Comun.ES_AR);
  private static final DateTimeFormatter MES = DateTimeFormatter.ofPattern("MMMM", Comun.ES_AR);
  private static final DateTimeFormatter AL =
      DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy, HH:mm", Comun.ES_AR);

  private final DonacionesApi donaciones;
  private final LogisticaApi logistica;

  AdminPersonasInicio(DonacionesApi donaciones, LogisticaApi logistica) {
    this.donaciones = donaciones;
    this.logistica = logistica;
  }

  /**
   * Arma el inicio con los nombres de las entidades que piden sumarse. {@code al} es la hora de
   * lectura si el resumen no trae la suya ({@code calculadoEn}).
   */
  AdminInicioVista inicio(LocalDateTime ahora, String al, List<String> pendientes) {
    LocalDate hoy = ahora.toLocalDate();
    DonacionesApi.Resumen resumen = donaciones.resumen();
    LocalDateTime calculado =
        resumen.calculadoEn() == null
            ? null
            : resumen.calculadoEn().atZoneSameInstant(Comun.ARGENTINA).toLocalDateTime();
    LocalDate dia = calculado == null ? hoy : calculado.toLocalDate();
    Map<String, Long> porEstado =
        resumen.donacionesPorEstado() == null ? Map.of() : resumen.donacionesPorEstado();
    DonacionesApi.PorVencer porVencer = resumen.porVencer();
    long vencen = porVencer == null ? 0 : numero(porVencer.cantidad());
    List<String> nombres = pendientes.stream().limit(NOMBRES).toList();
    Decisiones decisiones =
        new Decisiones(
            pendientes.size(),
            nombres,
            (long) pendientes.size() - nombres.size(),
            contar(porEstado, "EN_DEPOSITO"),
            vencen,
            vencen == 0 || porVencer.proxima() == null ? null : primera(porVencer.proxima(), hoy),
            numero(resumen.propuestasPendientes()));
    DonacionesApi.EntregadasEnElMes entregadas = resumen.entregadasEnElMes();
    DonacionesApi.CuantosDonantes donantes = resumen.donantes();
    Cifras cifras =
        new Cifras(
            contar(porEstado, "EN_DEPOSITO"),
            contar(porEstado, "ASIGNACION_REALIZADA") + contar(porEstado, "LISTA_PARA_ENTREGAR"),
            contar(porEstado, "EN_TRASLADO") + contar(porEstado, "EN_PUERTA"),
            contar(porEstado, "ENTREGA_FALLIDA"),
            contar(porEstado, "VENCIDA"),
            entregadas == null ? 0 : numero(entregadas.cantidad()),
            delMes(dia),
            contar(porEstado, "ENTREGADA"),
            numero(resumen.entidadesActivas()),
            donantes == null ? 0 : numero(donantes.activos()),
            donantes == null ? 0 : numero(donantes.total()),
            camiones());
    String fecha = FECHA.format(hoy);
    return new AdminInicioVista(
        Character.toUpperCase(fecha.charAt(0)) + fecha.substring(1),
        resumen(decisiones),
        decisiones,
        calculado == null ? al : AL.format(calculado),
        cifras);
  }

  /** La flota en ruta y libre, o null si Logística no respondió. */
  private Camiones camiones() {
    try {
      List<LogisticaApi.Camion> flota = logistica.camiones(null);
      return new Camiones(
          flota.stream().filter(c -> "EN_RUTA".equals(c.estado())).count(),
          flota.stream().filter(c -> "DISPONIBLE".equals(c.estado())).count());
    } catch (RestClientException sinLogistica) {
      return null;
    }
  }

  /** "en octubre, del 1 al 15"; el primer día, "el 1 de noviembre". */
  private static String delMes(LocalDate dia) {
    return dia.getDayOfMonth() == 1
        ? "el " + Comun.DIA.format(dia)
        : "en " + MES.format(dia) + ", del 1 al " + dia.getDayOfMonth();
  }

  private static long contar(Map<String, Long> porEstado, String estado) {
    return numero(porEstado.get(estado));
  }

  private static long numero(Long valor) {
    return valor == null ? 0 : valor;
  }

  private static String miles(long valor) {
    return NumberFormat.getIntegerInstance(Comun.ES_AR).format(valor);
  }

  /** "La primera, Leche en polvo, vence el 13 de noviembre, en 2 días." */
  private static String primera(DonacionesApi.Proxima proxima, LocalDate hoy) {
    LocalDate fecha = proxima.fechaVencimiento();
    if (fecha == null) {
      return null;
    }
    long dias = ChronoUnit.DAYS.between(hoy, fecha);
    String cuando;
    if (dias <= 0) {
      cuando = "vence hoy";
    } else if (dias == 1) {
      cuando = "vence el " + Comun.DIA.format(fecha) + ", en 1 día";
    } else {
      cuando = "vence el " + Comun.DIA.format(fecha) + ", en " + miles(dias) + " días";
    }
    return proxima.descripcion() == null || proxima.descripcion().isBlank()
        ? "La primera " + cuando + "."
        : "La primera, " + proxima.descripcion() + ", " + cuando + ".";
  }

  /** La frase de la cabecera, con separador de miles, o null si no hay nada que decidir. */
  static String resumen(Decisiones d) {
    List<String> esperan = new ArrayList<>();
    if (d.entidades() > 0) {
      esperan.add(cuantas(d.entidades(), "organización", "organizaciones"));
    }
    if (d.propuestas() > 0) {
      esperan.add(cuantas(d.propuestas(), "propuesta de asignación", "propuestas de asignación"));
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
    if (d.sinAsignar() > 0) {
      frases.add(sinAsignar(d));
    }
    return frases.isEmpty() ? null : String.join(" ", frases);
  }

  /** "Hay 7 donaciones sin asignar en el depósito; 4 vencen en los próximos 7 días." */
  private static String sinAsignar(Decisiones d) {
    String hay =
        "Hay " + cuantas(d.sinAsignar(), "donación", "donaciones") + " sin asignar en el depósito";
    if (d.vencen() == 0) {
      return hay + ".";
    }
    if (d.sinAsignar() == 1) {
      return hay + "; vence en los próximos 7 días.";
    }
    return hay
        + "; "
        + miles(d.vencen())
        + (d.vencen() == 1 ? " vence" : " vencen")
        + " en los próximos 7 días.";
  }

  private static String cuantas(long cantidad, String una, String varias) {
    return miles(cantidad) + " " + (cantidad == 1 ? una : varias);
  }
}
