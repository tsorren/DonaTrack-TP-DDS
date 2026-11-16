package grupo5.cliente.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.cliente.maqueta.MaquetaConfig;
import grupo5.cliente.puertos.PublicoMapaPuerto;
import grupo5.cliente.vistas.PublicoMapaVista;
import grupo5.cliente.vistas.PublicoMapaVista.Entrega;
import grupo5.cliente.vistas.PublicoMapaVista.Estacion;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * El mapa con lo que da el puerto, sin lo que Logística no trae: sin entidad, categoría ni foto,
 * con el punto como un lugar.
 */
@WebMvcTest(PublicoMapaController.class)
@ActiveProfiles("maqueta")
@Import({MaquetaConfig.class, Pantallas.class, PublicoMapaControllerTest.DatosDeLaApi.class})
class PublicoMapaControllerTest {

  @TestConfiguration
  static class DatosDeLaApi {

    @Bean
    PublicoMapaPuerto mapa() {
      return escenario -> datos;
    }
  }

  /** Lo que devuelve el puerto; cada test lo arma. */
  private static PublicoMapaVista datos;

  @Autowired private MockMvc mvc;

  private static PublicoMapaVista conLugares(Estacion... estaciones) {
    return new PublicoMapaVista(
        List.of(estaciones),
        "del 11 de octubre al 10 de noviembre de 2026",
        "10 de noviembre de 2026, 14:30",
        false,
        false,
        "Todavía no mostramos X.");
  }

  /** {@code cuantos} lugares con una entrega cada uno, del 1 al {@code cuantos}. */
  private static PublicoMapaVista conMuchos(int cuantos, long delPeriodo) {
    List<Estacion> lugares = new ArrayList<>();
    for (int n = 1; n <= cuantos; n++) {
      lugares.add(
          new Estacion(
              "lugar-" + n,
              n,
              -34.6 - n / 100.0,
              -58.4,
              "Lugar Ejemplo " + n,
              "Buenos Aires",
              List.of(),
              List.of(
                  new Entrega(
                      "Entrega de 3 kg",
                      null,
                      null,
                      "2 de noviembre",
                      "2026",
                      "2026-11-02",
                      "Lugar Ejemplo " + n,
                      null,
                      null))));
    }
    return new PublicoMapaVista(
        lugares,
        "del 13 de octubre al 12 de noviembre de 2026",
        "12 de noviembre de 2026, 10:30",
        false,
        false,
        null,
        delPeriodo);
  }

  private static Estacion palermo() {
    return new Estacion(
        "lugar-1",
        1,
        -34.6037,
        -58.3816,
        "Palermo",
        "Buenos Aires",
        List.of(),
        List.of(
            new Entrega(
                "Entrega de 12,5 kg",
                null,
                null,
                "2 de noviembre",
                "2026",
                "2026-11-02",
                "Palermo",
                null,
                null)));
  }

