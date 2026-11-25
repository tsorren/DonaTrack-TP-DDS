package grupo5.cliente.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.cliente.puertos.AdminOperacionPuerto;
import grupo5.cliente.puertos.AdminPersonasPuerto;
import grupo5.cliente.puertos.CuentasDelMenu;
import grupo5.cliente.vistas.AdminAsignacionesVista;
import grupo5.cliente.vistas.AdminAsignacionesVista.Cuentas;
import grupo5.cliente.vistas.AdminAsignacionesVista.Propuesta;
import grupo5.cliente.vistas.AdminAsignacionesVista.Sugerencia;
import grupo5.cliente.vistas.AdminInicioVista;
import grupo5.cliente.vistas.AdminInicioVista.Camiones;
import grupo5.cliente.vistas.AdminInicioVista.Cifras;
import grupo5.cliente.vistas.AdminInicioVista.Decisiones;
import grupo5.cliente.vistas.AdminInicioVista.Evolucion;
import grupo5.cliente.vistas.AdminInicioVista.Fila;
import grupo5.cliente.vistas.AdminInicioVista.Mes;
import grupo5.cliente.vistas.ConsultaAsignaciones;
import grupo5.cliente.vistas.CuentaVista;
import grupo5.cliente.vistas.Tramo;
import java.nio.charset.StandardCharsets;
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
import org.springframework.web.client.ResourceAccessException;

