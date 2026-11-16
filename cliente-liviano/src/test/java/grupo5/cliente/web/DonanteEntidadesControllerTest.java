package grupo5.cliente.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.cliente.maqueta.MaquetaConfig;
import grupo5.cliente.puertos.CuentaActual;
import grupo5.cliente.puertos.DonanteEntidadesPuerto;
import grupo5.cliente.puertos.DonanteEntidadesPuerto.Consulta;
import grupo5.cliente.vistas.DonanteEntidadesVista;
import grupo5.cliente.vistas.DonanteEntidadesVista.Categoria;
import grupo5.cliente.vistas.DonanteEntidadesVista.Entidad;
import grupo5.cliente.vistas.DonanteEntidadesVista.Pedido;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Entidades del donante con una página del puerto: sin lo recibido, el pedido va sin barra; la
 * categoría y "Ya doné" llegan resueltas sobre todas (R11-12) y la pantalla no vuelve a filtrar.
 */
@WebMvcTest(controllers = {DonanteEntidadesController.class, PantallasController.class})
@ActiveProfiles("maqueta")
@Import({MaquetaConfig.class, Pantallas.class})
class DonanteEntidadesControllerTest {

  private static final UUID DONANTE = UUID.fromString("6f1c2c1e-0000-4000-8000-0000000000aa");
  private static final String HOY = "hoy, 7 de noviembre, 19:40";
  private static final String HIGIENE = "6f1c2c1e-0000-4000-8000-0000000000a2";
  private static final List<Categoria> CATEGORIAS =
      List.of(
          new Categoria("alimentos", "Alimentos", "cat-alimentos"),
          new Categoria(HIGIENE, "Higiene", "cat-higiene"),
          new Categoria("ropa", "Ropa", "cat-ropa"));

  @MockitoBean private DonanteEntidadesPuerto entidades;
  @MockitoBean private CuentaActual cuentas;
  @Autowired private MockMvc mvc;

  private static DonanteEntidadesVista vista(
      List<Entidad> lista, long total, int pagina, int paginas) {
    return new DonanteEntidadesVista(lista, total, pagina, paginas, paginas, CATEGORIAS, HOY);
  }

  private static Pedido frazadas() {
    return new Pedido(
        "Frazadas", "ropa", "cat-ropa", "Piden 20 unidades", "Hasta el 15 de noviembre", null, 20);
  }

  @Test
  void muestraLaPaginaPedidaSinBarraCuandoNoHayLoRecibido() throws Exception {
    when(cuentas.donante(any())).thenReturn(Optional.of(DONANTE));
    when(entidades.entidades(eq(DONANTE), any()))
        .thenReturn(
            vista(List.of(new Entidad("Hogar Ejemplo", false, List.of(frazadas()))), 7, 2, 2));

    mvc.perform(get("/donante-entidades?pagina=2"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("7 organizaciones activas. Página 2 de 2.")))
        .andExpect(content().string(containsString("Piden 20 unidades · Hasta el 15 de noviembre")))
        .andExpect(content().string(containsString("Actualizado\n        hoy, 7 de noviembre")))
        .andExpect(content().string(not(containsString("<progress"))))
        .andExpect(content().string(not(containsString("solo está dibujada"))))
        // B7: el componente paginas, no el nav propio
        .andExpect(content().string(containsString("aria-label=\"Páginas de organizaciones\"")))
        .andExpect(content().string(containsString("aria-label=\"Página anterior\"")))
        .andExpect(content().string(not(containsString(">Página anterior</a>"))));
    verify(entidades).entidades(DONANTE, new Consulta("", null, false, 2));
  }

  @Test
  void sinCuentaMandaAIngresar() throws Exception {
    when(cuentas.donante(any())).thenReturn(Optional.empty());

    mvc.perform(get("/donante-entidades")).andExpect(redirectedUrl("/publico-ingresar"));
  }

