package grupo5.cliente.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.cliente.maqueta.MaquetaConfig;
import grupo5.cliente.puertos.CuentaActual;
import grupo5.cliente.puertos.DonanteSeguimientoPuerto;
import grupo5.cliente.vistas.DonanteDonacionVista;
import grupo5.cliente.vistas.DonanteSeguimientoVista;
import grupo5.cliente.vistas.DonanteSeguimientoVista.Punto;
import grupo5.cliente.vistas.EstadoEntrega;
import grupo5.cliente.vistas.PasoRecorrido;
import grupo5.cliente.vistas.PasoRecorrido.Avance;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Seguimiento del donante con lo que da el puerto: una entrega en camino, en el mapa. */
@WebMvcTest(controllers = {DonanteSeguimientoController.class, PantallasController.class})
@ActiveProfiles("maqueta")
@Import({MaquetaConfig.class, Pantallas.class})
class DonanteSeguimientoControllerTest {

  private static final UUID DONANTE = UUID.fromString("6f1c2c1e-0000-4000-8000-0000000000aa");

  @MockitoBean private DonanteSeguimientoPuerto seguimientos;
  @MockitoBean private CuentaActual cuentas;
  @Autowired private MockMvc mvc;

  @Test
  void muestraLaEntregaEnCaminoConSuPuntoEnElMapa() throws Exception {
    when(cuentas.donante(any())).thenReturn(Optional.of(DONANTE));
    DonanteDonacionVista silla =
        new DonanteDonacionVista(
            "s1",
            1,
            "Silla plegable",
            "1 unidad",
            null,
            "Mobiliario",
            "Hogar Ejemplo",
            "1 de noviembre de 2026",
            null,
            EstadoEntrega.EN_CAMINO,
            "Salió el 7 de noviembre, 10:00",
            null,
            null,
            "Posición aproximada, actualizada hoy a las 19:30.",
            List.of(new PasoRecorrido("En camino", "7 de noviembre", Avance.ACTUAL)),
            List.of());
    when(seguimientos.seguimiento(DONANTE))
        .thenReturn(
            new DonanteSeguimientoVista(
                List.of(silla),
                Map.of("s1", new Punto(-34.6, -58.4, -34.7, -58.5)),
                "hoy, 7 de noviembre, a las 19:40"));

    mvc.perform(get("/donante-seguimiento"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("1 entrega en curso.")))
        .andExpect(
            content().string(containsString("actualizadas hoy, 7 de noviembre, a las 19:40.")))
        .andExpect(content().string(containsString("data-lat=\"-34.6\"")))
        .andExpect(content().string(containsString("Para Hogar Ejemplo")));
  }

  @Test
  void sinCuentaMandaAIngresar() throws Exception {
    when(cuentas.donante(any())).thenReturn(Optional.empty());

    mvc.perform(get("/donante-seguimiento")).andExpect(redirectedUrl("/publico-ingresar"));
  }

  @Test
  void sinEntregasEnCursoMuestraElEstadoVacio() throws Exception {
    when(cuentas.donante(any())).thenReturn(Optional.of(DONANTE));
    when(seguimientos.seguimiento(DONANTE))
        .thenReturn(
            new DonanteSeguimientoVista(List.of(), Map.of(), "hoy, 7 de noviembre, a las 19:40"));

    mvc.perform(get("/donante-seguimiento"))
        .andExpect(status().isOk())
        .andExpect(
            content().string(containsString("Ningún camión está en camino con tus donaciones")))
        .andExpect(content().string(not(containsString("aria-labelledby=\"lista-titulo\""))));
  }
}
