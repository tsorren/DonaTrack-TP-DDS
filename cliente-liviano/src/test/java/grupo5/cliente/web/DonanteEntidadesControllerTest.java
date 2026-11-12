package grupo5.cliente.web;

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
        .andExpect(content().string(containsString("7 entidades activas. Página 2 de 2.")))
        .andExpect(content().string(containsString("Piden 20 unidades · Hasta el 15 de noviembre")))
        .andExpect(content().string(containsString("Actualizado\n        hoy, 7 de noviembre")))
        .andExpect(content().string(not(containsString("<progress"))))
        .andExpect(content().string(not(containsString("solo está dibujada"))))
        // B7: el componente paginas, no el nav propio
        .andExpect(content().string(containsString("aria-label=\"Páginas de entidades\"")))
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
        .andExpect(content().string(containsString("Por ahora no hay entidades activas.")))
        .andExpect(content().string(not(containsString("entidades activas. Página"))))
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
                    containsString("14 entidades coinciden con lo que elegiste. Página 2 de 3.")))
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
        .andExpect(content().string(containsString("Todavía no le donaste a ninguna entidad.")))
        .andExpect(
            content()
                .string(
                    containsString(
                        "Cuando el depósito asigne una de tus donaciones, la entidad aparece acá.")))
        .andExpect(content().string(containsString("Ver todas")));
  }

  @Test
  void conCategoriaYNingunaSugiereSacarLosFiltros() throws Exception {
    when(cuentas.donante(any())).thenReturn(Optional.of(DONANTE));
    when(entidades.entidades(eq(DONANTE), any())).thenReturn(vista(List.of(), 0, 1, 1));

    mvc.perform(get("/donante-entidades?categoria=ropa&ya=si"))
        .andExpect(
            content().string(containsString("Ninguna entidad coincide con lo que elegiste.")))
        .andExpect(
            content().string(containsString("Probá con otra categoría o sacá los filtros.")));
  }

  /**
   * B2 y B3: las categorías del filtro y los grupos son los de la vista (Higiene también); la
   * categoría elegida muestra solo su grupo y una entidad sin nada pendiente de ella lo dice.
   */
  @Test
  void lasCategoriasRealesFiltranYAgrupan() throws Exception {
    when(cuentas.donante(any())).thenReturn(Optional.of(DONANTE));
    Pedido jabon =
        new Pedido(
            "Jabón", HIGIENE, "cat-higiene", "Piden 30 unidades", "Sin fecha de cierre", null, 30);
    when(entidades.entidades(eq(DONANTE), any()))
        .thenReturn(
            vista(
                List.of(
                    new Entidad("Hogar Ejemplo", false, List.of(jabon, frazadas())),
                    new Entidad("Club Ejemplo", false, List.of(frazadas()))),
                5,
                1,
                1));

    mvc.perform(get("/donante-entidades"))
        .andExpect(content().string(containsString("id=\"grupo-2\"")))
        .andExpect(content().string(containsString("Jabón")))
        .andExpect(content().string(containsString("Todas las categorías")));
    mvc.perform(get("/donante-entidades?categoria=" + HIGIENE))
        .andExpect(
            content()
                .string(
                    containsString("5 entidades coinciden con lo que elegiste. Página 1 de 1.")))
        .andExpect(content().string(not(containsString("id=\"grupo-3\""))))
        .andExpect(content().string(containsString("Lo que pedía de Higiene ya está cubierto.")));
    verify(entidades).entidades(DONANTE, new Consulta("", HIGIENE, false, 1));
  }

  /** I6: un pedido sin categoría conocida no se pierde. */
  @Test
  void losPedidosSinCategoriaVanEnOtros() throws Exception {
    when(cuentas.donante(any())).thenReturn(Optional.of(DONANTE));
    Pedido suelto = new Pedido("Pañales", null, null, "Piden 10", "Sin fecha de cierre", null, 10);
    when(entidades.entidades(eq(DONANTE), any()))
        .thenReturn(vista(List.of(new Entidad("Hogar Ejemplo", false, List.of(suelto))), 1, 1, 1));

    mvc.perform(get("/donante-entidades"))
        .andExpect(content().string(containsString("Otros pedidos")))
        .andExpect(content().string(containsString("Pañales")));
  }

  /**
   * B1 e I2: una entidad que llega sin pedidos vigentes (con "Ya doné", una que ya tiene todo
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

    mvc.perform(get("/donante-entidades?ya=si"))
        .andExpect(
            content()
                .string(
                    containsString("3 entidades coinciden con lo que elegiste. Página 1 de 1.")))
        .andExpect(content().string(containsString("Sin pedidos pendientes")))
        .andExpect(
            content()
                .string(
                    containsString(
                        "<span data-v=\"nombre\">Comedor Ejemplo Este</span><span data-v=\"ya-donaste\">Ya le donaste</span>")))
        .andExpect(content().string(containsString("Hoy no pide nada.")));
  }

  /** Con una categoría elegida, la entidad sin pedidos sale en su grupo y no se repite. */
  @Test
  void conCategoriaLaEntidadSinPedidosNoSeRepite() throws Exception {
    when(cuentas.donante(any())).thenReturn(Optional.of(DONANTE));
    when(entidades.entidades(eq(DONANTE), any()))
        .thenReturn(vista(List.of(new Entidad("Comedor Ejemplo Este", true, List.of())), 1, 1, 1));

    mvc.perform(get("/donante-entidades?categoria=ropa"))
        .andExpect(content().string(containsString("Lo que pedía de Ropa ya está cubierto.")))
        .andExpect(content().string(not(containsString("Sin pedidos pendientes"))));
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