/**
 * El inicio del admin, «Por decidir» (A03, DA-04 y DA-25): la cabecera y las pestañas de la mesa,
 * la primera página de las propuestas sin problemas en la tabla con selección y, debajo, «Cómo está
 * todo» como un solo resumen (DA-29) y los gráficos (R11-42, DA-30), con lo que dan las APIs y no
 * tiene la maqueta.
 */
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
  private static final Decisiones TRES_Y_CUATRO = new Decisiones(3, List.of(), 0, 7, 4, null, 9);

  @MockitoBean private AdminPersonasPuerto personas;
  @MockitoBean private AdminOperacionPuerto operacion;
  @MockitoBean private CuentasDelMenu cuentas;
  @Autowired private MockMvc mvc;

  @BeforeEach
  void armar() {
    when(cuentas.de("admin"))
        .thenReturn(Optional.of(new CuentaVista("Leo", "Administración", null)));
    when(operacion.asignaciones(any(), eq(""))).thenReturn(asignaciones(2, 1, 1));
  }

  private static AdminInicioVista inicio(Decisiones decisiones, String resumen, Cifras cifras) {
    return new AdminInicioVista(
        "Martes 10 de noviembre de 2026",
        resumen,
        decisiones,
        "10 de noviembre de 2026, 18:30",
        cifras);
  }

  private static Propuesta propuesta(String clave, String cubre) {
    return new Propuesta(
        clave,
        "Comedor Barrio Ejemplo",
        "Lentejas, 15 kg",
        "15 de octubre",
        cubre,
        "Cubre la necesidad completa.",
        false,
        List.of(new Sugerencia("Quienes menos recibieron", "1.º de 6")),
        null,
        "libre",
        null,
        null);
  }

  /** La pestaña Propuestas: {@code total} sin problemas (como mucho 2 en la página). */
  private static AdminAsignacionesVista asignaciones(long total, long enConflicto, int paginas) {
    List<Propuesta> filas =
        total == 0
            ? List.of()
            : List.of(
                propuesta("lentejas", "Lentejas · 15 kg (de Juan Ejemplo)"),
                propuesta("arroz", "Arroz · 25 kg (de Ana Ejemplo)"));
    return new AdminAsignacionesVista(
        ConsultaAsignaciones.primera(),
        new Cuentas(total + enConflicto, total, enConflicto),
        "hoy, 0:00 · generó 7 propuestas",
        new Tramo<>(filas, total, 1, paginas),
        List.of(),
        null,
        List.of(),
        null,
        null);
  }

  private String html(String ruta) throws Exception {
    return mvc.perform(get(ruta))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString(StandardCharsets.UTF_8);
  }

  @Test
  void esPorDecidirConLaCabeceraLasCuatroPestanasYLaTablaDePropuestas() throws Exception {
    when(personas.inicio()).thenReturn(inicio(TRES_Y_CUATRO, null, CIFRAS));

    mvc.perform(get("/admin-inicio"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("<title>Por decidir - DonaTrack</title>")))
        .andExpect(content().string(matchesPattern("(?s).*<h1[^>]*>Por decidir</h1>.*")))
        .andExpect(content().string(containsString("Martes 10 de noviembre de 2026.")))
        .andExpect(content().string(containsString("Última asignación: hoy, 0:00 · generó 7")))
        .andExpect(
            content()
                .string(
                    matchesPattern(
                        "(?s).*<a href=\"/admin-inicio\"[^>]*aria-current=\"page\">Propuestas"
                            + " <b>2</b></a>.*")))
        .andExpect(
            content()
                .string(
                    matchesPattern(
                        "(?s).*<a href=\"/admin-asignaciones\\?pestana=conflictos\"[^>]*>Conflictos"
                            + " <b>1</b></a>.*")))
        .andExpect(
            content()
                .string(
                    matchesPattern(
                        "(?s).*<a href=\"/admin-entidades\"[^>]*>Solicitudes <b>3</b></a>.*")))
        .andExpect(
            content()
                .string(
                    matchesPattern(
                        "(?s).*<a href=\"/admin-donaciones\\?vence=pronto\"[^>]*>Por vencer"
                            + " <b>4</b></a>.*")))
        .andExpect(content().string(containsString("Propuestas sin problemas · 2")))
        .andExpect(content().string(containsString("action=\"/admin-asignaciones-aprobada\"")))
        .andExpect(content().string(containsString("name=\"pestana\" value=\"inicio\"")))
        .andExpect(content().string(containsString("name=\"propuesta\" value=\"lentejas\"")))
        .andExpect(content().string(containsString("name=\"propuesta\" value=\"arroz\"")))
        .andExpect(content().string(containsString("Lentejas · 15 kg (de Juan Ejemplo)")))
        .andExpect(
            content().string(containsString("Mostrando 1 a 2 de 2 propuestas sin problemas.")))
        // Una sola página: no hace falta ir a la lista.
        .andExpect(content().string(not(containsString("Ver las 2 propuestas"))))
        .andExpect(content().string(not(containsString("Hola, Leo"))))
        .andExpect(content().string(not(containsString("Lo que espera tu decisión"))))
        .andExpect(content().string(not(containsString("tarjeta decision"))))
        .andExpect(content().string(not(containsString("null"))));
    // La única acción llena de la pantalla es «Aprobar» de la tabla.
    assertThat(html("/admin-inicio").split("data-tono=\"principal\"", -1)).hasSize(2);
    verify(operacion, org.mockito.Mockito.atLeastOnce())
        .asignaciones(
            argThat(c -> ConsultaAsignaciones.PROPUESTAS.equals(c.pestana()) && c.pagina() == 1),
            eq(""));
  }

  @Test
  void conMasDeUnaPaginaLlevaALaListaDePropuestas() throws Exception {
    when(personas.inicio()).thenReturn(inicio(NADA, null, CIFRAS));
    when(operacion.asignaciones(any(), eq(""))).thenReturn(asignaciones(1_234, 0, 124));

    mvc.perform(get("/admin-inicio"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Propuestas sin problemas · 1.234")))
        .andExpect(
            content()
                .string(
                    matchesPattern(
                        "(?s).*<a href=\"/admin-asignaciones\"[^>]*>Ver las 1.234"
                            + " propuestas</a>.*")));
  }

  @Test
  void sinNadaQueDecidirLaPestanaMuestraSuVacioYElRestoSigue() throws Exception {
    when(personas.inicio()).thenReturn(inicio(NADA, null, CIFRAS));
    when(operacion.asignaciones(any(), eq(""))).thenReturn(asignaciones(0, 0, 0));

    mvc.perform(get("/admin-inicio"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("No hay propuestas que esperen tu decisión.")))
        .andExpect(content().string(not(containsString("name=\"propuesta\""))))
        .andExpect(content().string(containsString("Al 10 de noviembre de 2026, 18:30.")))
        .andExpect(content().string(containsString("Cómo está todo")))
        .andExpect(content().string(not(containsString("null"))));
  }

  @Test
  void siLasPropuestasNoLlegaronLaPestanaMuestraSuAvisoYElRestoSigue() throws Exception {
    when(personas.inicio()).thenReturn(inicio(TRES_Y_CUATRO, null, CIFRAS));
    when(operacion.asignaciones(any(), eq("")))
        .thenThrow(new ResourceAccessException("Donaciones no respondió"));

    mvc.perform(get("/admin-inicio"))
        .andExpect(status().isOk())
        .andExpect(content().string(matchesPattern("(?s).*<h1[^>]*>Por decidir</h1>.*")))
        .andExpect(content().string(containsString("class=\"aviso\" data-tipo=\"error\"")))
        .andExpect(content().string(containsString("No pudimos cargar las propuestas.")))
        .andExpect(content().string(not(containsString("name=\"propuesta\""))))
        // Sin la vista de asignaciones, Propuestas y Conflictos van sin cuenta; las otras, con la
        // suya.
        .andExpect(
            content()
                .string(
                    matchesPattern(
                        "(?s).*<a href=\"/admin-inicio\"[^>]*aria-current=\"page\">Propuestas</a>.*")))
        .andExpect(
            content()
                .string(
                    matchesPattern(
                        "(?s).*<a href=\"/admin-entidades\"[^>]*>Solicitudes <b>3</b></a>.*")))
        .andExpect(content().string(containsString("<dd>1.207</dd>")))
        .andExpect(content().string(containsString("Cómo está todo")))
        .andExpect(content().string(not(containsString("null"))));
  }

  // DA-29: «Cómo está todo» es un solo resumen con la lista de datos, su fecha y sus salidas
  @Test
  void elResumenEsUnaListaDeDatosConSuFechaYSusSalidas() throws Exception {
    when(personas.inicio()).thenReturn(inicio(NADA, null, CIFRAS));

    String html = html("/admin-inicio");
    String resumen = html.substring(html.indexOf("id=\"todo-titulo\""));
    resumen = resumen.substring(0, resumen.indexOf("</section>"));
    assertThat(resumen)
        .contains("Cómo está todo", "Al 10 de noviembre de 2026, 18:30.", "<dl class=\"datos\">")
        .contains("<dt>En el depósito, sin asignar</dt>", "<dd>1.207</dd>")
        .contains("<dt>Asignadas, por salir</dt>", "<dt>En camino o en la puerta</dt>")
        .contains("<dt>No se pudo entregar</dt>", "<dt>Vencidas en el depósito</dt>")
        .contains("<dt>Entregas confirmadas en noviembre, del 1 al 10</dt>")
        .contains("<dt>Entregas confirmadas en total</dt>", "<dd>1.284</dd>")
        .contains("<dt>La red</dt>")
        .contains("<dd>1 organización activa · 20.412 donantes activos, de 20.530 registrados</dd>")
        .contains("<dt>Camiones</dt>", "<dd>1 en ruta, 2 libres</dd>")
        .contains(">Ver las donaciones en el depósito</a>")
        .contains(">Ver las donaciones asignadas</a>")
        .contains(">Ver las donaciones vencidas en el depósito</a>")
        .contains(">Ver todas las donaciones</a>", ">Ver donantes</a>", ">Ver camiones</a>")
        .contains("Las confirma cada organización con su foto.")
        // En camino da 0: se muestra con su 0 y sin su salida.
        .doesNotContain("Ver las donaciones en camino");
    assertThat(html)
        .doesNotContain("registro-mesa")
        .doesNotContain("Entregas para mirar hoy")
        .doesNotContain("null");
  }

  @Test
  void siLogisticaNoRespondioSoloLaFilaDeCamionesMuestraSuAviso() throws Exception {
    Cifras sinCamiones =
        new Cifras(7, 4, 1, 1, 2, 0, "en noviembre, del 1 al 10", 0, 46, 86, 92, null);
    when(personas.inicio()).thenReturn(inicio(NADA, null, sinCamiones));

    String html = html("/admin-inicio");
    String camiones = html.substring(html.indexOf("<dt>Camiones</dt>"));
    camiones = camiones.substring(0, camiones.indexOf("</dd>"));
    assertThat(camiones)
        .contains("class=\"aviso\" data-tipo=\"error\"", "No pudimos cargar los camiones.")
        .contains("El resto de las cifras está al día.");
    assertThat(html.split("No pudimos cargar los camiones.", -1)).hasSize(2);
    assertThat(html).doesNotContain("No pudimos cargar las propuestas.");
    assertThat(html)
        .doesNotContain(">Ver camiones</a>")
        .contains("<dd>46 organizaciones activas · 86 donantes activos, de 92 registrados</dd>")
        .contains("Todavía no hay entregas confirmadas.")
        .contains("Se ve entre las asignadas.");
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
        // Las tablas de los gráficos son la tabla del catálogo (P20), con su rótulo por dato.
        .andExpect(content().string(containsString("<table class=\"tabla\">")))
        .andExpect(content().string(containsString("data-rotulo=\"Entregas confirmadas\"")))
        .andExpect(content().string(containsString("<th scope=\"row\">Otras 3 categorías</th>")))
        .andExpect(content().string(containsString("aria-hidden=\"true\"")))
        .andExpect(content().string(containsString("Noviembre todavía no terminó.")))
        .andExpect(content().string(containsString("href=\"#cat-alimentos\"")))
        .andExpect(content().string(not(containsString("No pudimos cargar los gráficos."))));
  }

  @Test
  void sinDonacionesLaLaminaLoDiceSinDibujo() throws Exception {
    when(personas.inicio()).thenReturn(conGraficos(Evolucion.SIN_DONACIONES));

    mvc.perform(get("/admin-inicio"))
        .andExpect(content().string(containsString("Todavía no hay donaciones registradas.")))
        .andExpect(
            content()
                .string(containsString("Los gráficos aparecen cuando se registre la primera.")))
        .andExpect(content().string(not(containsString("Por categoría"))))
        .andExpect(content().string(not(containsString("<caption>Donaciones registradas"))));
  }

  @Test
  void siLaEvolucionNoLlegoSoloSuLaminaMuestraElAviso() throws Exception {
    when(personas.inicio()).thenReturn(conGraficos(null));

    mvc.perform(get("/admin-inicio"))
        .andExpect(content().string(containsString("Los últimos 12 meses")))
        .andExpect(content().string(containsString("No pudimos cargar los gráficos.")))
        .andExpect(content().string(containsString("Las cifras de arriba están al día.")))
        .andExpect(content().string(containsString("20.412 donantes activos")))
        .andExpect(content().string(not(containsString("Mes a mes"))));
  }
}