  @Test
  void muestraLosLugaresDelPuertoYDiceLoQueNoSabe() throws Exception {
    datos = conLugares(palermo());

    mvc.perform(get("/publico-mapa"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Lugar por lugar")))
        .andExpect(content().string(containsString("Palermo")))
        .andExpect(content().string(containsString("Entrega de 12,5 kg · 2 de noviembre")))
        .andExpect(content().string(containsString("data-lat=\"-34.6037\"")))
        .andExpect(content().string(containsString("en 1 lugar, del 11 de octubre")))
        .andExpect(
            content().string(containsString("Actualizado el 10 de noviembre de 2026, 14:30")))
        .andExpect(content().string(containsString("Mostramos la localidad, nunca la dirección")))
        .andExpect(content().string(containsString("Todavía no mostramos X.")))
        .andExpect(content().string(not(containsString("Ver la foto de la entrega</button>"))))
        .andExpect(content().string(not(containsString("Qué se donó"))))
        .andExpect(content().string(not(containsString("Pide:"))))
        .andExpect(content().string(not(containsString("Comedor Barrio Ejemplo"))));
  }

  @Test
  void unLugarSinProvinciaNoLlevaElSeparadorSuelto() throws Exception {
    Estacion palermo = palermo();
    datos =
        conLugares(
            new Estacion(
                palermo.clave(),
                palermo.numero(),
                palermo.lat(),
                palermo.lng(),
                palermo.nombre(),
                "",
                palermo.pide(),
                palermo.entregas()));

    mvc.perform(get("/publico-mapa"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString(">1 entrega confirmada<")))
        .andExpect(content().string(not(containsString(" · 1 entrega confirmada"))));
  }

  @Test
  void sinEntregasMuestraElEstadoVacio() throws Exception {
    datos = conLugares();

    mvc.perform(get("/publico-mapa"))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(containsString("Todavía no hay entregas confirmadas en el último mes.")));
  }

  @Test
  void laListaVaPlegadaYLosBotonesDebajoDelMapa() throws Exception {
    datos = conLugares(palermo());

    String pagina =
        mvc.perform(get("/publico-mapa"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("<details class=\"plegable\"")))
            .andExpect(content().string(containsString("name=\"lugar-del-mapa\"")))
            .andExpect(content().string(containsString("Número 1 en el mapa.")))
            .andExpect(content().string(not(containsString("aria-label=\"Páginas de"))))
            .andReturn()
            .getResponse()
            .getContentAsString();

    int botones = pagina.indexOf("Quiero donar");
    assertTrue(pagina.indexOf("data-mapa ") < botones, "los botones van después del mapa");
    assertTrue(botones < pagina.indexOf("id=\"lista-del-mapa\""), "y antes de la lista");
    assertFalse(pagina.contains("<details class=\"plegable\" name=\"lugar-del-mapa\" open"));
  }

  @Test
  void conMasDeDiezLugaresLaListaSePaginaYElMapaTieneTodos() throws Exception {
    datos = conMuchos(14, 14);

    String pagina =
        mvc.perform(get("/publico-mapa"))
            .andExpect(status().isOk())
            .andExpect(
                content().string(containsString("aria-label=\"Páginas de la lista del mapa\"")))
            .andExpect(content().string(containsString("id=\"entidad-lugar-10\"")))
            .andExpect(content().string(not(containsString("id=\"entidad-lugar-11\""))))
            .andExpect(content().string(containsString("data-ficha=\"entidad-lugar-14\"")))
            .andExpect(
                content()
                    .string(
                        containsString(
                            "/publico-mapa?pagina=2&amp;abierta=lugar-11#entidad-lugar-11")))
            .andExpect(content().string(containsString("en 14 lugares, del 13 de octubre")))
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertEquals(14, pagina.split("data-ficha=", -1).length - 1);
  }

  @Test
  void laPaginaDosEmpiezaEnElOnceYDiceDondeEsta() throws Exception {
    datos = conMuchos(14, 14);

    mvc.perform(get("/publico-mapa").param("pagina", "2"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("id=\"entidad-lugar-11\"")))
        .andExpect(content().string(not(containsString("id=\"entidad-lugar-10\""))))
        .andExpect(content().string(containsString("Página 2 de 2 de la lista.")))
        .andExpect(content().string(containsString("autofocus")));
  }

  @Test
  void unaPaginaFueraDeRangoMuestraLaUltima() throws Exception {
    datos = conMuchos(14, 14);

    mvc.perform(get("/publico-mapa").param("pagina", "9"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("id=\"entidad-lugar-14\"")))
        .andExpect(content().string(not(containsString("id=\"entidad-lugar-1\""))));
    mvc.perform(get("/publico-mapa").param("pagina", "-3"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("id=\"entidad-lugar-1\"")));
  }

  @Test
  void conAbiertaLaFilaVieneAbiertaYElPaginadorLaQuita() throws Exception {
    datos = conMuchos(14, 14);

    mvc.perform(get("/publico-mapa").param("pagina", "2").param("abierta", "lugar-12"))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(
                    containsString(
                        "<details class=\"plegable\" name=\"lugar-del-mapa\" open=\"open\">")))
        .andExpect(content().string(containsString("href=\"/publico-mapa?pagina=1\"")));
  }

  @Test
  void conMasEntregasQueElTopeLaCifraDiceCuantasHayYCuantasMuestra() throws Exception {
    datos = conMuchos(3, 340);

    mvc.perform(get("/publico-mapa"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString(">340</b>")))
        .andExpect(
            content()
                .string(
                    containsString(
                        "entregas confirmadas del 13 de octubre al 12 de noviembre de 2026. El mapa"
                            + " muestra las 3 más recientes, en 3 lugares.")));
  }

  @Test
  void conCategoriasSinFiltroOConUnoQueNoExisteMuestraTodas() throws Exception {
    Estacion palermo = palermo();
    Entrega ropa =
        new Entrega(
            "Ropa de abrigo, 5 unidades",
            "ropa",
            "Ropa",
            "3 de noviembre",
            "2026",
            "2026-11-03",
            "Palermo",
            null,
            null);
    datos =
        new PublicoMapaVista(
            List.of(
                new Estacion(
                    palermo.clave(),
                    palermo.numero(),
                    palermo.lat(),
                    palermo.lng(),
                    palermo.nombre(),
                    palermo.barrio(),
                    palermo.pide(),
                    List.of(ropa))),
            "del 11 de octubre al 10 de noviembre de 2026",
            "10 de noviembre de 2026, 14:30",
            false,
            true,
            null);

    mvc.perform(get("/publico-mapa"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Ropa de abrigo, 5 unidades · Ropa")));
    mvc.perform(get("/publico-mapa").param("categoria", "otra"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Ropa de abrigo, 5 unidades · Ropa")));
    mvc.perform(get("/publico-mapa").param("categoria", "alimentos"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Todavía no hay entregas de alimentos")));
  }
}
