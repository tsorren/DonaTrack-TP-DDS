package grupo5.cliente.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import grupo5.cliente.cuentas.SolicitudesDeEntidad;
import grupo5.cliente.puertos.DescargaDeFilas;
import grupo5.cliente.vistas.AdminDonantesVista.Importacion;
import grupo5.cliente.vistas.AdminImportacionesVista;
import grupo5.cliente.vistas.AdminImportacionesVista.Fila;
import grupo5.cliente.vistas.ConsultaImportaciones;
import java.io.IOException;
import java.io.StringWriter;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** La lista de importaciones, el nombre del avance y la descarga, contra Donaciones (R11-20). */
class AdminPersonasImportacionesTest {

  private static final String DONACIONES = "http://donaciones";
  private static final String ARCHIVOS = DONACIONES + "/api/donantes/archivos";
  private static final UUID ARCHIVO = UUID.fromString("00000000-0000-4000-8000-0000000000aa");
  private static final MediaType JSON = MediaType.APPLICATION_JSON;

  private static final String CUATRO =
      """
      [
        {"id": "00000000-0000-4000-8000-0000000000a1", "path": "x.csv", "estado": "PROCESANDO",
         "totalFilas": 20000, "procesadas": 8400, "creadas": 7950, "actualizadas": 410,
         "conError": 40, "ultimos": [], "nombre": "donantes-noviembre.csv",
         "subidoEn": "2026-11-12T18:20:00"},
        {"id": "00000000-0000-4000-8000-0000000000a2", "path": "y.csv",
         "estado": "PROCESADO_CON_ERRORES", "totalFilas": 20000, "procesadas": 20000,
         "creadas": 19100, "actualizadas": 774, "conError": 126, "ultimos": [],
         "nombre": "donantes-octubre.csv", "subidoEn": "2026-10-15T18:02:00"},
        {"id": "00000000-0000-4000-8000-0000000000a3", "path": "z.csv", "estado": "ERROR",
         "totalFilas": null, "procesadas": 0, "creadas": 0, "actualizadas": 0, "conError": 0,
         "ultimos": [], "nombre": "prueba.csv", "subidoEn": "2026-09-29T17:40:00"},
        {"id": "00000000-0000-4000-8000-0000000000a4", "path": "w.csv", "estado": "PROCESADO",
         "totalFilas": 1200, "procesadas": 1200, "creadas": 1150, "actualizadas": 50,
         "conError": 0, "ultimos": []}
      ]
      """;

  private MockRestServiceServer donaciones;
  private AdminPersonasRest adaptador;

  @BeforeEach
  void armar() {
    RestClient.Builder builder = RestClient.builder();
    donaciones = MockRestServiceServer.bindTo(builder).build();
    Clock reloj = Clock.fixed(Instant.parse("2026-11-10T21:30:00Z"), ZoneOffset.UTC);
    adaptador =
        new AdminPersonasRest(
            ClientesHttp.cliente(builder, DONACIONES, DonacionesApi.class),
            ClientesHttp.cliente(RestClient.builder(), "http://logistica", LogisticaApi.class),
            reloj,
            new StaticListableBeanFactory().getBeanProvider(SolicitudesDeEntidad.class));
  }

  @Test
  void laListaHaceDosPedidosDeAUnaPaginaYDiceCadaEstadoEnPalabras_C10_C11() {
    donaciones
        .expect(once(), requestTo(ARCHIVOS + "?orden=recientes&page=0&size=20"))
        .andRespond(withSuccess(CUATRO, JSON).header("X-Total-Count", "23"));
    donaciones
        .expect(once(), requestTo(ARCHIVOS + "?estado=PENDIENTE&estado=PROCESANDO&size=1"))
        .andRespond(withSuccess("[]", JSON).header("X-Total-Count", "1"));

    AdminImportacionesVista vista =
        adaptador.importaciones(ConsultaImportaciones.primera(), "vacia");

    donaciones.verify();
    assertEquals(1, vista.enCurso());
    assertEquals(23, vista.importaciones().total());
    assertEquals(2, vista.importaciones().paginas());
    assertEquals("10 de noviembre de 2026, 18:30", vista.al());
    List<Fila> filas = vista.importaciones().filas();
    assertEquals(
        new Fila(
            "donantes-noviembre.csv",
            "12 nov 2026, 18:20",
            "2026-11-12T18:20:00",
            "importando",
            "Importando",
            "8.400 de 20.000",
            null,
            "Ver el avance",
            "/admin-donantes-importando?archivo=00000000-0000-4000-8000-0000000000a1"),
        filas.get(0));
    assertEquals("Terminada con filas con error", filas.get(1).estado());
    assertEquals(
        "20.000 filas: 19.100 nuevos, 774 actualizados y 126 con error", filas.get(1).filas());
    assertEquals(
        "/admin-donantes-importado?archivo=00000000-0000-4000-8000-0000000000a2",
        filas.get(1).enlace());
    assertEquals("No se importó ninguna fila", filas.get(2).estado());
    assertEquals("El archivo no se pudo leer.", filas.get(2).detalle());
    assertNull(filas.get(2).enlace());
    assertNull(filas.get(3).archivo());
    assertNull(filas.get(3).subido());
    assertEquals("1.200 filas: 1.150 nuevos y 50 actualizados", filas.get(3).filas());
  }

