package grupo5.cliente.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.cliente.maqueta.MaquetaConfig;
import grupo5.cliente.puertos.CuentaActual;
import grupo5.cliente.puertos.DonanteMisionesPuerto;
import grupo5.cliente.vistas.DonanteMisionesVista;
import grupo5.cliente.vistas.DonanteMisionesVista.Categoria;
import grupo5.cliente.vistas.DonanteMisionesVista.Historia;
import grupo5.cliente.vistas.DonanteMisionesVista.Hito;
import grupo5.cliente.vistas.DonanteMisionesVista.Mision;
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
 * Misiones e insignias con lo que da el puerto: sin la historia de cada planta ni el "tu donación
 * más grande", y "Sin permiso" para quien no es una persona.
 */
@WebMvcTest(DonanteMisionesController.class)
@ActiveProfiles("maqueta")
@Import({MaquetaConfig.class, Pantallas.class})
class DonanteMisionesControllerTest {

  private static final UUID DONANTE = UUID.fromString("6f1c2c1e-0000-4000-8000-0000000000aa");

  @MockitoBean private DonanteMisionesPuerto misiones;
  @MockitoBean private CuentaActual cuentas;
  @Autowired private MockMvc mvc;

  private static Mision mision(String categoria, String nombre, String estado) {
    return new Mision(
        categoria, nombre, nombre + " pide algo", estado, null, null, null, null, null, null, null);
  }

  /** Lo que arma el adaptador REST de una persona con una misión completa y otra en curso. */
  private static DonanteMisionesVista conUnaCompleta() {
    return new DonanteMisionesVista(
        true,
        false,
        "Lucía",
        "Colaborador",
        "3 de agosto de 2026",
        1,
        "10 de noviembre de 2026, 14:30",
        List.of(
            new Mision(
                "Colaborador",
                "Racha Inicial",
                "3 meses seguidos donando",
                "completa",
                "9 de octubre de 2026",
                null,
                null,
                null,
                null,
                null,
                null),
            new Mision(
                "Colaborador",
                "Explorador",
                "Donaciones en 3 categorías distintas",
                "curso",
                null,
                1,
                3,
                "1 de 3",
                null,
                null,
                null),
            mision("Sostenedor", "Gran Aporte", "cerrada")),
        List.of(
            new Categoria("Colaborador", "tu categoría", "Sostenedor", null),
            new Categoria("Sostenedor", "se abre después", null, "Colaborador")),
        new Historia(
            "Te sumaste a DonaTrack. Esta planta llega con tu cuenta.",
            List.of(new Hito("Te sumaste a DonaTrack", "3 de agosto de 2026", true)),
            null,
            null,
            null,
            null),
        "Todavía no mostramos X.");
  }

  @Test
  void unaPersonaVeSusMisionesAunqueNoHayaHistoriaDeLasPlantas() throws Exception {
    when(cuentas.donante(any())).thenReturn(Optional.of(DONANTE));
    when(misiones.misiones(eq(DONANTE))).thenReturn(conUnaCompleta());

    mvc.perform(get("/donante-misiones"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Tus misiones e insignias")))
        .andExpect(
            content()
                .string(
                    containsString(
                        "Gracias por donar, Lucía. Sos donante Colaborador desde el 3 de agosto de"
                            + " 2026. Completaste 1 de las 3 misiones. Al 10 de noviembre de 2026,"
                            + " 14:30.")))
        .andExpect(content().string(containsString("Llevás 1 de 3.")))
        .andExpect(content().string(containsString("Todavía no mostramos X.")))
        .andExpect(content().string(containsString("(1 de 3)")))
        .andExpect(content().string(containsString("Completa el 9 de octubre de 2026")))
        .andExpect(content().string(not(containsString("Martina"))))
        .andExpect(content().string(not(containsString("12 de 50"))));
  }

  @Test
  void laPlantaConseguidaSinHistoriaDiceCuandoSeCompleto() throws Exception {
    when(cuentas.donante(any())).thenReturn(Optional.of(DONANTE));
    when(misiones.misiones(eq(DONANTE))).thenReturn(conUnaCompleta());

    mvc.perform(get("/donante-misiones?variante=misiones-b&lugar=estante-izq"))
        .andExpect(status().isOk())
        .andExpect(
            content().string(containsString("Completaste esta misión el 9 de octubre de 2026.")))
        .andExpect(content().string(not(containsString("Ver la donación"))))
        .andExpect(content().string(not(containsString("galletitas"))));
    mvc.perform(get("/donante-misiones?variante=misiones-b&lugar=mesada-izq"))
        .andExpect(content().string(containsString("Esta planta llega con tu cuenta.")))
        .andExpect(content().string(containsString("Ver a quién donar")));
  }

  @Test
  void quienNoEsUnaPersonaVeSinPermiso() throws Exception {
    when(cuentas.donante(any())).thenReturn(Optional.of(DONANTE));
    when(misiones.misiones(eq(DONANTE)))
        .thenReturn(
            new DonanteMisionesVista(
                false, false, null, null, null, 0, "", List.of(), List.of(), null, null));

    mvc.perform(get("/donante-misiones"))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(containsString("Misiones e insignias es para quienes donan como persona.")))
        .andExpect(content().string(not(containsString("Tu huerta"))));
  }

  @Test
  void sinCuentaMandaAIngresar() throws Exception {
    when(cuentas.donante(any())).thenReturn(Optional.empty());

    mvc.perform(get("/donante-misiones")).andExpect(redirectedUrl("/publico-ingresar"));
  }
}
