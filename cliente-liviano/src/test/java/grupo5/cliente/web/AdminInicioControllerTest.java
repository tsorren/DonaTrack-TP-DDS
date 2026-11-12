package grupo5.cliente.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.cliente.puertos.AdminPersonasPuerto;
import grupo5.cliente.puertos.CuentasDelMenu;
import grupo5.cliente.vistas.AdminInicioVista;
import grupo5.cliente.vistas.AdminInicioVista.Camiones;
import grupo5.cliente.vistas.AdminInicioVista.Cifras;
import grupo5.cliente.vistas.AdminInicioVista.Decisiones;
import grupo5.cliente.vistas.AdminInicioVista.Evolucion;
import grupo5.cliente.vistas.AdminInicioVista.Fila;
import grupo5.cliente.vistas.AdminInicioVista.Mes;
import grupo5.cliente.vistas.CuentaVista;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** El inicio del admin, por temas (R11-21), con lo que dan las APIs y no tiene la maqueta. */
@WebMvcTest(AdminInicioController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({Pantallas.class, AdminDonantesControllerTest.Keycloak.class})
@TestPropertySource(
    properties = {
      "CLIENTE_LIVIANO_SECRET=prueba",
      "spring.security.oauth2.client.provider.keycloak.issuer-uri=http://keycloak/realms/donatrack"
    })
class AdminInicioControllerTest {

  private static final Cifras CIFRAS =
      new Cifras(
          1_207,
          1,
          0,
          0,
          2,
          1,
          "en noviembre, del 1 al 10",
          1_284,
          1,
          20_412,
          20_530,
          new Camiones(1, 2));
  private static final Decisiones NADA = new Decisiones(0, List.of(), 0, 0, 0, null, 0);

  @MockitoBean private AdminPersonasPuerto personas;
  @MockitoBean private CuentasDelMenu cuentas;
  @Autowired private MockMvc mvc;

  @BeforeEach
  void armar() {
    when(cuentas.de("admin"))
        .thenReturn(Optional.of(new CuentaVista("Leo", "Administración", null)));
  }

  private static AdminInicioVista inicio(Decisiones decisiones, String resumen, Cifras cifras) {
    return new AdminInicioVista(
        "Martes 10 de noviembre de 2026",
        resumen,
        decisiones,
        "10 de noviembre de 2026, 18:30",
        cifras);
  }

  @Test
  void sinNadaQueDecidirMuestraElVacioYLasCuatroTarjetas() throws Exception {
    when(personas.inicio()).thenReturn(inicio(NADA, null, CIFRAS));

    mvc.perform(get("/admin-inicio"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Martes 10 de noviembre de 2026")))
        .andExpect(content().string(containsString("Hoy no hay nada que espere tu decisión.")))
        .andExpect(content().string(not(containsString("mesa-resumen"))))
        .andExpect(content().string(containsString("Al 10 de noviembre de 2026, 18:30.")))
        .andExpect(content().string(containsString("Donaciones, por estado")))
        .andExpect(content().string(containsString("Entregas confirmadas")))
        .andExpect(content().string(containsString("La red")))
        .andExpect(content().string(containsString("aria-labelledby=\"registro-camiones\"")))
        .andExpect(content().string(not(containsString("null"))));
  }

  @Test
  void cadaCifraLlevaSeparadorDeMilesYSuPeriodo() throws Exception {
    when(personas.inicio()).thenReturn(inicio(NADA, null, CIFRAS));

    mvc.perform(get("/admin-inicio"))
        .andExpect(content().string(containsString(">1.207</dd>")))
        .andExpect(content().string(containsString(">1.284</dd>")))
        .andExpect(content().string(containsString(">20.412</dd>")))
        .andExpect(content().string(containsString("donantes activos, de 20.530 registrados")))
        .andExpect(content().string(containsString("en noviembre, del 1 al 10")))
        .andExpect(content().string(containsString("en total, desde la primera registrada")))
        .andExpect(content().string(containsString("<p>ahora</p>")))
        .andExpect(
            content().string(containsString("aria-label=\"Ver las donaciones en el depósito\"")))
        .andExpect(content().string(containsString("Ver las donaciones vencidas en el depósito")))
        // En camino da 0: se muestra con su 0 y sin «Ver ›».
        .andExpect(content().string(not(containsString("Ver las donaciones en camino"))))
        .andExpect(content().string(containsString("Las confirma cada organización con su foto.")));
  }

  @Test
  void soloMuestraLoQueEsperaConComoMuchoTresNombres() throws Exception {
    when(personas.inicio())
        .thenReturn(
            inicio(
                new Decisiones(
                    1_015,
                    List.of("Hogar Ejemplo", "Club Ejemplo", "Merendero Ejemplo"),
                    1_012,
                    0,
                    0,
                    null,
                    1),
                "Hoy esperan tu decisión 1.015 organizaciones y 1 propuesta de asignación.",
                CIFRAS));

    mvc.perform(get("/admin-inicio"))
        .andExpect(
            content()
                .string(
                    containsString(
                        "Hoy esperan tu decisión 1.015 organizaciones y 1 propuesta de"
                            + " asignación.")))
        .andExpect(content().string(containsString("1.015 piden sumarse")))
        .andExpect(content().string(containsString("Merendero Ejemplo")))
        .andExpect(content().string(containsString("y 1.012 más")))
        .andExpect(content().string(containsString("Revisar organizaciones")))
        .andExpect(content().string(containsString("1 espera tu revisión")))
        .andExpect(content().string(not(containsString("decision-sin-asignar"))))
        .andExpect(content().string(not(containsString("null"))));
  }

  @Test
  void lasDonacionesSinAsignarLlevanALasQueVencenOAlDeposito() throws Exception {
    when(personas.inicio())
        .thenReturn(
            inicio(
                new Decisiones(
                    0, List.of(), 0, 7, 4, "La primera vence el 12 de noviembre, en 2 días.", 0),
                "Hay 7 donaciones sin asignar en el depósito; 4 vencen en los próximos 7 días.",
                CIFRAS));

    mvc.perform(get("/admin-inicio"))
        .andExpect(content().string(containsString("7 en el depósito")))
        .andExpect(content().string(containsString("4 vencen en los próximos 7 días.")))
        .andExpect(content().string(containsString("Ver las que vencen")))
        .andExpect(content().string(containsString("Ver las 7 del depósito")))
        .andExpect(content().string(not(containsString("decision-propuestas"))));

    when(personas.inicio())
        .thenReturn(
            inicio(
                new Decisiones(0, List.of(), 0, 7, 0, null, 0),
                "Hay 7 donaciones sin asignar en el depósito.",
                CIFRAS));
    mvc.perform(get("/admin-inicio"))
        .andExpect(content().string(containsString("Ninguna vence en los próximos 7 días.")))
        .andExpect(content().string(containsString("Ver las del depósito")))
        .andExpect(content().string(not(containsString("Ver las que vencen"))));
  }

  @Test
  void siLogisticaNoRespondioSoloLaTarjetaCamionesMuestraSuAviso() throws Exception {
    Cifras sinCamiones =
        new Cifras(7, 4, 1, 1, 2, 0, "en noviembre, del 1 al 10", 0, 46, 86, 92, null);
    when(personas.inicio()).thenReturn(inicio(NADA, null, sinCamiones));

    mvc.perform(get("/admin-inicio"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("No pudimos cargar los camiones.")))
        .andExpect(content().string(containsString("El resto de las cifras está al día.")))
        .andExpect(content().string(not(containsString("Ver camiones"))))
        .andExpect(content().string(containsString(">46</dd>")))
        .andExpect(content().string(containsString("Todavía no hay entregas confirmadas.")))
        .andExpect(content().string(containsString("Se ve entre las asignadas.")));
  }

  // R11-42: los gráficos, con su tabla al lado, el mes en curso marcado y sus estados
  private static AdminInicioVista conGraficos(Evolucion evolucion) {
    return new AdminInicioVista(
        "Martes 10 de noviembre de 2026",
        null,
        NADA,
        "10 de noviembre de 2026, 18:30",
        CIFRAS,
        evolucion);
  }

  @Test
  void losGraficosVanConSuTablaYElMesEnCursoMarcado() throws Exception {
    Evolucion evolucion =
        new Evolucion(
            "de octubre a noviembre de 2026",
            "Desde octubre de 2026, cuando se registró la primera donación, se registraron 1.230"
                + " donaciones y se confirmaron 3 entregas.",
            "Una entrega cuenta en el mes en que la organización la confirmó: puede ser de una"
                + " donación registrada antes. Noviembre todavía no terminó.",
            2_000,
            1_000,
            List.of(
                new Mes("oct", "2026", "Octubre 2026", false, 1_200, 2, 60, 0),
                new Mes("nov", null, "Noviembre 2026, del 1 al 10", true, 30, 1, 2, 0)),
            new Fila("Total", null, 1_230, 3, 0, 0),
            "Alimentos es la categoría con más donaciones registradas desde octubre de 2026: 1.230"
                + " de 1.230.",
            List.of(
                new Fila("Alimentos", "cat-alimentos", 1_230, 3, 100, 0),
                new Fila("Otras 3 categorías", "cat-generico", 0, 0, 0, 0)),
            new Fila("Total", null, 1_230, 3, 0, 0));
    when(personas.inicio()).thenReturn(conGraficos(evolucion));

    mvc.perform(get("/admin-inicio"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Mes a mes")))
        .andExpect(content().string(containsString("Por categoría")))
        .andExpect(content().string(containsString("de octubre a noviembre de 2026")))
        .andExpect(content().string(containsString("se registraron 1.230")))
        .andExpect(content().string(containsString("Noviembre 2026, del 1 al 10")))
        .andExpect(content().string(containsString("en curso")))
        .andExpect(content().string(containsString("data-curso=\"si\"")))
        .andExpect(content().string(containsString("viewBox=\"0 0 20 100\"")))
        .andExpect(content().string(containsString("y=\"40\"")))
        .andExpect(
            content()
                .string(
                    containsString(
                        "<caption>Donaciones registradas y entregas confirmadas, mes a"
                            + " mes</caption>")))
        .andExpect(content().string(containsString("<th scope=\"row\">Otras 3 categorías</th>")))
        .andExpect(content().string(containsString("aria-hidden=\"true\"")))
        .andExpect(content().string(containsString("Noviembre todavía no terminó.")))
        .andExpect(content().string(containsString("href=\"#cat-alimentos\"")))
        .andExpect(content().string(not(containsString("No pudimos cargar los gráficos."))));
  }

  @Test
  void sinDonacionesLaTarjetaLoDiceSinDibujo() throws Exception {
    when(personas.inicio()).thenReturn(conGraficos(Evolucion.VACIA));

    mvc.perform(get("/admin-inicio"))
        .andExpect(content().string(containsString("Todavía no hay donaciones registradas.")))
        .andExpect(
            content()
                .string(containsString("Los gráficos aparecen cuando se registre la primera.")))
        .andExpect(content().string(not(containsString("Por categoría"))))
        .andExpect(content().string(not(containsString("<caption>"))));
  }

  @Test
  void siLaEvolucionNoLlegoSoloSuTarjetaMuestraElAviso() throws Exception {
    when(personas.inicio()).thenReturn(conGraficos(null));

    mvc.perform(get("/admin-inicio"))
        .andExpect(content().string(containsString("Los últimos 12 meses")))
        .andExpect(content().string(containsString("No pudimos cargar los gráficos.")))
        .andExpect(content().string(containsString("Las cifras de arriba están al día.")))
        .andExpect(content().string(containsString(">20.412</dd>")))
        .andExpect(content().string(not(containsString("Mes a mes"))));
  }
}
