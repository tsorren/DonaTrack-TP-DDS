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

/**
 * Seguimiento de una donación (arquetipo A07, DA-03a) con lo que da el puerto: la pedida o la
 * primera en camino, en el mapa, con su recorrido.
 */
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

    // DA-03a: una sola donación, a la que se llega desde su detalle (las migas vuelven a él).
    mvc.perform(get("/donante-seguimiento"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("<h1>Silla plegable, 1 unidad</h1>")))
        .andExpect(
            content().string(containsString("href=\"/donante-donaciones-detalle?donacion=s1\"")))
        .andExpect(
            content()
                .string(
                    containsString(
                        "Posición aproximada, actualizada hoy, 7 de noviembre, a las 19:40.")))
        .andExpect(content().string(containsString("data-lat=\"-34.6\"")))
        .andExpect(content().string(containsString("Para Hogar Ejemplo")))
        // HG-02: el texto de «Si no llega», con la hora de salida, como en el inicio.
        .andExpect(
            content()
                .string(
                    containsString(
                        "En camino desde el 7 de noviembre, 10:00. Posición aproximada,")))
        .andExpect(content().string(not(containsString("Salió el 7 de noviembre"))))
        // HG-02: la hora estimada no llega del backend.
        .andExpect(content().string(not(containsString("Hora estimada"))));
  }

  /** DA-03a: con ?donacion=, la pedida; una que no va en curso lo dice, sin mostrar otra. */
  @Test
  void muestraLaDonacionPedidaYAvisaSiNoVaEnCurso() throws Exception {
    when(cuentas.donante(any())).thenReturn(Optional.of(DONANTE));
    when(seguimientos.seguimiento(DONANTE))
        .thenReturn(
            new DonanteSeguimientoVista(
                List.of(
                    donacion("s1", "Silla plegable", "Hogar Ejemplo"),
                    donacion("m1", "Mesa ratona", "Comedor Ejemplo")),
                Map.of("m1", new Punto(-34.61, -58.41, -34.7, -58.5)),
                "hoy, 7 de noviembre, a las 19:40"));

    mvc.perform(get("/donante-seguimiento?donacion=m1"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("<h1>Mesa ratona, 1 unidad</h1>")))
        .andExpect(content().string(containsString("data-lat=\"-34.61\"")))
        .andExpect(content().string(not(containsString("Silla plegable, 1 unidad"))));

    mvc.perform(get("/donante-seguimiento?donacion=otra"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("No encontramos esa donación en camino.")))
        .andExpect(content().string(not(containsString("data-mapa"))));
  }

  /** R11-39: lo que espera destino es un aviso con ícono, título y lista, no un párrafo suelto. */
  @Test
  void loQueEsperaDestinoVaEnUnAvisoConIconoYEstructura() throws Exception {
    when(cuentas.donante(any())).thenReturn(Optional.of(DONANTE));
    DonanteDonacionVista silla = donacion("s1", "Silla plegable", "Hogar Ejemplo");
    DonanteDonacionVista arroz = donacion("a1", "Arroz largo fino", null);
    DonanteDonacionVista fideos = donacion("f1", "Fideos secos", null);
    when(seguimientos.seguimiento(DONANTE))
        .thenReturn(
            new DonanteSeguimientoVista(
                List.of(silla, arroz, fideos),
                Map.of("s1", new Punto(-34.6, -58.4, -34.7, -58.5)),
                "hoy, 7 de noviembre, a las 19:40"));

    mvc.perform(get("/donante-seguimiento"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("data-v=\"sin-destino\"")))
        .andExpect(content().string(containsString("aria-labelledby=\"sin-destino-titulo\"")))
        .andExpect(content().string(containsString("<h3 id=\"sin-destino-titulo\"")))
        .andExpect(content().string(containsString("Todavía esperan destino")))
        .andExpect(content().string(containsString("<li>Arroz largo fino</li>")))
        .andExpect(content().string(containsString("<li>Fideos secos</li>")))
        .andExpect(
            content().string(containsString("Las vas a poder seguir acá cuando se les asigne una")))
        .andExpect(content().string(not(containsString("seguimiento-pie"))));
  }

  @Test
  void conUnaSolaSinDestinoElAvisoVaEnSingular() throws Exception {
    when(cuentas.donante(any())).thenReturn(Optional.of(DONANTE));
    when(seguimientos.seguimiento(DONANTE))
        .thenReturn(
            new DonanteSeguimientoVista(
                List.of(
                    donacion("s1", "Silla plegable", "Hogar Ejemplo"),
                    donacion("a1", "Arroz largo fino", null)),
                Map.of("s1", new Punto(-34.6, -58.4, -34.7, -58.5)),
                "hoy, 7 de noviembre, a las 19:40"));

    mvc.perform(get("/donante-seguimiento"))
        .andExpect(content().string(containsString("Todavía espera destino")))
        .andExpect(
            content().string(containsString("La vas a poder seguir acá cuando se le asigne una")));
  }

  @Test
  void sinNadaSinDestinoNoHayAviso() throws Exception {
    when(cuentas.donante(any())).thenReturn(Optional.of(DONANTE));
    when(seguimientos.seguimiento(DONANTE))
        .thenReturn(
            new DonanteSeguimientoVista(
                List.of(donacion("s1", "Silla plegable", "Hogar Ejemplo")),
                Map.of("s1", new Punto(-34.6, -58.4, -34.7, -58.5)),
                "hoy, 7 de noviembre, a las 19:40"));

    mvc.perform(get("/donante-seguimiento"))
        .andExpect(content().string(not(containsString("data-v=\"sin-destino\""))));
  }

  private static DonanteDonacionVista donacion(String clave, String titulo, String destino) {
    boolean conDestino = destino != null;
    return new DonanteDonacionVista(
        clave,
        1,
        titulo,
        "1 unidad",
        null,
        "Mobiliario",
        destino,
        "1 de noviembre de 2026",
        null,
        conDestino ? EstadoEntrega.EN_CAMINO : EstadoEntrega.PENDIENTE,
        null,
        null,
        null,
        "Posición aproximada, actualizada hoy a las 19:30.",
        List.of(new PasoRecorrido("En camino", "7 de noviembre", Avance.ACTUAL)),
        List.of());
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
        .andExpect(content().string(not(containsString("aria-labelledby=\"recorrido-titulo\""))))
        .andExpect(content().string(not(containsString("data-mapa"))));
  }
}
