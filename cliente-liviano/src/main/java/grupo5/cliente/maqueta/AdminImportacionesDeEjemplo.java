package grupo5.cliente.maqueta;

import grupo5.cliente.puertos.DescargaDeFilas;
import grupo5.cliente.vistas.AdminDonantesVista.FilaConError;
import grupo5.cliente.vistas.AdminImportacionesVista;
import grupo5.cliente.vistas.AdminImportacionesVista.Fila;
import grupo5.cliente.vistas.ConsultaImportaciones;
import grupo5.cliente.vistas.Tramo;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * Las importaciones de ejemplo de la ficha r11-p1-importaciones (sección 4.1): una de cada estado y
 * varias viejas terminadas, 23 en total. La búsqueda, los filtros y el orden se aplican acá, como
 * los resuelve Donaciones. Escenarios: {@code vacia} (nunca se importó nada), {@code
 * sin-resultados} (una búsqueda que no coincide) y {@code con-viejas} (al final, dos de antes de
 * que se guardaran el nombre y la fecha).
 */
final class AdminImportacionesDeEjemplo {

  private static final String AL = "12 de noviembre de 2026, 18:34";
  private static final String AVANCE = "/admin-donantes-importando";
  private static final String RESULTADO = "/admin-donantes-importado";
  private static final String VER_EL_RESULTADO = "Ver el resultado";

  /** Una importación de ejemplo y su día de subida (null: sin fecha guardada). */
  private record Ejemplo(LocalDate dia, Fila fila) {}

  private static final List<Ejemplo> EJEMPLOS = ejemplos();

  private static final List<Ejemplo> VIEJAS =
      List.of(
          new Ejemplo(
              null,
              new Fila(
                  null,
                  null,
                  null,
                  "terminada",
                  "Terminada",
                  "1.200 filas: 1.150 nuevos y 50 actualizados",
                  null,
                  VER_EL_RESULTADO,
                  RESULTADO + "?escenario=sin-errores")),
          new Ejemplo(
              null,
              new Fila(
                  null,
                  null,
                  null,
                  "error",
                  "No se importó ninguna fila",
                  null,
                  "El archivo no se pudo leer.",
                  null,
                  null)));

  private AdminImportacionesDeEjemplo() {}

  static AdminImportacionesVista importaciones(ConsultaImportaciones pedida, String escenario) {
    String cual = escenario == null ? "" : escenario;
    ConsultaImportaciones consulta =
        "sin-resultados".equals(cual) && !pedida.filtrada()
            ? new ConsultaImportaciones(
                "padron-2019",
                pedida.estado(),
                pedida.subida(),
                pedida.desde(),
                pedida.hasta(),
                pedida.orden(),
                pedida.pagina(),
                pedida.avisos())
            : pedida;
    List<Ejemplo> todas = new ArrayList<>();
    if (!"vacia".equals(cual)) {
      todas.addAll(EJEMPLOS);
    }
    if ("con-viejas".equals(cual)) {
      todas.addAll(VIEJAS);
    }
    Comparator<Ejemplo> porDia =
        Comparator.comparing(
            Ejemplo::dia,
            Comparator.nullsLast(
                "antiguas".equals(consulta.orden())
                    ? Comparator.<LocalDate>naturalOrder()
                    : Comparator.<LocalDate>reverseOrder()));
    List<Fila> filas =
        todas.stream()
            .filter(e -> coincide(e, consulta))
            .sorted(porDia)
            .map(Ejemplo::fila)
            .toList();
    long enCurso =
        todas.stream()
            .filter(e -> Set.of("esperando", "importando").contains(e.fila().etapa()))
            .count();
    return AdminImportacionesVista.de(
        enCurso, AL, Tramo.de(filas, consulta.pagina(), 20), consulta);
  }

  private static boolean coincide(Ejemplo ejemplo, ConsultaImportaciones consulta) {
    Fila fila = ejemplo.fila();
    if (!consulta.buscar().isEmpty()
        && (fila.archivo() == null
            || !sinTildes(fila.archivo()).contains(sinTildes(consulta.buscar())))) {
      return false;
    }
    boolean conFecha = consulta.desde() != null || consulta.hasta() != null;
    if (conFecha
        && (ejemplo.dia() == null
            || (consulta.desde() != null && ejemplo.dia().isBefore(consulta.desde()))
            || (consulta.hasta() != null && ejemplo.dia().isAfter(consulta.hasta())))) {
      return false;
    }
    return switch (consulta.estado()) {
      case "en-curso" -> Set.of("esperando", "importando").contains(fila.etapa());
      case "terminadas" -> Set.of("terminada", "con-errores").contains(fila.etapa());
      case "con-errores" -> "con-errores".equals(fila.etapa());
      case "cortadas" -> Set.of("cortada", "error").contains(fila.etapa());
      default -> true;
    };
  }

