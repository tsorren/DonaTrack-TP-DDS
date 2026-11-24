package grupo5.cliente.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.cliente.maqueta.MaquetaConfig;
import grupo5.cliente.puertos.DonacionesPuerto;
import grupo5.cliente.vistas.DetalleDonacionVista;
import grupo5.cliente.vistas.DetalleDonacionVista.Noticia;
import grupo5.cliente.vistas.DetalleDonacionVista.Problema;
import grupo5.cliente.vistas.EstadoEntrega;
import grupo5.cliente.vistas.MisDonacionesVista;
import grupo5.cliente.vistas.MisDonacionesVista.Categoria;
import grupo5.cliente.vistas.MisDonacionesVista.Donacion;
import grupo5.cliente.vistas.PasoRecorrido;
import grupo5.cliente.vistas.PasoRecorrido.Avance;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * La pantalla con lo que da Donaciones: categorías por id, una sin ícono, y la cuenta por {@code
 * ?donante=}, que la sesión recuerda.
 */
@WebMvcTest(
    controllers = {MisDonacionesController.class, PantallasController.class},
    properties = "cliente.datos.cuenta=parametro")
@ActiveProfiles("maqueta")
@Import({
  MaquetaConfig.class,
  Pantallas.class,
  CuentaPorParametro.class,
  MisDonacionesControllerTest.DosDonaciones.class
})
class MisDonacionesControllerTest {

  private static final String ALIMENTOS = "6f1c2c1e-0000-4000-8000-0000000000c1";
  private static final String MUEBLES = "6f1c2c1e-0000-4000-8000-0000000000c2";
  private static final String MOBILIARIO = "Mobiliario";
  private static final String DONANTE = "6f1c2c1e-0000-4000-8000-0000000000aa";

  @TestConfiguration
  static class DosDonaciones {

    @Bean
    DonacionesPuerto donaciones() {
      return new DonacionesPuerto() {
        // Una por página: las dos de ejemplo alcanzan para probar el paginador.
        @Override
        public int porPagina() {
          return 1;
        }

        @Override
        public MisDonacionesVista misDonaciones(UUID donante) {
          return new MisDonacionesVista(
              List.of(
                  new Donacion(
                      "b",
                      2,
                      "Silla plegable",
                      "1 unidad",
                      MUEBLES,
                      null,
                      MOBILIARIO,
                      null,
                      "14 de octubre de 2026",
                      EstadoEntrega.PENDIENTE,
                      "En el depósito, espera destino"),
                  new Donacion(
                      "a",
                      1,
                      "Fideos secos",
                      "8 kg",
                      ALIMENTOS,
                      "cat-alimentos",
                      "Alimentos",
                      "Comedor Barrio Ejemplo",
                      "1 de octubre de 2026",
                      EstadoEntrega.ENTREGADA,
                      "Entrega confirmada el 5 de octubre")),
              List.of(
                  new Categoria(ALIMENTOS, "Alimentos", List.of(new Categoria("arroz", "Arroz"))),
                  new Categoria(MUEBLES, MOBILIARIO)),
              "hoy, 7 de noviembre, 19:40");
        }

        @Override
        public Optional<DetalleDonacionVista> detalle(UUID donante, String clave) {
          // Como lo arma el adaptador REST: entregada sin foto y sin "Figura como" (HB-20); en
          // camino y con problema, con sus cinco etapas y la última noticia.
          return switch (clave == null ? "" : clave) {
            case "a" ->
                Optional.of(
                    new DetalleDonacionVista(
                        misDonaciones(donante).donaciones().get(1),
                        "5 de octubre de 2026",
                        List.of(),
                        null,
                        null,
                        null,
                        "Comedor Barrio Ejemplo confirmó la entrega el 5 de octubre.",
                        List.of(),
                        null));
            case "c" -> Optional.of(enCurso("c", EstadoEntrega.EN_CAMINO, null));
            case "d" ->
                Optional.of(
                    enCurso(
                        "d",
                        EstadoEntrega.CON_PROBLEMA,
                        new Problema(
                            "No pudo llegar a Hogar Ejemplo.",
                            "El camino estaba cortado. Última noticia: 11 de octubre, 16:10.")));
            default -> Optional.empty();
          };
        }

        /** Una que ya tiene destino: en camino o con problema, en la cuarta etapa. */
        private DetalleDonacionVista enCurso(
            String clave, EstadoEntrega estado, Problema problema) {
          return new DetalleDonacionVista(
              new Donacion(
                  clave,
                  3,
                  "Mesa ratona",
                  "1 unidad",
                  MUEBLES,
                  null,
                  MOBILIARIO,
                  "Hogar Ejemplo",
                  "8 de octubre de 2026",
                  estado,
                  "Salió el 11 de octubre"),
              null,
              List.of(
                  new PasoRecorrido("En depósito", "8 de octubre", Avance.HECHO),
                  new PasoRecorrido("Asignada", "9 de octubre", Avance.HECHO),
                  new PasoRecorrido("Lista para entregar", "10 de octubre", Avance.HECHO),
                  new PasoRecorrido("En camino", "11 de octubre", Avance.ACTUAL),
                  new PasoRecorrido("Entregada", "Pendiente", Avance.PENDIENTE)),
              problema,
              null,
              null,
              null,
              List.of(
                  new Noticia(
                      "11 de octubre, 16:10",
                      "2026-10-11T16:10",
                      "El camión salió a entregar tu donación a Hogar Ejemplo.")),
              "Distribuidora Ejemplo SA");
        }
      };
    }
  }

