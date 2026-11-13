package grupo5.cliente.rest;

import grupo5.cliente.puertos.DescargaDeFilas;
import grupo5.cliente.rest.DonacionesApi.Archivo;
import grupo5.cliente.vistas.AdminImportacionesVista;
import grupo5.cliente.vistas.AdminImportacionesVista.Fila;
import grupo5.cliente.vistas.ConsultaImportaciones;
import grupo5.cliente.vistas.Tramo;
import java.io.IOException;
import java.io.Writer;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;

/**
 * Las importaciones de donantes del admin desde Donaciones (R11-20): la lista de a una página, con
 * la búsqueda y los filtros resueltos allá, la cuenta de las que siguen en curso (un número, no una
 * lista) y la descarga de las filas con error de a tandas. Lo usa {@link AdminPersonasRest}.
 */
final class AdminPersonasImportaciones {

  /** De a 20 importaciones por página. */
  static final int POR_PAGINA = 20;

  /** De a cuántas filas con error se piden para la descarga (el máximo que acepta Donaciones). */
  static final int TANDA = 100;

  static final String ENCABEZADO = "Fila;Qué pasó;Código";
  static final String DESCARGA_CORTADA =
      "No pudimos terminar la descarga: faltan filas. Volvé a descargarla o miralas en la tabla.";

  private static final String FIN = "\r\n";
  private static final String[] MESES = {
    "ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic"
  };
  private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
  private static final String ESTADO = "estado";
  private static final String PENDIENTE = "PENDIENTE";
  private static final String PROCESANDO = "PROCESANDO";
  private static final String VER_EL_AVANCE = "Ver el avance";
  private static final String VER_EL_RESULTADO = "Ver el resultado";

  private final DonacionesApi donaciones;

  AdminPersonasImportaciones(DonacionesApi donaciones) {
    this.donaciones = donaciones;
  }

  /** La página pedida y la cuenta de las que siguen en curso: dos pedidos a Donaciones. */
  AdminImportacionesVista importaciones(ConsultaImportaciones consulta, String al) {
    Tramo<Archivo> pagina =
        Paginas.pedir(
            consulta.pagina(),
            POR_PAGINA,
            numero -> donaciones.archivos(filtros(consulta, numero)));
    Tramo<Fila> filas =
        new Tramo<>(
            pagina.filas().stream().map(AdminPersonasImportaciones::fila).toList(),
            pagina.total(),
            pagina.pagina(),
            pagina.paginas());
    return AdminImportacionesVista.de(enCurso(), al, filas, consulta);
  }

  /** Cuántas siguen en curso: el total de una página de una, sin traer la lista. */
  private long enCurso() {
    MultiValueMap<String, String> filtros = new LinkedMultiValueMap<>();
    filtros.add(ESTADO, PENDIENTE);
    filtros.add(ESTADO, PROCESANDO);
    filtros.add("size", "1");
    ResponseEntity<List<Archivo>> respuesta = donaciones.archivos(filtros);
    return Paginas.total(respuesta, List.of(), 1, 1);
  }

  private static MultiValueMap<String, String> filtros(ConsultaImportaciones consulta, int pagina) {
    MultiValueMap<String, String> filtros = new LinkedMultiValueMap<>();
    if (!consulta.buscar().isEmpty()) {
      filtros.add("q", consulta.buscar());
    }
    switch (consulta.estado()) {
      case "en-curso" -> {
        filtros.add(ESTADO, PENDIENTE);
        filtros.add(ESTADO, PROCESANDO);
      }
      case "terminadas" -> {
        filtros.add(ESTADO, "PROCESADO");
        filtros.add(ESTADO, "PROCESADO_CON_ERRORES");
      }
      case "con-errores" -> filtros.add(ESTADO, "PROCESADO_CON_ERRORES");
      case "cortadas" -> filtros.add(ESTADO, "ERROR");
      default -> {
        // Todos los estados.
      }
    }
    if (consulta.desde() != null) {
      filtros.add("desde", consulta.desde().toString());
    }
    if (consulta.hasta() != null) {
      filtros.add("hasta", consulta.hasta().toString());
    }
    filtros.add("orden", consulta.orden().isEmpty() ? "recientes" : consulta.orden());
    filtros.add("page", String.valueOf(pagina));
    filtros.add("size", String.valueOf(POR_PAGINA));
    return filtros;
  }

