package grupo5.cliente.rest;

import grupo5.cliente.vistas.AdminInicioVista;
import grupo5.cliente.vistas.AdminInicioVista.Camiones;
import grupo5.cliente.vistas.AdminInicioVista.Cifras;
import grupo5.cliente.vistas.AdminInicioVista.Decisiones;
import grupo5.cliente.vistas.AdminInicioVista.Evolucion;
import grupo5.cliente.vistas.AdminInicioVista.Fila;
import grupo5.cliente.vistas.AdminInicioVista.Mes;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.ToLongFunction;
import org.springframework.web.client.RestClientException;

/**
 * El inicio del admin por temas (R11-21) con lo que dan Donaciones y Logística: las solicitudes
 * (con el nombre que ya traen, como mucho 3), y las cifras y lo que espera, que son números de
 * {@code GET /api/donaciones/resumen}: ninguna lista se trae para contarla, tampoco las propuestas
 * (DTI-40, ítem 1). Los camiones se cuentan sobre la flota hasta que Logística dé el número; si no
 * responde, solo esa tarjeta queda sin datos. Los gráficos (R11-42) salen de {@code GET
 * /api/donaciones/resumen/evolucion}, ya contados: acá solo se calculan las alturas, la escala y
 * los textos; si no llegan, solo su tarjeta muestra el aviso.
 */
final class AdminPersonasInicio {

