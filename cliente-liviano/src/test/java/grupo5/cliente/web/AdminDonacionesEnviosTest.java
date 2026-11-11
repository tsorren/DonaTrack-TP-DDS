package grupo5.cliente.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.cliente.puertos.AdminOperacionPuerto;
import grupo5.cliente.vistas.AdminDonacionesVista;
import grupo5.cliente.vistas.AdminDonacionesVista.Donacion;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.HttpClientErrorException;

/**
 * Donaciones fuera de la maqueta: el detalle de una donación que está (y el 404 de una que no),
 * marcarla vencida con el nombre del admin y el 409 si cambió a la vez.
 */
@WebMvcTest({AdminDonacionesController.class, AdminDonacionesEnvios.class})
@AutoConfigureMockMvc(addFilters = false)
@Import({
  Pantallas.class,
  AdminCamionesEnviosTest.Cuenta.class,
  AdminDonacionesEnviosTest.Reloj.class
})
class AdminDonacionesEnviosTest {

  private static final String ID = "6f1c2c1e-0000-4000-8000-0000000000d1";

  @TestConfiguration
  static class Reloj {

    @Bean
    Clock reloj() {
      return Clock.fixed(Instant.parse("2026-11-10T21:30:00Z"), ZoneOffset.UTC);
    }
  }

  @MockitoBean private AdminOperacionPuerto operacion;
  @Autowired private MockMvc mvc;

  @BeforeEach
  void unaDonacion() {
    when(operacion.donaciones(""))
        .thenReturn(
            new AdminDonacionesVista(
                List.of(
                    new Donacion(
                        ID,
                        "Leche en polvo",
                        "10 unidades",
                        "10 u",
                        "Ana Ejemplo",
                        "8 de noviembre",
                        "deposito",
                        "12 de noviembre, en 2 días",
                        true,
                        "Nuevo",
                        null,
                        null,
                        false,
                        null,
                        List.of("8 de noviembre: llegó al depósito."))),
                Map.of(),
                "1 en el depósito."));
  }

  @Test
  void elDetalleMuestraLaDonacionYUnaQueNoEstaEsUn404() throws Exception {
    mvc.perform(get("/admin-donaciones-detalle?donacion=" + ID))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("8 de noviembre: llegó al depósito.")))
        .andExpect(content().string(containsString("Marcar como vencida")));
    mvc.perform(get("/admin-donaciones-detalle?donacion=otra")).andExpect(status().isNotFound());
    mvc.perform(get("/admin-donaciones-detalle")).andExpect(status().isNotFound());
    mvc.perform(get("/admin-donaciones-detalle-vencida")).andExpect(status().isNotFound());
  }

  @Test
  void marcarVencidaGuardaConElNombreDelAdminYVuelveAlDetalle() throws Exception {
    mvc.perform(
            post("/admin-donaciones-detalle-vencida")
                .param("donacion", ID)
                .param("motivo", "Se rompió el envase"))
        .andExpect(redirectedUrl("/admin-donaciones-detalle?donacion=" + ID))
        .andExpect(
            flash()
                .attribute(
                    Envios.AVISO, "La marcaste como vencida el 10 de noviembre de 2026, 18:30."));
    verify(operacion).marcarVencida(ID, "Leo Ejemplo", "Se rompió el envase");
  }

  @Test
  void siCambioALaVezNoLaMarca() throws Exception {
    doThrow(HttpClientErrorException.create(HttpStatus.CONFLICT, "Conflict", null, null, null))
        .when(operacion)
        .marcarVencida(ID, "Leo Ejemplo", null);

    mvc.perform(post("/admin-donaciones-detalle-vencida").param("donacion", ID))
        .andExpect(redirectedUrl("/admin-donaciones-detalle?donacion=" + ID))
        .andExpect(
            flash()
                .attribute(
                    Envios.AVISO,
                    "Esta donación cambió mientras la mirabas: no la marcamos como vencida."
                        + " Volvimos a cargar su estado."));
  }
}