  /** Un renglón, con el mismo vocabulario que el avance y el resultado. */
  static Fila fila(Archivo archivo) {
    int procesadas = entero(archivo.procesadas());
    int conError = entero(archivo.conError());
    String avance = "/admin-donantes-importando?archivo=" + archivo.id();
    String resultado = "/admin-donantes-importado?archivo=" + archivo.id();
    String desglose = desglose(archivo.creadas(), archivo.actualizadas(), conError);
    String[] partes =
        switch (archivo.estado() == null ? "" : archivo.estado()) {
          case PENDIENTE ->
              new String[] {
                "esperando",
                "Esperando para empezar",
                null,
                "Todavía no empezó a leer filas.",
                VER_EL_AVANCE,
                avance
              };
          case "PROCESADO" ->
              new String[] {
                "terminada",
                "Terminada",
                numero(procesadas) + " filas: " + desglose,
                null,
                VER_EL_RESULTADO,
                resultado
              };
          case "PROCESADO_CON_ERRORES" ->
              new String[] {
                "con-errores",
                "Terminada con filas con error",
                numero(procesadas) + " filas: " + desglose,
                null,
                VER_EL_RESULTADO,
                resultado
              };
          case "ERROR" -> cortadaOError(procesadas, desglose, resultado);
          default ->
              new String[] {
                "importando",
                "Importando",
                archivo.totalFilas() == null
                    ? numero(procesadas) + " filas leídas hasta ahora"
                    : numero(procesadas) + " de " + numero(archivo.totalFilas()),
                null,
                VER_EL_AVANCE,
                avance
              };
        };
    LocalDateTime subido = archivo.subidoEn();
    return new Fila(
        nombre(archivo.nombre()),
        subido == null ? null : corta(subido),
        subido == null ? null : ISO.format(subido),
        partes[0],
        partes[1],
        partes[2],
        partes[3],
        partes[4],
        partes[5]);
  }

  /** Un {@code ERROR} con filas leídas se cortó a mitad de camino; sin ninguna, no se leyó. */
  private static String[] cortadaOError(int procesadas, String desglose, String resultado) {
    if (procesadas > 0) {
      return new String[] {
        "cortada",
        "Se cortó en la fila " + numero(procesadas),
        numero(procesadas) + " filas leídas: " + desglose,
        null,
        VER_EL_RESULTADO,
        resultado
      };
    }
    return new String[] {
      "error", "No se importó ninguna fila", null, "El archivo no se pudo leer.", null, null
    };
  }

  /** "3.010 nuevos y 190 actualizados" o "19.100 nuevos, 774 actualizados y 126 con error". */
  private static String desglose(Integer creadas, Integer actualizadas, int conError) {
    return conError == 0
        ? numero(creadas) + " nuevos y " + numero(actualizadas) + " actualizados"
        : numero(creadas)
            + " nuevos, "
            + numero(actualizadas)
            + " actualizados y "
            + numero(conError)
            + " con error";
  }

  /** "15 oct 2026, 18:02". */
  static String corta(LocalDateTime cuando) {
    return cuando.getDayOfMonth()
        + " "
        + MESES[cuando.getMonthValue() - 1]
        + " "
        + cuando.getYear()
        + ", "
        + String.format("%02d:%02d", cuando.getHour(), cuando.getMinute());
  }

  /**
   * La descarga: el encabezado y las filas de a {@link #TANDA}, cada tanda escrita apenas llega. Si
   * un pedido falla a mitad de camino, la última línea lo dice y el archivo termina ahí.
   */
  DescargaDeFilas filasConError(UUID id) {
    Archivo archivo = donaciones.archivo(id);
    int conError = entero(archivo.conError());
    return new DescargaDeFilas(nombre(archivo.nombre()), salida -> escribir(id, conError, salida));
  }

  private void escribir(UUID id, int conError, Writer salida) throws IOException {
    salida.write('﻿');
    salida.write(ENCABEZADO + FIN);
    int tandas = (conError + TANDA - 1) / TANDA;
    for (int tanda = 0; tanda < tandas; tanda++) {
      List<DonacionesApi.FilaConError> filas;
      try {
        filas = donaciones.filasConError(id, tanda, TANDA);
      } catch (RestClientException noRespondio) {
        salida.write(celda(DESCARGA_CORTADA) + FIN);
        salida.flush();
        return;
      }
      for (DonacionesApi.FilaConError fila : filas) {
        salida.write(
            (fila.linea() == null ? "" : String.valueOf(fila.linea()))
                + ";"
                + celda(AdminPersonasRest.queFallo(fila.motivo()))
                + ";"
                + celda(fila.codigo() == null ? "" : fila.codigo())
                + FIN);
      }
      salida.flush();
      if (filas.size() < TANDA) {
        return;
      }
    }
  }

  /** Entre comillas si trae el separador, comillas o un salto de línea. */
  static String celda(String texto) {
    if (texto.contains(";") || texto.contains("\"") || texto.contains("\n")) {
      return "\"" + texto.replace("\"", "\"\"") + "\"";
    }
    return texto;
  }

  private static String nombre(String nombre) {
    return nombre == null || nombre.isBlank() ? null : nombre;
  }

  private static String numero(Integer valor) {
    return NumberFormat.getIntegerInstance(Comun.ES_AR).format(valor == null ? 0 : valor);
  }

  private static int entero(Integer valor) {
    return valor == null ? 0 : valor;
  }
}
