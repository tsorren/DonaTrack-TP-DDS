package grupo5.cliente.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.cliente.maqueta.MaquetaConfig;
import grupo5.cliente.puertos.CuentaActual;
import grupo5.cliente.puertos.DonantePerfilPuerto;
import grupo5.cliente.puertos.DonantePerfilPuerto.Tipo;
import grupo5.cliente.vistas.DonantePerfilVista;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * En la maqueta, con un puerto que da otra cuenta: la pantalla lee del modelo y elige la versión
 * por el tipo de la cuenta (R11-06).
 */
@WebMvcTest(controllers = {DonantePerfilController.class, PantallasController.class})
@ActiveProfiles("maqueta")
@Import({MaquetaConfig.class, Pantallas.class})
class DonantePerfilControllerTest {
  private static final UUID DONANTE = UUID.fromString("6f1c2c1e-0000-4000-8000-0000000000aa");

  private static final DonantePerfilVista PERSONA =
      new DonantePerfilVista(
          "Martina Ruiz",
          "Persona donante · martina@ejemplo.test",
          "Donante Colaborador",
          true,
          null,
          null,
          "Calle Falsa",
          null,
          null,
          null,
          "Caballito",
          "Ciudad de Buenos Aires",
          "martina@ejemplo.test",
          null,
          null,
          "Aceptaste la versión 2 el 3 de noviembre de 2026.",
          "Martina",
          "Ruiz",
          "DNI 12.345.678");

  private static final DonantePerfilVista EMPRESA =
      new DonantePerfilVista(
          "Distribuidora Ejemplo SA",
          "Empresa donante · contacto@ejemplo.test",
          "Empresa donante · Sostenedor",
          false,
          "Alimentos",
          "30-00000000-0",
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          "Aceptaste la versión 2 el 3 de noviembre de 2026.",
          null,
          null,
          null);

  @MockitoBean private DonantePerfilPuerto perfiles;
  @MockitoBean private CuentaActual cuentas;
  @Autowired private MockMvc mvc;