  private static String sinTildes(String texto) {
    return Normalizer.normalize(texto, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT);
  }

  /** Las filas con error de ejemplo, en CSV, con el mismo texto que la tabla. */
  static DescargaDeFilas filasConError(List<FilaConError> filas) {
    return new DescargaDeFilas(
        "donantes-octubre.csv",
        salida -> {
          salida.write('﻿');
          salida.write("Fila;Qué pasó;Código\r\n");
          for (FilaConError fila : filas) {
            salida.write(
                fila.fila().replace("Fila ", "").replace(".", "") + ";" + fila.que() + ";\r\n");
          }
        });
  }

  private static List<Ejemplo> ejemplos() {
    List<Ejemplo> ejemplos = new ArrayList<>();
    ejemplos.add(
        new Ejemplo(
            LocalDate.of(2026, 11, 12),
            new Fila(
                "donantes-noviembre.csv",
                "12 nov 2026, 18:20",
                "2026-11-12T18:20:00",
                "importando",
                "Importando",
                "8.400 de 20.000",
                null,
                "Ver el avance",
                AVANCE)));
    ejemplos.add(
        new Ejemplo(
            LocalDate.of(2026, 10, 15),
            new Fila(
                "donantes-octubre.csv",
                "15 oct 2026, 18:02",
                "2026-10-15T18:02:00",
                "con-errores",
                "Terminada con filas con error",
                "20.000 filas: 19.100 nuevos, 774 actualizados y 126 con error",
                null,
                VER_EL_RESULTADO,
                RESULTADO)));
    ejemplos.add(
        new Ejemplo(
            LocalDate.of(2026, 9, 30),
            new Fila(
                "padrón-septiembre.csv",
                "30 sep 2026, 09:12",
                "2026-09-30T09:12:00",
                "terminada",
                "Terminada",
                "3.200 filas: 3.010 nuevos y 190 actualizados",
                null,
                VER_EL_RESULTADO,
                RESULTADO + "?escenario=sin-errores")));
    ejemplos.add(
        new Ejemplo(
            LocalDate.of(2026, 9, 29),
            new Fila(
                "prueba.csv",
                "29 sep 2026, 17:40",
                "2026-09-29T17:40:00",
                "error",
                "No se importó ninguna fila",
                null,
                "El archivo no se pudo leer.",
                null,
                null)));
    ejemplos.add(
        new Ejemplo(
            LocalDate.of(2026, 8, 31),
            new Fila(
                "padron-agosto.csv",
                "31 ago 2026, 11:05",
                "2026-08-31T11:05:00",
                "cortada",
                "Se cortó en la fila 8.400",
                "8.400 filas leídas: 7.950 nuevos, 410 actualizados y 40 con error",
                null,
                VER_EL_RESULTADO,
                RESULTADO + "?escenario=cortada")));
    ejemplos.add(
        new Ejemplo(
            LocalDate.of(2026, 8, 20),
            new Fila(
                "donantes-escuela.csv",
                "20 ago 2026, 10:00",
                "2026-08-20T10:00:00",
                "esperando",
                "Esperando para empezar",
                null,
                "Todavía no empezó a leer filas.",
                "Ver el avance",
                AVANCE + "?escenario=esperando")));
    IntStream.range(0, 17)
        .forEach(
            i -> {
              LocalDate dia = LocalDate.of(2026, 7, 28).minusDays(7L * i);
              String fecha = dia.getDayOfMonth() + " " + mes(dia) + " 2026, 10:30";
              ejemplos.add(
                  new Ejemplo(
                      dia,
                      new Fila(
                          "padron-semanal-" + (17 - i) + ".csv",
                          fecha,
                          dia + "T10:30:00",
                          "terminada",
                          "Terminada",
                          "450 filas: 420 nuevos y 30 actualizados",
                          null,
                          VER_EL_RESULTADO,
                          RESULTADO + "?escenario=sin-errores")));
            });
    return List.copyOf(ejemplos);
  }

  private static String mes(LocalDate dia) {
    return List.of(
            "ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")
        .get(dia.getMonthValue() - 1);
  }
}