  @Autowired private MockMvc mvc;

  @Test
  void sinCuentaOConUnIdMalEscritoMandaAIngresar() throws Exception {
    mvc.perform(get("/donante-donaciones")).andExpect(redirectedUrl("/publico-ingresar"));
    mvc.perform(get("/donante-donaciones?donante=no-es-un-id"))
        .andExpect(redirectedUrl("/publico-ingresar"));
  }

  @Test
  void laSesionRecuerdaAlDonanteYElFiltroUsaLasCategoriasDeLaApi() throws Exception {
    MockHttpSession sesion = new MockHttpSession();
    mvc.perform(get("/donante-donaciones?donante=" + DONANTE).session(sesion))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(
                    containsString(
                        "2 donaciones, de la más reciente a la primera. Actualizado hoy, 7 de"
                            + " noviembre, 19:40.")))
        .andExpect(content().string(containsString(MOBILIARIO)));

    mvc.perform(get("/donante-donaciones?categoria=" + ALIMENTOS).session(sesion))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("1 donación con esos filtros.")))
        .andExpect(content().string(containsString("Fideos secos")))
        .andExpect(content().string(not(containsString("Silla plegable"))));
  }

  /**
   * El arquetipo A05 de la guía (#donaciones): el título «Donaciones», la tabla que se apila con
   * una raíz y un sello por fila, y las vistas por estado con su cuenta, que no cambia con el
   * estado elegido y conserva los demás filtros.
   */
  @Test
  void esLaTablaDeLaGuiaConUnaRaizYUnSelloPorFilaYLasVistasConSuCuenta() throws Exception {
    MockHttpSession sesion = new MockHttpSession();
    mvc.perform(get("/donante-donaciones?donante=" + DONANTE).session(sesion))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("<h1>Donaciones</h1>")))
        .andExpect(content().string(containsString("<table class=\"tabla\">")))
        .andExpect(content().string(containsString("data-rotulo=\"Para\"")))
        .andExpect(content().string(containsString("data-rotulo=\"Lo último\"")))
        .andExpect(content().string(containsString("Donación 0002")))
        .andExpect(content().string(containsString("Todavía sin organización")))
        .andExpect(
            content()
                .string(
                    containsString(
                        "aria-label=\"El recorrido de esta donación: va por el paso 1 de 5\"")))
        .andExpect(
            content().string(containsString("<span class=\"estado\" data-estado=\"pendiente\">")))
        .andExpect(content().string(containsString("Paso 1 de 5")))
        .andExpect(content().string(containsString("aria-label=\"Donaciones por estado\"")))
        .andExpect(
            content()
                .string(
                    matchesPattern(
                        "(?s).*href=\"/donante-donaciones\\?donante=[^\"]+\"\\s+aria-current=\"page\">"
                            + "Todas <b>2</b>.*")))
        .andExpect(content().string(containsString(">Pendientes <b>1</b></a>")))
        .andExpect(content().string(containsString(">En camino <b>0</b></a>")))
        .andExpect(content().string(not(containsString("huerta-lista"))));

    // Con un estado elegido: su vista queda marcada, las cuentas no cambian y se conserva el mes.
    mvc.perform(get("/donante-donaciones?mes=2026-10&pagina=2&estado=entregada").session(sesion))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("1 donación con esos filtros.")))
        .andExpect(
            content()
                .string(
                    containsString(
                        "aria-label=\"El recorrido de esta donación: los 5 pasos, cumplidos\"")))
        .andExpect(content().string(containsString("Paso 5 de 5")))
        .andExpect(content().string(containsString("Entrega confirmada el 5 de octubre.")))
        .andExpect(
            content()
                .string(
                    containsString(
                        "href=\"/donante-donaciones?mes=2026-10&amp;estado=entregada\""
                            + " aria-current=\"page\">Entregadas <b>1</b></a>")))
        .andExpect(
            content()
                .string(
                    containsString(
                        "href=\"/donante-donaciones?mes=2026-10&amp;estado=pendiente\">"
                            + "Pendientes <b>1</b></a>")))
        .andExpect(
            content()
                .string(containsString("href=\"/donante-donaciones?mes=2026-10\">Todas <b>2</b>")));
  }

  @Test
  void conUnMesMuestraSoloLasDeEseMesYLoDice() throws Exception {
    MockHttpSession sesion = new MockHttpSession();
    mvc.perform(get("/donante-donaciones?mes=2026-10&donante=" + DONANTE).session(sesion))
        .andExpect(status().isOk())
        .andExpect(
            content().string(containsString("Estás viendo las registradas en octubre de 2026.")))
        .andExpect(content().string(containsString("Ver todas tus donaciones")))
        // R11-B04: el mes llega a la barra como sus días, cada uno atado a su campo
        .andExpect(
            content()
                .string(
                    matchesPattern("(?s).*name=\"desde\" type=\"date\"\\s+value=\"2026-10-01\".*")))
        .andExpect(
            content()
                .string(
                    matchesPattern("(?s).*name=\"hasta\" type=\"date\"\\s+value=\"2026-10-31\".*")))
        .andExpect(content().string(containsString("Silla plegable")));

    mvc.perform(get("/donante-donaciones?mes=2026-09").session(sesion))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(containsString("No hay donaciones registradas en septiembre de 2026.")))
        .andExpect(content().string(not(containsString("Silla plegable"))));

    mvc.perform(get("/donante-donaciones?mes=no-es-un-mes").session(sesion))
        .andExpect(status().isOk())
        .andExpect(content().string(not(containsString("Estás viendo las registradas"))))
        .andExpect(content().string(containsString("Silla plegable")));
  }

  @Test
  void elPaginadorMuestraLaPaginaYConservaElMesYLosFiltros() throws Exception {
    MockHttpSession sesion = new MockHttpSession();
    mvc.perform(get("/donante-donaciones?mes=2026-10&donante=" + DONANTE).session(sesion))
        .andExpect(status().isOk())
        .andExpect(
            content().string(containsString("2 donaciones, de la más reciente a la primera.")))
        .andExpect(content().string(containsString("Página 1 de 2")))
        .andExpect(content().string(containsString("Silla plegable")))
        .andExpect(content().string(not(containsString("Fideos secos"))))
        .andExpect(
            content()
                .string(
                    containsString(
                        "href=\"/donante-donaciones?mes=2026-10&amp;donante="
                            + DONANTE
                            + "&amp;pagina=2\"")))
        // B7: el componente paginas, el mismo de las otras listas
        .andExpect(content().string(containsString("aria-label=\"Páginas de tus donaciones\"")))
        .andExpect(content().string(not(containsString("Página anterior"))));

    mvc.perform(get("/donante-donaciones?mes=2026-10&pagina=2").session(sesion))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Página 2 de 2")))
        .andExpect(content().string(containsString("Fideos secos")))
        .andExpect(content().string(not(containsString("Silla plegable"))))
        .andExpect(
            content()
                .string(containsString("href=\"/donante-donaciones?mes=2026-10&amp;pagina=1\"")))
        .andExpect(content().string(not(containsString("Página siguiente"))));

    // Una página de más muestra la última; una mal escrita, la primera.
    mvc.perform(get("/donante-donaciones?pagina=9").session(sesion))
        .andExpect(content().string(containsString("Página 2 de 2")));
    mvc.perform(get("/donante-donaciones?pagina=dos").session(sesion))
        .andExpect(content().string(containsString("Página 1 de 2")));

    // Con un filtro que deja una sola, no hay páginas; el total cuenta las filtradas.
    mvc.perform(get("/donante-donaciones?estado=entregada").session(sesion))
        .andExpect(content().string(containsString("1 donación con esos filtros.")))
        .andExpect(content().string(not(containsString("Página 1 de"))));
    mvc.perform(get("/donante-donaciones?estado=en-camino").session(sesion))
        .andExpect(content().string(containsString("No hay donaciones con esos filtros.")))
        .andExpect(content().string(not(containsString("Todavía no tenés donaciones"))))
        .andExpect(content().string(not(containsString("todavía no tiene donaciones"))));
  }

  /**
   * R11-B04: con la maqueta, cada campo de la barra filtra los datos de ejemplo: el rango de fechas
   * (al revés se da vuelta), la organización a la que fue (sin mayúsculas ni tildes) y el orden.
   * "Sacar los filtros" aparece con algo elegido.
   */
  @Test
  void laBarraFiltraPorFechasOrganizacionYOrden() throws Exception {
    MockHttpSession sesion = new MockHttpSession();
    mvc.perform(get("/donante-donaciones?donante=" + DONANTE).session(sesion))
        .andExpect(content().string(not(containsString("Sacar los filtros"))));

    mvc.perform(get("/donante-donaciones?desde=2026-10-20&hasta=2026-10-10").session(sesion))
        .andExpect(content().string(containsString("1 donación con esos filtros.")))
        .andExpect(content().string(containsString("Silla plegable")))
        .andExpect(content().string(not(containsString("Fideos secos"))))
        .andExpect(content().string(containsString("value=\"2026-10-10\">")))
        .andExpect(content().string(containsString("value=\"2026-10-20\">")))
        // Sin max ni min: la persona puede correr el rango entero (revisión de R11-B04)
        .andExpect(content().string(not(containsString("max=\"2026-10-20\""))))
        .andExpect(content().string(containsString("Sacar los filtros")));

    mvc.perform(get("/donante-donaciones?quien=COMEDOR barrio").session(sesion))
        .andExpect(content().string(containsString("1 donación con esos filtros.")))
        .andExpect(content().string(containsString("Fideos secos")))
        .andExpect(content().string(not(containsString("Silla plegable"))));

    mvc.perform(get("/donante-donaciones?orden=antiguas").session(sesion))
        .andExpect(
            content().string(containsString("2 donaciones, de la primera a la más reciente.")))
        .andExpect(content().string(containsString("Fideos secos")))
        .andExpect(content().string(not(containsString("Silla plegable"))))
        .andExpect(content().string(matchesPattern(elegida("antiguas", "Más antiguas primero"))));
  }

  /** La opción {@code valor} del select, marcada como elegida. */
  private static Pattern elegida(String valor, String texto) {
    return Pattern.compile(
        "(?s).*value=\"" + valor + "\"\\s+selected=\"selected\">" + texto + "</option>.*");
  }

  /** B6 (RF-05): la subcategoría elegida queda marcada y la categoría muestra la suya. */
  @Test
  void laSubcategoriaQuedaElegidaConSuCategoria() throws Exception {
    MockHttpSession sesion = new MockHttpSession();
    mvc.perform(
            get("/donante-donaciones?categoria="
                    + MUEBLES
                    + "&subcategoria=arroz&donante="
                    + DONANTE)
                .session(sesion))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(
                    containsString("<label for=\"donaciones-subcategoria\">Subcategoría</label>")))
        .andExpect(content().string(matchesPattern(elegida("arroz", "Arroz"))))
        .andExpect(content().string(matchesPattern(elegida(ALIMENTOS, "Alimentos"))))
        .andExpect(content().string(containsString("Fideos secos")))
        .andExpect(content().string(not(containsString("Silla plegable"))));
  }

  @Test
  void elDetalleMuestraLoQueDaLaApiYNoInventaLaDeOtroDonante() throws Exception {
    mvc.perform(get("/donante-donaciones-detalle?donacion=a"))
        .andExpect(redirectedUrl("/publico-ingresar"));

    MockHttpSession sesion = new MockHttpSession();
    mvc.perform(get("/donante-donaciones-detalle?donacion=a&donante=" + DONANTE).session(sesion))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(
                    containsString("Comedor Barrio Ejemplo confirmó la entrega el 5 de octubre.")))
        .andExpect(content().string(not(containsString("Figura como"))))
        .andExpect(content().string(not(containsString("Falta confirmar la entrega"))));

    mvc.perform(get("/donante-donaciones-detalle?donacion=b").session(sesion))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("No encontramos esa donación.")));
  }

  /**
   * DA-03a: el seguimiento de una donación se abre desde su detalle. La en camino (A07) lleva el
   * enlace a su seguimiento; la entregada, no.
   */
  @Test
  void elDetalleEnCaminoEnlazaASuSeguimiento() throws Exception {
    MockHttpSession sesion = new MockHttpSession();
    mvc.perform(get("/donante-donaciones-detalle?donacion=c&donante=" + DONANTE).session(sesion))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("<h1>Mesa ratona, 1 unidad</h1>")))
        .andExpect(content().string(containsString("href=\"/donante-seguimiento?donacion=c\"")))
        .andExpect(content().string(containsString("Ver dónde está el camión")))
        .andExpect(content().string(containsString("aria-current=\"step\"")))
        .andExpect(content().string(containsString("figurás como «")));

    mvc.perform(get("/donante-donaciones-detalle?donacion=a").session(sesion))
        .andExpect(content().string(not(containsString("/donante-seguimiento?donacion="))));
  }

  /**
   * A08 y HG-01: la con problema dice qué pasó una sola vez (el aviso, sin la última noticia, que
   * va en «Qué sigue») y no inventa la nueva salida; el paso que no pudo ser lleva su signo.
   */
  @Test
  void elDetalleConProblemaNoInventaLaNuevaSalida() throws Exception {
    MockHttpSession sesion = new MockHttpSession();
    mvc.perform(get("/donante-donaciones-detalle?donacion=d&donante=" + DONANTE).session(sesion))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("class=\"aviso\" data-tipo=\"error\"")))
        .andExpect(content().string(containsString("No pudo llegar a Hogar Ejemplo.")))
        .andExpect(content().string(containsString("<p>El camino estaba cortado.</p>")))
        .andExpect(content().string(containsString("Todavía sin fecha de nueva salida")))
        .andExpect(content().string(containsString("data-paso=\"problema\"")))
        .andExpect(content().string(not(containsString("/donante-seguimiento?donacion="))));
  }
}