  @Test
  void losFiltrosDeLaPantallaSeResuelvenEnDonaciones_C3_C12() {
    ConsultaImportaciones consulta =
        ConsultaImportaciones.de(
            "octubre",
            "terminadas",
            "mes-anterior",
            null,
            null,
            "antiguas",
            2,
            LocalDate.of(2026, 11, 10));
    donaciones
        .expect(
            requestTo(
                ARCHIVOS
                    + "?q=octubre&estado=PROCESADO&estado=PROCESADO_CON_ERRORES&desde=2026-10-01"
                    + "&hasta=2026-10-31&orden=antiguas&page=1&size=20"))
        .andRespond(withSuccess(CUATRO, JSON).header("X-Total-Count", "24"));
    donaciones
        .expect(requestTo(ARCHIVOS + "?estado=PENDIENTE&estado=PROCESANDO&size=1"))
        .andRespond(withSuccess("[]", JSON).header("X-Total-Count", "0"));

    AdminImportacionesVista vista = adaptador.importaciones(consulta, null);

    donaciones.verify();
    assertEquals(2, vista.importaciones().pagina());
  }

  @Test
  void elAvanceTomaElNombreYLaFechaDeDonacionesSinNecesitarLaDireccion_C13() {
    donaciones
        .expect(requestTo(ARCHIVOS + "/" + ARCHIVO))
        .andRespond(
            withSuccess(
                """
                {"id": "00000000-0000-4000-8000-0000000000aa", "path": "x.csv",
                 "estado": "PROCESANDO", "totalFilas": 20000, "procesadas": 8400, "creadas": 7950,
                 "actualizadas": 410, "conError": 40, "ultimos": [],
                 "nombre": "donantes-octubre.csv", "subidoEn": "2026-10-15T18:02:00"}
                """,
                JSON));

    Importacion importacion = adaptador.importacion(ARCHIVO, null, "importando", 1);

    assertEquals("donantes-octubre.csv", importacion.archivo());
    assertEquals("15 de octubre de 2026, 18:02", importacion.subido());
    assertEquals("2026-10-15T18:02", importacion.subidoIso());
    assertEquals(ARCHIVO.toString(), importacion.id());
  }

  @Test
  void sinNombreEnDonacionesSeUsaElDeLaDireccion() {
    donaciones
        .expect(requestTo(ARCHIVOS + "/" + ARCHIVO))
        .andRespond(
            withSuccess(
                """
                {"id": "00000000-0000-4000-8000-0000000000aa", "path": "x.csv",
                 "estado": "PENDIENTE", "procesadas": 0, "creadas": 0, "actualizadas": 0,
                 "conError": 0}
                """,
                JSON));

    Importacion importacion = adaptador.importacion(ARCHIVO, "viejo.csv", "importando", 1);

    assertEquals("viejo.csv", importacion.archivo());
    assertNull(importacion.subido());
  }

  @Test
  void laDescargaPideDeACienYEscribeCadaFilaConSuTexto_C14() throws IOException {
    donaciones
        .expect(requestTo(ARCHIVOS + "/" + ARCHIVO))
        .andRespond(withSuccess(archivo(250), JSON));
    for (int tanda = 0; tanda < 3; tanda++) {
      int cuantas = tanda < 2 ? 100 : 50;
      donaciones
          .expect(
              once(),
              requestTo(ARCHIVOS + "/" + ARCHIVO + "/filas-con-error?page=" + tanda + "&size=100"))
          .andRespond(withSuccess(filas(tanda * 100, cuantas), JSON));
    }

    DescargaDeFilas descarga = adaptador.filasConError(ARCHIVO);
    StringWriter salida = new StringWriter();
    descarga.contenido().escribir(salida);

    donaciones.verify();
    assertEquals("donantes-octubre.csv", descarga.archivo());
    List<String> lineas = salida.toString().lines().toList();
    assertEquals(251, lineas.size());
    assertEquals("﻿Fila;Qué pasó;Código", lineas.get(0));
    assertEquals(
        "2;La fila no tiene las mismas columnas que el encabezado.;ERR-CSR-003", lineas.get(1));
  }

  @Test
  void siUnaTandaFallaElArchivoTerminaConElAviso_C14() throws IOException {
    donaciones
        .expect(requestTo(ARCHIVOS + "/" + ARCHIVO))
        .andRespond(withSuccess(archivo(250), JSON));
    donaciones
        .expect(requestTo(ARCHIVOS + "/" + ARCHIVO + "/filas-con-error?page=0&size=100"))
        .andRespond(withSuccess(filas(0, 100), JSON));
    donaciones
        .expect(requestTo(ARCHIVOS + "/" + ARCHIVO + "/filas-con-error?page=1&size=100"))
        .andRespond(withServerError());

    StringWriter salida = new StringWriter();
    adaptador.filasConError(ARCHIVO).contenido().escribir(salida);

    List<String> lineas = salida.toString().lines().toList();
    assertEquals(102, lineas.size());
    assertTrue(lineas.get(101).startsWith("No pudimos terminar la descarga: faltan filas."));
  }

  private static String archivo(int conError) {
    return """
        {"id": "00000000-0000-4000-8000-0000000000aa", "path": "x.csv",
         "estado": "PROCESADO_CON_ERRORES", "totalFilas": 1000, "procesadas": 1000,
         "creadas": 700, "actualizadas": 50, "conError": %d, "ultimos": [],
         "nombre": "donantes-octubre.csv", "subidoEn": "2026-10-15T18:02:00"}
        """
        .formatted(conError);
  }

  private static String filas(int desde, int cuantas) {
    return IntStream.range(desde, desde + cuantas)
        .mapToObj(
            i ->
                "{\"linea\": "
                    + (i + 2)
                    + ", \"motivo\": \"FORMATO_INVALIDO\", \"codigo\": \"ERR-CSR-003\"}")
        .collect(Collectors.joining(",", "[", "]"));
  }
}