  private static final int NOMBRES = 3;
  private static final DateTimeFormatter FECHA =
      DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", Comun.ES_AR);
  private static final DateTimeFormatter MES = DateTimeFormatter.ofPattern("MMMM", Comun.ES_AR);
  private static final DateTimeFormatter AL =
      DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy, HH:mm", Comun.ES_AR);
  private static final DateTimeFormatter MES_DE_ANIO =
      DateTimeFormatter.ofPattern("MMMM 'de' yyyy", Comun.ES_AR);
  private static final DateTimeFormatter MES_Y_ANIO =
      DateTimeFormatter.ofPattern("MMMM yyyy", Comun.ES_AR);
  private static final int CIEN = 100;
  private static final int MESES = 12;
  private static final String TOTAL = "Total";

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
    return new AdminInicioVista(
        mayuscula(FECHA.format(hoy)),
        resumen(decisiones),
        decisiones,
        calculado == null ? al : AL.format(calculado),
        cifras,
        evolucion(hoy));
  }

  /** Los gráficos, o null si Donaciones no los dio: solo esa tarjeta muestra el aviso. */
  private Evolucion evolucion(LocalDate hoy) {
    DonacionesApi.Evolucion evolucion;
    try {
      evolucion = donaciones.evolucion();
    } catch (RestClientException sinSeries) {
      return null;
    }
    return evolucion == null ? null : evolucion(evolucion, hoy);
  }

  /**
   * Los dos gráficos con sus textos. {@code hoy} es el día de lectura si la evolución no trae el de
   * su cálculo; sirve para decir hasta qué día va el mes en curso.
   */
  static Evolucion evolucion(DonacionesApi.Evolucion e, LocalDate hoy) {
    List<DonacionesApi.MesEvolucion> meses = e.meses() == null ? List.of() : e.meses();
    if (e.desde() == null || meses.isEmpty()) {
      return Evolucion.VACIA;
    }
    LocalDate dia =
        e.calculadoEn() == null
            ? hoy
            : e.calculadoEn().atZoneSameInstant(Comun.ARGENTINA).toLocalDate();
    YearMonth desde = YearMonth.parse(meses.get(0).mes());
    YearMonth ultimo = YearMonth.parse(meses.get(meses.size() - 1).mes());
    boolean enCurso = ultimo.equals(YearMonth.from(dia));
    long registradas = suma(meses, m -> numero(m.registradas()));
    long entregadas = suma(meses, m -> numero(m.entregadas()));
    long tope =
        tope(
            Math.max(
                maximo(meses, m -> numero(m.registradas())),
                maximo(meses, m -> numero(m.entregadas()))));
    List<Mes> columnas = new ArrayList<>();
    for (int i = 0; i < meses.size(); i++) {
      columnas.add(columna(meses.get(i), i == 0, enCurso && i == meses.size() - 1, dia, tope));
    }
    String desdeCuando = MES_DE_ANIO.format(desde);
    boolean doce = meses.size() == MESES;
    String frase =
        (doce
                ? "En los últimos 12 meses "
                : "Desde " + desdeCuando + ", cuando se registró la primera donación, ")
            + registraron(registradas)
            + " y "
            + confirmaron(entregadas)
            + "."
            + masRegistradas(meses);
    String nota =
        "Una entrega cuenta en el mes en que la organización la confirmó: puede ser de una donación"
            + " registrada antes."
            + (enCurso ? " " + mayuscula(MES.format(ultimo)) + " todavía no terminó." : "");
    List<Fila> categorias = categorias(e);
    Fila totalCategorias =
        new Fila(
            TOTAL,
            null,
            suma(categorias, Fila::registradas),
            suma(categorias, Fila::entregadas),
            0,
            0);
    return new Evolucion(
        periodo(desde, ultimo),
        frase,
        nota,
        tope,
        tope / 2,
        columnas,
        new Fila(TOTAL, null, registradas, entregadas, 0, 0),
        fraseCategorias(e, totalCategorias, doce ? "en estos 12 meses" : "desde " + desdeCuando),
        categorias,
        totalCategorias);
  }

  /** Un mes del gráfico: el año va debajo del primero y de enero. */
  private static Mes columna(
      DonacionesApi.MesEvolucion m, boolean primero, boolean enCurso, LocalDate dia, long tope) {
    YearMonth mes = YearMonth.parse(m.mes());
    String nombre = mayuscula(MES_Y_ANIO.format(mes));
    if (enCurso) {
      nombre += dia.getDayOfMonth() == 1 ? ", solo el 1" : ", del 1 al " + dia.getDayOfMonth();
    }
    return new Mes(
        MES.format(mes).substring(0, 3),
        primero || mes.getMonth() == Month.JANUARY ? String.valueOf(mes.getYear()) : null,
        nombre,
        enCurso,
        numero(m.registradas()),
        numero(m.entregadas()),
        proporcion(numero(m.registradas()), tope),
        proporcion(numero(m.entregadas()), tope));
  }

  /**
   * " El mes con más donaciones registradas fue agosto de 2026, con 165."; si empatan, el primero.
   * Nada si todos dan 0.
   */
  private static String masRegistradas(List<DonacionesApi.MesEvolucion> meses) {
    DonacionesApi.MesEvolucion mayor = meses.get(0);
    for (DonacionesApi.MesEvolucion m : meses) {
      if (numero(m.registradas()) > numero(mayor.registradas())) {
        mayor = m;
      }
    }
    long cuantas = numero(mayor.registradas());
    if (cuantas == 0) {
      return "";
    }
    return " El mes con más donaciones registradas fue "
        + MES_DE_ANIO.format(YearMonth.parse(mayor.mes()))
        + ", con "
        + miles(cuantas)
        + ".";
  }

  /** Las del catálogo, como llegan (de más a menos), y "Otras 3 categorías" con la suma. */
  private static List<Fila> categorias(DonacionesApi.Evolucion e) {
    List<DonacionesApi.CategoriaEvolucion> deEvolucion =
        e.categorias() == null ? List.of() : e.categorias();
    DonacionesApi.OtrasCategorias otras = e.otras();
    long mayor =
        Math.max(
            maximo(deEvolucion, c -> numero(c.registradas())),
            maximo(deEvolucion, c -> numero(c.entregadas())));
    if (otras != null) {
      mayor = Math.max(mayor, Math.max(numero(otras.registradas()), numero(otras.entregadas())));
    }
    List<Fila> filas = new ArrayList<>();
    for (DonacionesApi.CategoriaEvolucion c : deEvolucion) {
      filas.add(fila(c.nombre(), Comun.icono(c.nombre()), c.registradas(), c.entregadas(), mayor));
    }
    if (otras != null) {
      int cuantas = otras.categorias() == null ? 0 : otras.categorias();
      filas.add(
          fila(
              cuantas == 1 ? "Otra categoría" : "Otras " + cuantas + " categorías",
              Comun.ICONO_GENERICO,
              otras.registradas(),
              otras.entregadas(),
              mayor));
    }
    return filas;
  }

  private static Fila fila(
      String nombre, String icono, Long registradas, Long entregadas, long mayor) {
    return new Fila(
        nombre,
        icono,
        numero(registradas),
        numero(entregadas),
        proporcion(numero(registradas), mayor),
        proporcion(numero(entregadas), mayor));
  }

  /**
   * "Alimentos es la categoría con más donaciones registradas en estos 12 meses: 542 de 1.424."; si
   * empatan, "Alimentos y Ropa son las categorías…: 287 cada una, de 1.424.".
   */
  private static String fraseCategorias(DonacionesApi.Evolucion e, Fila total, String cuando) {
    List<DonacionesApi.CategoriaEvolucion> categorias =
        e.categorias() == null ? List.of() : e.categorias();
    long mayor = maximo(categorias, c -> numero(c.registradas()));
    if (mayor == 0) {
      return mayuscula(cuando) + " no se registraron donaciones con categoría.";
    }
    List<String> primeras =
        categorias.stream()
            .filter(c -> numero(c.registradas()) == mayor)
            .map(DonacionesApi.CategoriaEvolucion::nombre)
            .toList();
    String cifra = ": " + miles(mayor);
    String de = " de " + miles(total.registradas()) + ".";
    if (primeras.size() == 1) {
      return primeras.get(0)
          + " es la categoría con más donaciones registradas "
          + cuando
          + cifra
          + de;
    }
    return enumerar(primeras)
        + " son las categorías con más donaciones registradas "
        + cuando
        + cifra
        + " cada una,"
        + de;
  }

  /** "A y B", "A, B y C". */
  private static String enumerar(List<String> nombres) {
    int ultimo = nombres.size() - 1;
    return String.join(", ", nombres.subList(0, ultimo)) + " y " + nombres.get(ultimo);
  }

  /** "de noviembre de 2025 a octubre de 2026", "de julio a octubre de 2026" u "octubre de 2026". */
  static String periodo(YearMonth desde, YearMonth hasta) {
    if (desde.equals(hasta)) {
      return MES_DE_ANIO.format(hasta);
    }
    String inicio =
        desde.getYear() == hasta.getYear() ? MES.format(desde) : MES_DE_ANIO.format(desde);
    return "de " + inicio + " a " + MES_DE_ANIO.format(hasta);
  }

  /**
   * El tope de la escala: el número fácil (10, 20, 50, 100, 200…) igual o mayor que la cifra más
   * alta, para que la línea de la mitad también sea redonda.
   */
  static long tope(long mayor) {
    long paso = 10;
    while (paso * 5 < mayor) {
      paso *= 10;
    }
    if (paso >= mayor) {
      return paso;
    }
    return paso * 2 >= mayor ? paso * 2 : paso * 5;
  }

  /** El alto o el largo en centésimos de {@code total}; 0 si no hay total. */
  private static int proporcion(long valor, long total) {
    return total == 0 ? 0 : (int) Math.round(valor * (double) CIEN / total);
  }

  private static <T> long suma(List<T> filas, ToLongFunction<T> cifra) {
    return filas.stream().mapToLong(cifra).sum();
  }

  private static <T> long maximo(List<T> filas, ToLongFunction<T> cifra) {
    return filas.stream().mapToLong(cifra).max().orElse(0);
  }

  private static String registraron(long cuantas) {
    return cuantas == 1
        ? "se registró 1 donación"
        : "se registraron " + miles(cuantas) + " donaciones";
  }

  private static String confirmaron(long cuantas) {
    return cuantas == 1
        ? "se confirmó 1 entrega"
        : "se confirmaron " + miles(cuantas) + " entregas";
  }

  private static String mayuscula(String texto) {
    return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
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