  @Test
  void sinEntidadesActivasMuestraElEstadoVacio() throws Exception {
    when(cuentas.donante(any())).thenReturn(Optional.of(DONANTE));
    when(entidades.entidades(eq(DONANTE), any())).thenReturn(vista(List.of(), 0, 1, 1));

    mvc.perform(get("/donante-entidades"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Por ahora no hay organizaciones activas.")))
        .andExpect(content().string(not(containsString("organizaciones activas. Página"))))
        .andExpect(content().string(not(containsString("class=\"paginas\""))));
  }

  /** B1, B4 y B8: "Ya doné" cuenta todas y el paginador lo conserva. */
  @Test
  void yaDoneFiltraTodasYElPaginadorLoConserva() throws Exception {
    when(cuentas.donante(any())).thenReturn(Optional.of(DONANTE));
    when(entidades.entidades(eq(DONANTE), any()))
        .thenReturn(
            vista(
                List.of(
                    new Entidad("Hogar Ejemplo", true, List.of(frazadas())),
                    new Entidad("Club Ejemplo", true, List.of(frazadas()))),
                14,
                2,
                3));

    mvc.perform(get("/donante-entidades?ya=si&nombre=comedor&pagina=2"))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(
                    containsString(
                        "14 organizaciones coinciden con lo que elegiste. Página 2 de 3.")))
        .andExpect(content().string(containsString("Club Ejemplo")))
        .andExpect(content().string(not(containsString("En esta página"))))
        .andExpect(content().string(containsString("ya=si&amp;nombre=comedor&amp;pagina=3")))
        .andExpect(content().string(containsString("ya=si&amp;nombre=comedor&amp;pagina=1")));
    verify(entidades).entidades(DONANTE, new Consulta("comedor", null, true, 2));
  }

  @Test
  void yaDoneSinNingunaLoDiceConLaCuentaNueva() throws Exception {
    when(cuentas.donante(any())).thenReturn(Optional.of(DONANTE));
    when(entidades.entidades(eq(DONANTE), any())).thenReturn(vista(List.of(), 0, 1, 1));

    mvc.perform(get("/donante-entidades?ya=si"))
        .andExpect(
            content().string(containsString("Todavía no le donaste a ninguna organización.")))
        .andExpect(
            content()
                .string(
                    containsString(
                        "Cuando el depósito asigne una de tus donaciones, la organización aparece acá.")))
        .andExpect(content().string(containsString("Ver todas")));
  }

  @Test
  void conCategoriaYNingunaSugiereSacarLosFiltros() throws Exception {
    when(cuentas.donante(any())).thenReturn(Optional.of(DONANTE));
    when(entidades.entidades(eq(DONANTE), any())).thenReturn(vista(List.of(), 0, 1, 1));

    mvc.perform(get("/donante-entidades?categoria=ropa&ya=si"))
        .andExpect(
            content().string(containsString("Ninguna organización coincide con lo que elegiste.")))
        .andExpect(
            content().string(containsString("Probá con otra categoría o sacá los filtros.")));
  }

  /**
   * B2, B3 y R11-27: las categorías del filtro son las de la vista (Higiene también); la lista no
   * se agrupa por categoría: una tarjeta por organización, con sus categorías en el pie. Con una
   * categoría elegida, sus pedidos van primero y una organización sin nada pendiente de ella lo
   * dice en su tarjeta.
   */
  @Test
  void unaTarjetaPorOrganizacionYLaCategoriaComoFiltro() throws Exception {
    when(cuentas.donante(any())).thenReturn(Optional.of(DONANTE));
    Pedido jabon =
        new Pedido(
            "Jabón", HIGIENE, "cat-higiene", "Piden 30 unidades", "Sin fecha de cierre", null, 30);
    when(entidades.entidades(eq(DONANTE), any()))
        .thenReturn(
            vista(
                List.of(
                    new Entidad("Hogar Ejemplo", false, List.of(frazadas(), jabon)),
                    new Entidad("Club Ejemplo", false, List.of(frazadas()))),
                5,
                1,
                1));

    String todas =
        mvc.perform(get("/donante-entidades"))
            .andExpect(content().string(containsString("Todas las categorías")))
            .andExpect(content().string(not(containsString("data-v=\"grupo\""))))
            .andExpect(content().string(containsString("Higiene · Ropa")))
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(veces(todas, "data-v=\"organizacion\"")).isEqualTo(2);
    assertThat(veces(todas, "<span data-v=\"nombre\">Hogar Ejemplo</span>")).isEqualTo(1);
    assertThat(todas.indexOf("Frazadas")).isLessThan(todas.indexOf("Jabón"));

    String higiene =
        mvc.perform(get("/donante-entidades?categoria=" + HIGIENE))
            .andExpect(
                content()
                    .string(
                        containsString(
                            "5 organizaciones coinciden con lo que elegiste. Página 1 de 1.")))
            .andExpect(
                content().string(containsString("Lo que pedía de Higiene ya está cubierto.")))
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(veces(higiene, "data-v=\"organizacion\"")).isEqualTo(2);
    assertThat(higiene.indexOf("Jabón")).isLessThan(higiene.indexOf("Frazadas"));
    verify(entidades).entidades(DONANTE, new Consulta("", HIGIENE, false, 1));
  }

  /** I6: un pedido sin categoría conocida no se pierde: va con el ícono de respaldo y "Otras". */
  @Test
  void losPedidosSinCategoriaVanConOtras() throws Exception {
    when(cuentas.donante(any())).thenReturn(Optional.of(DONANTE));
    Pedido suelto = new Pedido("Pañales", null, null, "Piden 10", "Sin fecha de cierre", null, 10);
    when(entidades.entidades(eq(DONANTE), any()))
        .thenReturn(
            vista(
                List.of(new Entidad("Hogar Ejemplo", false, List.of(frazadas(), suelto))),
                1,
                1,
                1));

    mvc.perform(get("/donante-entidades"))
        .andExpect(content().string(containsString("Pañales")))
        .andExpect(content().string(containsString("#cat-generico")))
        .andExpect(content().string(containsString("Ropa · Otras")))
        .andExpect(content().string(not(containsString("Otros pedidos"))));
  }

  /**
   * B1 e I2: una organización que llega sin pedidos vigentes (con "Ya doné", una que ya tiene todo
   * cubierto) también se ve, con su marca, para que la pantalla muestre todas las que cuenta.
   */
  @Test
  void unaEntidadSinPedidosVigentesTambienSeVe() throws Exception {
    when(cuentas.donante(any())).thenReturn(Optional.of(DONANTE));
    when(entidades.entidades(eq(DONANTE), any()))
        .thenReturn(
            vista(
                List.of(
                    new Entidad("Hogar Ejemplo", true, List.of(frazadas())),
                    new Entidad("Club Ejemplo", true, List.of(frazadas())),
                    new Entidad("Comedor Ejemplo Este", true, List.of())),
                3,
                1,
                1));

    String pagina =
        mvc.perform(get("/donante-entidades?ya=si"))
            .andExpect(
                content()
                    .string(
                        containsString(
                            "3 organizaciones coinciden con lo que elegiste. Página 1 de 1.")))
            .andExpect(content().string(not(containsString("Sin pedidos pendientes"))))
            .andExpect(
                content()
                    .string(
                        containsString(
                            "<span data-v=\"nombre\">Comedor Ejemplo Este</span><span data-v=\"ya-donaste\">Ya le donaste</span>")))
            .andExpect(content().string(containsString("Hoy no pide nada.")))
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(veces(pagina, "data-v=\"organizacion\"")).isEqualTo(3);
  }

  /** Con una categoría elegida, la organización sin pedidos sale una sola vez, con el aviso. */
  @Test
  void conCategoriaLaEntidadSinPedidosNoSeRepite() throws Exception {
    when(cuentas.donante(any())).thenReturn(Optional.of(DONANTE));
    when(entidades.entidades(eq(DONANTE), any()))
        .thenReturn(vista(List.of(new Entidad("Comedor Ejemplo Este", true, List.of())), 1, 1, 1));

    String pagina =
        mvc.perform(get("/donante-entidades?categoria=ropa"))
            .andExpect(content().string(containsString("Lo que pedía de Ropa ya está cubierto.")))
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(veces(pagina, "<span data-v=\"nombre\">Comedor Ejemplo Este</span>")).isEqualTo(1);
  }

  /** R11-27: hasta 3 pedidos a la vista; el resto, en un plegable que dice cuántos faltan. */
  @Test
  void conMasDeTresPedidosElRestoVaPlegado() throws Exception {
    when(cuentas.donante(any())).thenReturn(Optional.of(DONANTE));
    when(entidades.entidades(eq(DONANTE), any()))
        .thenReturn(
            vista(
                List.of(
                    new Entidad("Hogar Ejemplo", false, pedidos(5)),
                    new Entidad("Club Ejemplo", false, pedidos(4)),
                    new Entidad("Merendero Ejemplo", false, pedidos(3))),
                3,
                1,
                1));

    String pagina =
        mvc.perform(get("/donante-entidades"))
            .andExpect(content().string(containsString("Ver 2 necesidades más")))
            .andExpect(content().string(containsString("Ver 1 necesidad más")))
            .andExpect(
                content()
                    .string(
                        containsString("<span class=\"solo-lectores\"> de Hogar Ejemplo</span>")))
            .andExpect(content().string(containsString("Ver menos")))
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(veces(pagina, "<details data-v=\"mas\"")).isEqualTo(2);
    // El cuarto pedido de Hogar va dentro de su plegable; el tercero, a la vista.
    int hogar = pagina.indexOf("<span data-v=\"nombre\">Hogar Ejemplo</span>");
    int plegable = pagina.indexOf("<details data-v=\"mas\"", hogar);
    assertThat(pagina.indexOf("Pedido 4", hogar)).isGreaterThan(plegable);
    assertThat(pagina.indexOf("Pedido 3", hogar)).isLessThan(plegable);
  }

  /**
   * R11-28: "Cómo llega tu donación" es una franja de ayuda arriba de la búsqueda, no una tarjeta
   * más; "Ver Mis donaciones" es un enlace, no un botón.
   */
  @Test
  void comoLlegaEsUnaFranjaDeAyudaArribaDeLaLista() throws Exception {
    when(cuentas.donante(any())).thenReturn(Optional.of(DONANTE));
    when(entidades.entidades(eq(DONANTE), any()))
        .thenReturn(
            vista(List.of(new Entidad("Hogar Ejemplo", false, List.of(frazadas()))), 1, 1, 1));

    String pagina =
        mvc.perform(get("/donante-entidades"))
            .andExpect(
                content()
                    .string(
                        containsString("<aside data-v=\"ayuda\" aria-labelledby=\"como-titulo\">")))
            .andExpect(
                content()
                    .string(
                        containsString("<a href=\"/donante-donaciones\">Ver Mis donaciones</a>")))
            .andExpect(content().string(not(containsString("como-donar"))))
            .andExpect(content().string(not(containsString("por categoría"))))
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(pagina.indexOf("data-v=\"ayuda\"")).isLessThan(pagina.indexOf("role=\"search\""));
    assertThat(pagina.indexOf("data-v=\"ayuda\""))
        .isLessThan(pagina.indexOf("data-v=\"organizacion\""));
  }

  @Test
  void conErrorLaFranjaSigue() throws Exception {
    when(cuentas.donante(any())).thenReturn(Optional.of(DONANTE));
    when(entidades.entidades(eq(DONANTE), any())).thenReturn(vista(List.of(), 0, 1, 1));

    mvc.perform(get("/donante-entidades?escenario=error"))
        .andExpect(content().string(containsString("No pudimos cargar las organizaciones.")))
        .andExpect(content().string(containsString("data-v=\"ayuda\"")));
  }

  private static List<Pedido> pedidos(int cuantos) {
    return IntStream.rangeClosed(1, cuantos)
        .mapToObj(
            i ->
                new Pedido(
                    "Pedido " + i, "ropa", "cat-ropa", "Piden 10 unidades", "Sin fecha", null, 10))
        .toList();
  }

  private static int veces(String texto, String buscado) {
    int cuenta = 0;
    for (int i = texto.indexOf(buscado); i >= 0; i = texto.indexOf(buscado, i + 1)) {
      cuenta++;
    }
    return cuenta;
  }

  /** R11-18: el nombre va en su propia línea y la marca debajo, en otro elemento. */
  @Test
  void elNombreYLaMarcaVanSeparados() throws Exception {
    when(cuentas.donante(any())).thenReturn(Optional.of(DONANTE));
    when(entidades.entidades(eq(DONANTE), any()))
        .thenReturn(
            vista(List.of(new Entidad("Hogar Ejemplo", true, List.of(frazadas()))), 1, 1, 1));

    mvc.perform(get("/donante-entidades"))
        .andExpect(
            content()
                .string(
                    containsString(
                        "<span data-v=\"nombre\">Hogar Ejemplo</span><span data-v=\"ya-donaste\">Ya le donaste</span>")));
  }

  @Test
  void enLaMaquetaComoLlegaMuestraElDepositoDeEjemplo() throws Exception {
    when(cuentas.donante(any())).thenReturn(Optional.of(DONANTE));
    when(entidades.entidades(eq(DONANTE), any())).thenReturn(vista(List.of(), 0, 1, 1));

    mvc.perform(get("/donante-entidades"))
        .andExpect(content().string(containsString("Depósito Central Ejemplo")));
  }
}
