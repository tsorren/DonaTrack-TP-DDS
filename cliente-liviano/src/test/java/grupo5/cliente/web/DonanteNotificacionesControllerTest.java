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
import grupo5.cliente.puertos.DonanteNotificacionesPuerto;
import grupo5.cliente.vistas.DonanteNotificacionesVista;
import grupo5.cliente.vistas.DonanteNotificacionesVista.Aviso;
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

/** Las notificaciones del donante con la bandeja que da el puerto. */
@WebMvcTest(controllers = {DonanteNotificacionesController.class, PantallasController.class})
@ActiveProfiles("maqueta")
@Import({MaquetaConfig.class, Pantallas.class})
class DonanteNotificacionesControllerTest {

  private static final UUID DONANTE = UUID.fromString("6f1c2c1e-0000-4000-8000-0000000000aa");

  @MockitoBean private DonanteNotificacionesPuerto notificaciones;
  @MockitoBean private CuentaActual cuentas;
  @Autowired private MockMvc mvc;

  @Test
  void muestraLaBandejaConSusCategoriasYLasSinLeer() throws Exception {
    when(cuentas.donante(any())).thenReturn(Optional.of(DONANTE));
    when(notificaciones.bandeja(DONANTE))
        .thenReturn(
            new DonanteNotificacionesVista(
                List.of(
                    new Aviso(
                        "b1",
                        "2026-11-07T18:00",
                        "¡Felicitaciones! Has ascendido.",
                        "otras",
                        "/donante-donaciones",
                        false),
                    new Aviso(
                        "b2",
                        "2026-11-06T10:00",
                        "Tu donación de Fideos fue asignada a Comedor",
                        "asignadas",
                        "/donante-donaciones",
                        true)),
                "2026-11-07T19:40"));

    mvc.perform(get("/donante-notificaciones"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("1 sin leer.")))
        .andExpect(content().string(containsString("¡Felicitaciones! Has ascendido.")))
        .andExpect(content().string(containsString("Otras novedades")))
        .andExpect(content().string(containsString("Donaciones asignadas")))
        // El menú de Categoría muestra solo las que tienen avisos.
        .andExpect(content().string(not(containsString("Problemas en la entrega"))));
  }

  @Test
  void sinCuentaMandaAIngresar() throws Exception {
    when(cuentas.donante(any())).thenReturn(Optional.empty());

    mvc.perform(get("/donante-notificaciones")).andExpect(redirectedUrl("/publico-ingresar"));
  }
}
