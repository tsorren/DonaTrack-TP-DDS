package grupo5.cliente.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.cliente.puertos.AdminPersonasPuerto;
import grupo5.cliente.puertos.CuentasDelMenu;
import grupo5.cliente.puertos.PersonaNueva;
import grupo5.cliente.vistas.AdminEntidadesVista;
import grupo5.cliente.vistas.AdminEntidadesVista.Entidad;
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

/**
 * Entidades fuera de la maqueta, sin la cadena de seguridad (la prueba SolicitudesConIngresoTest):
 * lo que llega al puerto y lo que vuelve a la pantalla.
 */
@WebMvcTest({AdminEntidadesController.class, AdminEntidadesEnvioController.class})
@AutoConfigureMockMvc(addFilters = false)
@Import({Pantallas.class, AdminDonantesControllerTest.Keycloak.class})
@TestPropertySource(
    properties = {
      "CLIENTE_LIVIANO_SECRET=prueba",
      "spring.security.oauth2.client.provider.keycloak.issuer-uri=http://keycloak/realms/donatrack"
    })
class AdminEntidadesControllerTest {

  @MockitoBean private AdminPersonasPuerto personas;
  @MockitoBean private CuentasDelMenu cuentas;
  @Autowired private MockMvc mvc;

  @BeforeEach
  void armar() {
    when(cuentas.de("admin"))
        .thenReturn(Optional.of(new CuentaVista("Leo", "Administración", null)));
  }

  /** Una entidad como la da la API: sin lo que nadie guarda. */
  private static Entidad comoLaApi(String clave, String estado, String nombre, String cuit) {
    return new Entidad(
        clave, estado, nombre, "ONG", null, "", null, null, null, List.of(), null, null, null, null,
        null, null, null, cuit, null, null, null, null, null);
  }

  @Test
  void conLoQueDaLaApiCadaFichaMuestraLoQueSabeYNoEscribeNull() throws Exception {
    when(personas.entidades())
        .thenReturn(
            new AdminEntidadesVista(
                1,
                "10 de noviembre de 2026, 18:30",
                List.of(
                    comoLaApi("e1", "activa", "Comedor Ejemplo", null),
                    comoLaApi("cuenta-1", "pendiente", "Hogar Ejemplo", "30-00000011-0"),
                    comoLaApi("e2", "inactiva", "Club Ejemplo", null))));

    for (String entidad : new String[] {"e1", "cuenta-1", "e2"}) {
      mvc.perform(get("/admin-entidades?entidad=" + entidad))
          .andExpect(status().isOk())
          .andExpect(content().string(not(containsString("null"))))
          .andExpect(
              content()
                  .string(
                      containsString(
                          "Elegí una para ver su ficha. Al 10 de noviembre de 2026, 18:30.")))
          .andExpect(content().string(containsString("Activas · 1")));
    }
    mvc.perform(get("/admin-entidades?entidad=cuenta-1"))
        .andExpect(content().string(containsString("ONG · pidió sumarse")))
        .andExpect(content().string(containsString("Pendiente de aprobación")))
        .andExpect(content().string(containsString("CUIT 30-00000011-0")))
        .andExpect(content().string(not(containsString("data-tono=\"error\"><span"))))
        .andExpect(content().string(containsString("name=\"entidad\" value=\"cuenta-1\"")))
        .andExpect(content().string(containsString("hx-indicator=\"#enviando-aprobar-cuenta-1\"")));
    mvc.perform(get("/admin-entidades?entidad=e1"))
        .andExpect(content().string(containsString(">Activa<")))
        .andExpect(content().string(not(containsString("Entregas confirmadas"))))
        .andExpect(content().string(not(containsString("Misión"))));
    mvc.perform(get("/admin-entidades?entidad=e2"))
        .andExpect(content().string(containsString(">Inactiva<")))
        .andExpect(content().string(not(containsString("Estuvo activa"))));
  }

  @Test
  void aprobarDejaElAvisoConElNombre() throws Exception {
    when(personas.aprobarEntidad("cuenta-1")).thenReturn("Hogar Ejemplo");

    mvc.perform(post("/admin-entidades-aprobada").param("entidad", "cuenta-1"))
        .andExpect(redirectedUrl("/admin-entidades"))
        .andExpect(
            flash()
                .attribute(
                    Envios.AVISO,
                    "Aprobaste a Hogar Ejemplo. Ya puede publicar sus necesidades y recibir"
                        + " donaciones."));
  }

  @Test
  void registrarLaEntidadLlevaSuDireccionYSuResponsableYQuedaActiva() throws Exception {
    mvc.perform(
            post("/admin-entidades-registrada")
                .param("razonSocial", "Escuela Ejemplo N.º 5")
                .param("tipo", "Institución")
                .param("cuit", "30-00000001-0")
                .param("calle", "Calle Ejemplo")
                .param("altura", "123")
                .param("localidad", "Localidad Ejemplo")
                .param("provincia", "Buenos Aires")
                .param("pais", "Argentina")
                .param("responsable", "Clara María Ejemplo")
                .param("correo", "escuela5@ejemplo.org"))
        .andExpect(redirectedUrl("/admin-entidades"))
        .andExpect(
            flash()
                .attribute(
                    Envios.AVISO,
                    "Registraste a Escuela Ejemplo N.º 5. Queda activa: la registraste vos, no"
                        + " necesita aprobación."));
    verify(personas)
        .registrarEntidad(
            new PersonaNueva.Juridica(
                "Escuela Ejemplo N.º 5",
                "INSTITUCION",
                null,
                new PersonaNueva.Direccion(
                    "Calle Ejemplo", 123, null, "Localidad Ejemplo", "Buenos Aires", "Argentina"),
                new PersonaNueva.Humana("Clara María", "Ejemplo", "escuela5@ejemplo.org"),
                "escuela5@ejemplo.org"));
  }
}
