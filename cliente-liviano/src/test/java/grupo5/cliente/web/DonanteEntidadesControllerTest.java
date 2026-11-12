package grupo5.cliente.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.cliente.maqueta.MaquetaConfig;
import grupo5.cliente.puertos.CuentaActual;
import grupo5.cliente.puertos.DonanteEntidadesPuerto;
import grupo5.cliente.vistas.DonanteEntidadesVista;
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

/** Entidades del donante con una página de la API: sin lo recibido, el pedido va sin barra. */
@WebMvcTest(controllers = {DonanteEntidadesController.class, PantallasController.class})
@ActiveProfiles("maqueta")
@Import({MaquetaConfig.class, Pantallas.class})
class DonanteEntidadesControllerTest {

  private static final UUID DONANTE = UUID.fromString("6f1c2c1e-0000-4000-8000-0000000000aa");

  @MockitoBean private DonanteEntidadesPuerto entidades;
  @MockitoBean private CuentaActual cuentas;
  @Autowired private MockMvc mvc;

  @Test
  void muestraLaPaginaPedidaSinBarraCuandoNoHayLoRecibido() throws Exception {
    when(cuentas.donante(any())).thenReturn(Optional.of(DONANTE));
    when(entidades.entidades(eq(DONANTE), anyString(), anyInt()))
        .thenReturn(
            new DonanteEntidadesVista(
                List.of(
                    new Entidad(
                        "Hogar Ejemplo",
                        List.of(),
                        List.of(
                            new Pedido(
                                "Frazadas",
                                "cat-ropa",
                                "Piden 20 unidades",
                                "Hasta el 15 de noviembre",
                                null,
                                20)))),
                7,
                2,
                2,
                "hoy, 7 de noviembre, 19:40"));

    mvc.perform(get("/donante-entidades?pagina=2"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("7 entidades activas. Página 2 de 2.")))
        .andExpect(content().string(containsString("Piden 20 unidades · Hasta el 15 de noviembre")))
        .andExpect(content().string(containsString("Actualizado\n        hoy, 7 de noviembre")))
        .andExpect(content().string(not(containsString("<progress"))))
        .andExpect(content().string(not(containsString("solo está dibujada"))))
        .andExpect(content().string(containsString("Página anterior")));
  }

  @Test
  void sinCuentaMandaAIngresar() throws Exception {
    when(cuentas.donante(any())).thenReturn(Optional.empty());

    mvc.perform(get("/donante-entidades")).andExpect(redirectedUrl("/publico-ingresar"));
  }

  @Test
  void sinEntidadesActivasMuestraElEstadoVacio() throws Exception {
    when(cuentas.donante(any())).thenReturn(Optional.of(DONANTE));
    when(entidades.entidades(eq(DONANTE), anyString(), anyInt()))
        .thenReturn(new DonanteEntidadesVista(List.of(), 0, 1, 1, "hoy, 7 de noviembre, 19:40"));

    mvc.perform(get("/donante-entidades"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Por ahora no hay entidades activas.")))
        .andExpect(content().string(not(containsString("entidades activas. Página"))));
  }

  @Test
  void yaDoneDiceQueFiltraLaPaginaYElPaginadorLoConserva() throws Exception {
    when(cuentas.donante(any())).thenReturn(Optional.of(DONANTE));
    when(entidades.entidades(eq(DONANTE), anyString(), anyInt()))
        .thenReturn(
            new DonanteEntidadesVista(
                List.of(
                    new Entidad("Hogar Ejemplo", List.of("d1"), List.of(frazadas())),
                    new Entidad("Club Ejemplo", List.of(), List.of(frazadas()))),
                14,
                3,
                3,
                "hoy, 7 de noviembre, 19:40"));

    mvc.perform(get("/donante-entidades?ya=si&pagina=2"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Ya doné")))
        .andExpect(
            content().string(containsString("En esta página, 1 entidad a la que ya donaste.")))
        .andExpect(content().string(not(containsString("Club Ejemplo"))))
        .andExpect(content().string(containsString("/donante-entidades?pagina=3&amp;ya=si")))
        .andExpect(content().string(containsString("/donante-entidades?pagina=1&amp;ya=si")));
  }

  @Test
  void enLaMaquetaComoLlegaMuestraElDepositoDeEjemplo() throws Exception {
    when(cuentas.donante(any())).thenReturn(Optional.of(DONANTE));
    when(entidades.entidades(eq(DONANTE), anyString(), anyInt()))
        .thenReturn(new DonanteEntidadesVista(List.of(), 0, 1, 1, "hoy, 7 de noviembre, 19:40"));

    mvc.perform(get("/donante-entidades"))
        .andExpect(content().string(containsString("Depósito Central Ejemplo")));
  }

  private static Pedido frazadas() {
    return new Pedido(
        "Frazadas", "cat-ropa", "Piden 20 unidades", "Hasta el 15 de noviembre", null, 20);
  }
}
