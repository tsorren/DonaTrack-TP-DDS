package grupo5.cliente.maqueta;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * La portada y las demás públicas con la sesión iniciada, en la maqueta (docs/interfaz/flujos/
 * r11-p1-portada.md, R11-15): {@code ?escenario=con-sesion} muestra la cuenta y "Ir a mi inicio" en
 * lugar de "Ingresar" y "Crear una cuenta"; {@code ?escenario=con-sesion-pendiente}, la cuenta con
 * la solicitud en revisión y "Ver mi solicitud".
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("maqueta")
class PortadaConSesionMaquetaTest {

  private static final String CREAR_UNA_CUENTA = "href=\"/publico-registro\"";
  private static final String INGRESAR = "href=\"/publico-ingresar\"";
  private static final String CERRAR_SESION = "Cerrar sesión";
  private static final String IR_A_MI_INICIO = "Ir a mi inicio";
  private static final String ADMINISTRADORES = "href=\"/admin-administradores\"";

  @Autowired private MockMvc mvc;

  @Test
  void conSesionLaPortadaMuestraLaCuentaYIrAMiInicio() throws Exception {
    mvc.perform(get("/publico-portada?escenario=con-sesion"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("class=\"navegacion-cuenta\"")))
        .andExpect(content().string(containsString("aria-label=\"Cuenta de Distribuidora")))
        .andExpect(content().string(containsString(IR_A_MI_INICIO)))
        .andExpect(content().string(containsString(">Mi inicio<")))
        .andExpect(content().string(containsString("href=\"/donante-inicio\"")))
        .andExpect(content().string(containsString("href=\"/donante-perfil\">Mi perfil")))
        .andExpect(content().string(containsString(CERRAR_SESION)))
        .andExpect(content().string(containsString("href=\"/publico-mapa\"")))
        .andExpect(content().string(not(containsString(CREAR_UNA_CUENTA))))
        .andExpect(content().string(not(containsString(INGRESAR))))
        .andExpect(content().string(not(containsString("armazon-avisos"))))
        .andExpect(
            content()
                .string(
                    not(matchesPattern("(?s).*aria-current=\"page\"[^>]*>\\s*<span[^>]*>Ir a.*"))));
  }

  @Test
  void elLogoDeLaPortadaLlevaALaPortadaConOSinSesion() throws Exception {
    for (String direccion : List.of("/publico-portada", "/publico-portada?escenario=con-sesion")) {
      mvc.perform(get(direccion))
          .andExpect(content().string(containsString("aria-label=\"DonaTrack, ir a la portada\"")))
          .andExpect(content().string(not(containsString("ir al inicio"))));
    }
  }

  @Test
  void laAdministracionVaASuInicioConSuPerfilYAdministradores() throws Exception {
    // Desde R11-10 (p1-admins) la administración tiene su Mi perfil y, solo ella, Administradores.
    mvc.perform(get("/publico-portada?escenario=con-sesion&area=admin"))
        .andExpect(content().string(containsString("href=\"/admin-inicio\"")))
        .andExpect(content().string(containsString(IR_A_MI_INICIO)))
        .andExpect(content().string(containsString("href=\"/admin-perfil\">Mi perfil")))
        .andExpect(content().string(containsString(ADMINISTRADORES)));
    mvc.perform(get("/publico-portada?escenario=con-sesion&area=entidad"))
        .andExpect(content().string(containsString("href=\"/entidad-inicio\"")))
        .andExpect(content().string(containsString("href=\"/entidad-perfil\">Mi perfil")))
        .andExpect(content().string(not(containsString(ADMINISTRADORES))));
  }

  @Test
  void conLaSolicitudPendienteOfreceVerLaSolicitudSinMiPerfil() throws Exception {
    mvc.perform(get("/publico-portada?escenario=con-sesion-pendiente"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("class=\"navegacion-cuenta\"")))
        .andExpect(content().string(containsString("Ver mi solicitud")))
        .andExpect(content().string(containsString(">Mi solicitud<")))
        .andExpect(content().string(containsString("href=\"/publico-registro-entidad-enviada\"")))
        .andExpect(content().string(containsString("Solicitud en revisión")))
        .andExpect(content().string(containsString(CERRAR_SESION)))
        .andExpect(content().string(not(containsString(IR_A_MI_INICIO))))
        .andExpect(content().string(not(containsString(">Mi perfil<"))))
        .andExpect(content().string(not(containsString(CREAR_UNA_CUENTA))));
  }

  @Test
  void sinSesionLaPortadaSigueConElEncabezadoDeVisitante() throws Exception {
    for (String direccion :
        List.of("/", "/publico-portada", "/publico-portada?escenario=vacio", "/publico-mapa")) {
      mvc.perform(get(direccion))
          .andExpect(content().string(containsString(CREAR_UNA_CUENTA)))
          .andExpect(content().string(containsString(INGRESAR)))
          .andExpect(content().string(not(containsString("class=\"navegacion-cuenta\""))))
          .andExpect(content().string(not(containsString(IR_A_MI_INICIO))));
    }
  }

  @Test
  void lasDemasPublicasTambienMuestranLaCuenta() throws Exception {
    for (String pantalla :
        List.of(
            "publico-donaciones",
            "publico-mapa",
            "publico-entidades",
            "publico-nosotros",
            "publico-para-donantes",
            "publico-para-entidades",
            "publico-legales")) {
      mvc.perform(get("/" + pantalla + "?escenario=con-sesion"))
          .andExpect(status().isOk())
          .andExpect(content().string(containsString("class=\"navegacion-cuenta\"")))
          .andExpect(content().string(containsString(IR_A_MI_INICIO)))
          .andExpect(content().string(not(containsString(CREAR_UNA_CUENTA))));
    }
  }

  @Test
  void contactoSigueConElMarcoPrivadoDelRol() throws Exception {
    mvc.perform(get("/publico-contacto?escenario=con-sesion"))
        .andExpect(content().string(containsString("class=\"raices-privado\"")))
        .andExpect(content().string(not(containsString(IR_A_MI_INICIO))));
  }
}
