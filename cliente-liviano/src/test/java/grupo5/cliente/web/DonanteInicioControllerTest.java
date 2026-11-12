package grupo5.cliente.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.cliente.maqueta.MaquetaConfig;
import grupo5.cliente.puertos.CuentaActual;
import grupo5.cliente.puertos.DonanteInicioPuerto;
import grupo5.cliente.puertos.DonantePerfilPuerto;
import grupo5.cliente.puertos.DonantePerfilPuerto.Tipo;
import grupo5.cliente.vistas.DonanteDonacionVista;
import grupo5.cliente.vistas.DonanteDonacionVista.Carta;
import grupo5.cliente.vistas.DonanteInicioVista;
import grupo5.cliente.vistas.DonanteInicioVista.Aviso;
import grupo5.cliente.vistas.EstadoEntrega;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * El inicio del donante con lo que da el puerto: las fechas de hoy y no las de la maqueta, y la
 * versión del tipo de la cuenta (R11-06).
 */
@WebMvcTest(controllers = {DonanteInicioController.class, PantallasController.class})
@ActiveProfiles("maqueta")
@Import({MaquetaConfig.class, Pantallas.class})
class DonanteInicioControllerTest {

  private static final UUID DONANTE = UUID.fromString("6f1c2c1e-0000-4000-8000-0000000000aa");

  private static final DonanteDonacionVista SILLA =
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
          EstadoEntrega.PENDIENTE,
          "Asignada, espera el camión",
          null,
          null,
          null,
          List.of(),
          List.of(
              new Carta(
                  "Asignamos tu donación a una necesidad de Hogar Ejemplo.",
                  "7 de noviembre, 10:00",
                  "2026-11-07T10:00")));

  private static final DonanteInicioVista CON_SILLA =
      new DonanteInicioVista(
          List.of(SILLA),
          List.of(new Aviso(SILLA, 0)),
          1,
          0,
          "noviembre de 2026",
          "7 de noviembre",
          "7 de noviembre, 19:40",
          "1 de noviembre",
          "2026-11-07",
          "2026-11-06");

  @MockitoBean private DonanteInicioPuerto inicios;
  @MockitoBean private DonantePerfilPuerto perfiles;
  @MockitoBean private CuentaActual cuentas;
  @Autowired private MockMvc mvc;

  @BeforeEach
  void cuentaDeEmpresa() {
    when(cuentas.donante(any())).thenReturn(Optional.of(DONANTE));
    when(perfiles.tipo(DONANTE)).thenReturn(Tipo.EMPRESA);
  }

  @Test
  void muestraLasCifrasYLasNoticiasDeHoy() throws Exception {
    when(inicios.inicio(DONANTE)).thenReturn(CON_SILLA);

    mvc.perform(get("/donante-inicio"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("en noviembre de 2026. Actualizado hoy,")))
        .andExpect(content().string(containsString("Noviembre de 2026")))
        .andExpect(content().string(containsString("entregas en curso hoy, 7 de noviembre")))
        .andExpect(content().string(containsString("desde el 1 de noviembre")))
        .andExpect(content().string(containsString("Va a Hogar Ejemplo.")))
        .andExpect(content().string(containsString("Hoy, 10:00")))
        .andExpect(content().string(containsString("Ver todas (1 sin leer)")));
  }

  @Test
  void laEmpresaNoVeLaHuertaNiQuePlantaSos() throws Exception {
    when(inicios.inicio(DONANTE)).thenReturn(CON_SILLA);

    mvc.perform(get("/donante-inicio"))
        .andExpect(status().isOk())
        .andExpect(content().string(not(containsString("Ver mi huerta"))))
        .andExpect(content().string(not(containsString("planta sos vos"))))
        .andExpect(content().string(not(containsString("href=\"/donante-misiones\""))));
  }

  @Test
  void laPersonaVeSuAnioConLaHuertaArribaDeTodo() throws Exception {
    when(perfiles.tipo(DONANTE)).thenReturn(Tipo.PERSONA);
    when(inicios.inicio(DONANTE)).thenReturn(CON_SILLA);

    mvc.perform(get("/donante-inicio"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Este es tu 2026, mes a mes")))
        .andExpect(content().string(containsString("Noviembre, este mes · 1 donación")))
        .andExpect(content().string(containsString("Silla plegable, 1 unidad")))
        .andExpect(content().string(containsString("Ver todas (1 sin leer)")))
        .andExpect(
            content()
                .string(
                    matchesPattern(
                        Pattern.compile(
                            "(?s).*Ver mi huerta.*id=\"anio-meses\".*", Pattern.DOTALL))))
        .andExpect(content().string(containsString(">Misiones<")))
        .andExpect(content().string(not(containsString("planta sos vos"))))
        .andExpect(content().string(not(containsString("Tu próxima misión"))))
        .andExpect(content().string(not(containsString("cuenta=persona"))))
        .andExpect(content().string(not(containsString("tu empresa"))));
  }

  @Test
  void sinTipoEsPersonaSinHuertaNiMisiones() throws Exception {
    when(perfiles.tipo(DONANTE)).thenReturn(Tipo.SIN_TIPO);
    when(inicios.inicio(DONANTE)).thenReturn(CON_SILLA);

    mvc.perform(get("/donante-inicio"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Este es tu 2026, mes a mes")))
        .andExpect(content().string(not(containsString("Ver mi huerta"))))
        .andExpect(content().string(not(containsString("href=\"/donante-misiones\""))))
        .andExpect(content().string(not(containsString("tu empresa"))));
  }

  @Test
  void laPersonaSinDonacionesVeElEstadoVacio() throws Exception {
    when(perfiles.tipo(DONANTE)).thenReturn(Tipo.PERSONA);
    when(inicios.inicio(DONANTE)).thenReturn(vacio());

    mvc.perform(get("/donante-inicio"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Todavía no tenés donaciones.")))
        .andExpect(content().string(containsString("Ver mi huerta")))
        .andExpect(content().string(not(containsString("id=\"anio-meses\""))));
  }

  @Test
  void sinCuentaMandaAIngresar() throws Exception {
    when(cuentas.donante(any())).thenReturn(Optional.empty());

    mvc.perform(get("/donante-inicio")).andExpect(redirectedUrl("/publico-ingresar"));
  }

  @Test
  void unaCuentaSinDonacionesVeElEstadoVacio() throws Exception {
    when(inicios.inicio(DONANTE)).thenReturn(vacio());

    mvc.perform(get("/donante-inicio"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Todavía no hay donaciones de tu empresa.")))
        .andExpect(content().string(not(containsString("Lo que <em>ya dio fruto.</em>"))));
  }

  private static DonanteInicioVista vacio() {
    return new DonanteInicioVista(
        List.of(),
        List.of(),
        0,
        0,
        "noviembre de 2026",
        "7 de noviembre",
        "7 de noviembre, 19:40",
        "7 de noviembre",
        "2026-11-07",
        "2026-11-06");
  }
}
