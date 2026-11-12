package grupo5.cliente.maqueta;

import grupo5.cliente.vistas.AdminInicioVista.Evolucion;
import grupo5.cliente.vistas.AdminInicioVista.Fila;
import grupo5.cliente.vistas.AdminInicioVista.Mes;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Los gráficos de ejemplo del inicio del admin (R11-42), con los datos de su ficha
 * (docs/interfaz/flujos/raices-entidad-navegable.md, R11-42, sección 6), coherentes con las cifras
 * de R11-21: jueves 15 de octubre de 2026. {@code ?escenario=evolucion-corta} da la plataforma
 * nueva (desde julio) y {@code ?escenario=evolucion-vacia}, todavía sin donaciones; el error lo
 * elige la plantilla ({@code error-evolucion}).
 */
final class AdminInicioGraficosDeEjemplo {

  private static final String NOTA =
      "Una entrega cuenta en el mes en que la organización la confirmó: puede ser de una donación"
          + " registrada antes. Octubre todavía no terminó.";
  private static final String TOTAL = "Total";

  private AdminInicioGraficosDeEjemplo() {}

  /** Los del escenario pedido; sin escenario, los 12 meses de la ficha. */
  static Evolucion evolucion() {
    String escenario = null;
    if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes pedido) {
      escenario = pedido.getRequest().getParameter("escenario");
    }
    if ("evolucion-vacia".equals(escenario)) {
      return Evolucion.VACIA;
    }
    return "evolucion-corta".equals(escenario) ? corta() : completa();
  }

  private static Evolucion completa() {
    String[] cortos = {
      "nov", "dic", "ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct"
    };
    String[] nombres = {
      "Noviembre 2025",
      "Diciembre 2025",
      "Enero 2026",
      "Febrero 2026",
      "Marzo 2026",
      "Abril 2026",
      "Mayo 2026",
      "Junio 2026",
      "Julio 2026",
      "Agosto 2026",
      "Septiembre 2026",
      "Octubre 2026, del 1 al 15"
    };
    long[] registradas = {96, 132, 88, 74, 110, 121, 138, 152, 147, 165, 143, 58};
    long[] entregadas = {71, 104, 90, 66, 85, 102, 117, 128, 131, 139, 119, 9};
    List<Mes> meses = new ArrayList<>();
    for (int i = 0; i < cortos.length; i++) {
      String anio =
          switch (i) {
            case 0 -> "2025";
            case 2 -> "2026";
            default -> null;
          };
      meses.add(
          mes(
              cortos[i],
              anio,
              nombres[i],
              i == cortos.length - 1,
              registradas[i],
              entregadas[i],
              200));
    }
    return new Evolucion(
        "de noviembre de 2025 a octubre de 2026",
        "En los últimos 12 meses se registraron 1.424 donaciones y se confirmaron 1.161 entregas."
            + " El mes con más donaciones registradas fue agosto de 2026, con 165.",
        NOTA,
        200,
        100,
        meses,
        new Fila(TOTAL, null, 1_424, 1_161, 0, 0),
        "Alimentos es la categoría con más donaciones registradas en estos 12 meses: 542 de"
            + " 1.424.",
        List.of(
            categoria("Alimentos", "cat-alimentos", 542, 461, 542),
            categoria("Ropa", "cat-ropa", 287, 241, 542),
            categoria("Higiene", "cat-higiene", 164, 139, 542),
            categoria("Útiles escolares", "cat-escolares", 131, 102, 542),
            categoria("Blanquería", "cat-blanqueria", 98, 77, 542),
            categoria("Juguetes", "cat-juguetes", 86, 64, 542),
            categoria("Muebles", "cat-muebles", 71, 48, 542),
            categoria("Electrodomésticos", "cat-electrodomesticos", 45, 29, 542)),
        new Fila(TOTAL, null, 1_424, 1_161, 0, 0));
  }

  /** La plataforma nueva: la primera donación es de julio de 2026. */
  private static Evolucion corta() {
    return new Evolucion(
        "de julio a octubre de 2026",
        "Desde julio de 2026, cuando se registró la primera donación, se registraron 312"
            + " donaciones y se confirmaron 198 entregas. El mes con más donaciones registradas fue"
            + " septiembre de 2026, con 102.",
        NOTA,
        200,
        100,
        List.of(
            mes("jul", "2026", "Julio 2026", false, 64, 30, 200),
            mes("ago", null, "Agosto 2026", false, 88, 52, 200),
            mes("sep", null, "Septiembre 2026", false, 102, 71, 200),
            mes("oct", null, "Octubre 2026, del 1 al 15", true, 58, 45, 200)),
        new Fila(TOTAL, null, 312, 198, 0, 0),
        "Alimentos es la categoría con más donaciones registradas desde julio de 2026: 120 de"
            + " 312.",
        List.of(
            categoria("Alimentos", "cat-alimentos", 120, 80, 120),
            categoria("Ropa", "cat-ropa", 64, 40, 120),
            categoria("Higiene", "cat-higiene", 40, 26, 120),
            categoria("Útiles escolares", "cat-escolares", 30, 20, 120),
            categoria("Blanquería", "cat-blanqueria", 22, 14, 120),
            categoria("Juguetes", "cat-juguetes", 18, 10, 120),
            categoria("Muebles", "cat-muebles", 12, 6, 120),
            categoria("Electrodomésticos", "cat-electrodomesticos", 6, 2, 120)),
        new Fila(TOTAL, null, 312, 198, 0, 0));
  }

  private static Mes mes(
      String corto,
      String anio,
      String nombre,
      boolean enCurso,
      long registradas,
      long entregadas,
      long tope) {
    return new Mes(
        corto,
        anio,
        nombre,
        enCurso,
        registradas,
        entregadas,
        centesimos(registradas, tope),
        centesimos(entregadas, tope));
  }

  private static Fila categoria(
      String nombre, String icono, long registradas, long entregadas, long mayor) {
    return new Fila(
        nombre,
        icono,
        registradas,
        entregadas,
        centesimos(registradas, mayor),
        centesimos(entregadas, mayor));
  }

  private static int centesimos(long valor, long total) {
    return (int) Math.round(valor * 100.0 / total);
  }
}