  @Test
  void laPersonaVeSusDatosYNuncaLosDeUnaEmpresa() throws Exception {
    when(cuentas.donante(any())).thenReturn(Optional.of(DONANTE));
    when(perfiles.tipo(DONANTE)).thenReturn(Tipo.PERSONA);
    when(perfiles.perfil(DONANTE)).thenReturn(PERSONA);

    mvc.perform(get("/donante-perfil"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Persona donante · martina@ejemplo.test")))
        .andExpect(content().string(containsString("Donó: Donante Colaborador")))
        .andExpect(content().string(containsString("value=\"no\" checked")))
        .andExpect(content().string(containsString("value=\"Martina\"")))
        .andExpect(content().string(containsString("value=\"Ruiz\"")))
        // El documento, fijo: texto, no un campo (como la dirección de la organización).
        .andExpect(content().string(containsString("<strong>DNI 12.345.678</strong>")))
        .andExpect(content().string(not(containsString("name=\"documento\""))))
        .andExpect(content().string(containsString("value=\"Calle Falsa\"")))
        .andExpect(content().string(containsString("Aceptaste la versión 2")))
        .andExpect(content().string(containsString("hx-indicator=\"#enviando-donante-perfil\"")))
        .andExpect(content().string(containsString("href=\"/donante-misiones\"")))
        .andExpect(content().string(not(containsString("Razón social"))))
        .andExpect(content().string(not(containsString("CUIT"))))
        .andExpect(content().string(not(containsString(">Empresa<"))))
        .andExpect(content().string(not(containsString("Empresa donante ·"))))
        .andExpect(content().string(not(containsString("Fecha de nacimiento"))));
  }

  @Test
  void unaCuentaSinTipoNoSeTrataComoEmpresa() throws Exception {
    when(cuentas.donante(any())).thenReturn(Optional.of(DONANTE));
    when(perfiles.tipo(DONANTE)).thenReturn(Tipo.SIN_TIPO);
    when(perfiles.perfil(DONANTE)).thenReturn(PERSONA);

    mvc.perform(get("/donante-perfil"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("value=\"Martina\"")))
        .andExpect(content().string(not(containsString("Razón social"))))
        .andExpect(content().string(not(containsString("CUIT"))))
        .andExpect(content().string(not(containsString("href=\"/donante-misiones\""))))
        .andExpect(content().string(not(containsString("tus misiones"))));
  }

  @Test
  void laEmpresaSigueConRazonSocialYCuit() throws Exception {
    when(cuentas.donante(any())).thenReturn(Optional.of(DONANTE));
    when(perfiles.tipo(DONANTE)).thenReturn(Tipo.EMPRESA);
    when(perfiles.perfil(DONANTE)).thenReturn(EMPRESA);

    mvc.perform(get("/donante-perfil"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Razón social")))
        // El CUIT, fijo: texto, no un campo (como la dirección de la organización).
        .andExpect(content().string(containsString("<strong>30-00000000-0</strong>")))
        .andExpect(content().string(not(containsString("name=\"cuit\""))))
        .andExpect(content().string(not(containsString("href=\"/donante-misiones\""))));
  }

  /** DON-07: deriva de A12, como el de la organización; la persona sigue a la empresa (DA-13). */
  @Test
  void cadaSeccionEsUnFormularioConLasPiezasDelCatalogo() throws Exception {
    when(cuentas.donante(any())).thenReturn(Optional.of(DONANTE));
    when(perfiles.tipo(DONANTE)).thenReturn(Tipo.EMPRESA, Tipo.PERSONA);
    when(perfiles.perfil(DONANTE)).thenReturn(EMPRESA, PERSONA);

    for (int vez = 0; vez < 2; vez++) {
      mvc.perform(get("/donante-perfil"))
          .andExpect(status().isOk())
          .andExpect(content().string(containsString("/css/donante-perfil.css")))
          .andExpect(content().string(containsString("<h1>Mi perfil</h1>")))
          .andExpect(content().string(containsString("aria-label=\"Secciones de tu perfil\"")))
          .andExpect(
              content()
                  .string(
                      containsString(
                          "class=\"pila\" data-espacio=\"4\" aria-labelledby=\"nombre-titulo\"")))
          .andExpect(
              content()
                  .string(
                      containsString(
                          "class=\"pila\" data-espacio=\"4\" aria-labelledby=\"datos-titulo\"")))
          .andExpect(
              content()
                  .string(
                      containsString(
                          "class=\"pila\" data-espacio=\"4\" aria-labelledby=\"avisos-titulo\"")))
          .andExpect(content().string(containsString("<div class=\"campo\">")))
          .andExpect(content().string(containsString("<fieldset class=\"opciones\"")))
          .andExpect(content().string(containsString("class=\"boton\" data-tono=\"principal\"")))
          .andExpect(content().string(not(containsString("class=\"formulario"))))
          .andExpect(content().string(not(containsString("class=\"perfil"))))
          .andExpect(content().string(not(containsString("readonly"))));
    }
  }

  /** DA-06: después de guardar, el perfil muestra el aviso breve del servidor en su región. */
  @Test
  void despuesDeGuardarMuestraElAvisoBreve() throws Exception {
    when(cuentas.donante(any())).thenReturn(Optional.of(DONANTE));
    when(perfiles.tipo(DONANTE)).thenReturn(Tipo.PERSONA);
    when(perfiles.perfil(DONANTE)).thenReturn(PERSONA);

    mvc.perform(
            get("/donante-perfil")
                .flashAttr(
                    Envios.AVISO, "Listo. Las organizaciones ven tu nombre: «Martina Ruiz»."))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(
                    containsString(
                        "class=\"aviso-breve\" data-estado=\"hecho\">Listo. Las organizaciones ven"
                            + " tu nombre: «Martina Ruiz».")));
  }

  /**
   * En la maqueta, la ruta de guardado sigue: es el perfil con el aviso breve del catálogo, con su
   * signo y su cierre, para la empresa y para la persona.
   */
  @Test
  void enLaMaquetaGuardadoEsElPerfilConElAvisoBreve() throws Exception {
    when(cuentas.donante(any())).thenReturn(Optional.of(DONANTE));
    when(perfiles.tipo(DONANTE)).thenReturn(Tipo.EMPRESA, Tipo.PERSONA);
    when(perfiles.perfil(DONANTE)).thenReturn(EMPRESA, PERSONA);

    mvc.perform(get("/donante-perfil-guardado"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("<h1>Mi perfil</h1>")))
        .andExpect(content().string(containsString("hx-swap-oob=\"beforeend:#avisos\"><div")))
        .andExpect(content().string(containsString("class=\"aviso-breve\" data-estado=\"hecho\"")))
        .andExpect(content().string(containsString("aria-label=\"Cerrar el aviso\"")))
        .andExpect(
            content()
                .string(
                    containsString(
                        "<p>Listo. Las organizaciones ya no ven tu nombre: ven «Empresa donante ·"
                            + " Sostenedor».</p>")));
    mvc.perform(get("/donante-perfil-guardado").param("seccion", "datos"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("id=\"persona-perfil\"")))
        .andExpect(
            content().string(containsString("<p>Listo. Guardamos los datos de tu cuenta.</p>")))
        .andExpect(content().string(containsString("aria-label=\"Cerrar el aviso\"")))
        .andExpect(content().string(not(containsString("ya no ven tu nombre"))));
  }

  @Test
  void elTipoSePideUnaVezPorSesion() throws Exception {
    when(cuentas.donante(any())).thenReturn(Optional.of(DONANTE));
    when(perfiles.tipo(DONANTE)).thenReturn(Tipo.PERSONA);
    when(perfiles.perfil(DONANTE)).thenReturn(PERSONA);
    MockHttpSession sesion = new MockHttpSession();

    mvc.perform(get("/donante-perfil").session(sesion)).andExpect(status().isOk());
    mvc.perform(get("/donante-perfil").session(sesion)).andExpect(status().isOk());

    verify(perfiles, times(1)).tipo(DONANTE);
  }

  @Test
  void sinCuentaMandaAIngresar() throws Exception {
    when(cuentas.donante(any())).thenReturn(Optional.empty());

    mvc.perform(get("/donante-perfil")).andExpect(redirectedUrl("/publico-ingresar"));
  }
}
