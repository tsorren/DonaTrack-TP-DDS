package grupo5.cliente.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

import grupo5.cliente.puertos.AdminPersonasPuerto;
import grupo5.cliente.puertos.CuentasDelMenu;
import grupo5.cliente.puertos.PersonaNueva;
import grupo5.cliente.vistas.CuentaVista;
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
@WebMvcTest({AdminEntidadesEnvioController.class})
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
