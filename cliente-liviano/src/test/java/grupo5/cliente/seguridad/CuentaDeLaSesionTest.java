package grupo5.cliente.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import grupo5.cliente.puertos.DirectorioDeAdministradores;
import grupo5.cliente.puertos.DirectorioDeAdministradores.Administrador;
import grupo5.cliente.puertos.DonantePerfilPuerto;
import grupo5.cliente.puertos.EntidadPerfilPuerto;
import grupo5.cliente.vistas.CuentaVista;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** R11-44: el menú y el saludo dicen el nombre de la cuenta, nunca el rol. */
class CuentaDeLaSesionTest {

  private static final UUID PERSONA = UUID.randomUUID();
  private static final String EMISOR = "http://keycloak/realms/donatrack";

  private final DonantePerfilPuerto donantes = mock(DonantePerfilPuerto.class);
  private final EntidadPerfilPuerto entidades = mock(EntidadPerfilPuerto.class);
  private final DirectorioDeAdministradores administradores =
      mock(DirectorioDeAdministradores.class);
  private final StaticListableBeanFactory beans = new StaticListableBeanFactory();
  private CuentaDeLaSesion cuentas;
  private MockHttpServletRequest pedido;

  @BeforeEach
  void preparar() {
    beans.addBean("donantes", donantes);
    beans.addBean("entidades", entidades);
    beans.addBean("administradores", administradores);
    cuentas =
        new CuentaDeLaSesion(
            EMISOR,
            beans.getBeanProvider(DonantePerfilPuerto.class),
            beans.getBeanProvider(EntidadPerfilPuerto.class),
            beans.getBeanProvider(DirectorioDeAdministradores.class));
    pedido = new MockHttpServletRequest();
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(pedido));
  }

  @AfterEach
  void limpiar() {
    RequestContextHolder.resetRequestAttributes();
    SecurityContextHolder.clearContext();
  }

  private static void ingresaComo(String rol, UUID persona) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("sub", "cuenta-1");
    claims.put("roles", List.of(rol));
    if (persona != null) {
      claims.put("personaId", persona.toString());
    }
    OidcIdToken token = new OidcIdToken("t", Instant.now(), Instant.now().plusSeconds(60), claims);
    var usuario = new DefaultOidcUser(List.of(), token);
    SecurityContextHolder.getContext()
        .setAuthentication(new TestingAuthenticationToken(usuario, null));
  }

  @Test
  void laPersonaQueDonaSeVeConSuNombreYApellidoYSePideUnaVezPorSesion() {
    ingresaComo("DONANTE", PERSONA);
    when(donantes.cuenta(PERSONA)).thenReturn(new CuentaVista("Martina Ruiz", "Persona donante"));

    CuentaVista cuenta = cuentas.de("donante").orElseThrow();
    cuentas.de("donante");

    assertThat(cuenta.nombre()).isEqualTo("Martina Ruiz");
    assertThat(cuenta.rol()).isEqualTo("Persona donante");
    assertThat(cuenta.cambiarClave()).isEqualTo(EMISOR + "/account");
    verify(donantes, times(1)).cuenta(PERSONA);
  }

  @Test
  void laOrganizacionSeVeConSuRazonSocial() {
    ingresaComo("ENTIDAD", PERSONA);
    when(entidades.cuenta(PERSONA))
        .thenReturn(new CuentaVista("Comedor Ejemplo", EntidadPerfilPuerto.ORGANIZACION));

    CuentaVista cuenta = cuentas.de("entidad").orElseThrow();

    assertThat(cuenta.nombre()).isEqualTo("Comedor Ejemplo");
    assertThat(cuenta.rol()).isEqualTo("Organización");
  }

  @Test
  void laAdministracionSeVeConSuNombreOSuCorreoSiTodaviaNoLoCargo() {
    ingresaComo("ADMIN", null);
    when(administradores.cuenta("cuenta-1"))
        .thenReturn(
            Optional.of(
                new Administrador(
                    "cuenta-1", null, null, "admin@ejemplo.test", LocalDate.now(), true)));

    assertThat(cuentas.de("admin").orElseThrow().nombre()).isEqualTo("admin@ejemplo.test");

    when(administradores.cuenta("cuenta-1"))
        .thenReturn(
            Optional.of(
                new Administrador(
                    "cuenta-1", "Ana", "Pérez", "admin@ejemplo.test", LocalDate.now(), true)));
    cuentas.olvidar(pedido);

    CuentaVista cuenta = cuentas.de("admin").orElseThrow();
    assertThat(cuenta.nombre()).isEqualTo("Ana Pérez");
    assertThat(cuenta.rol()).isEqualTo("Administración");
  }

  @Test
  void siElServicioNoRespondeDiceTuCuentaYNoElRolYVuelveAProbar() {
    ingresaComo("DONANTE", PERSONA);
    when(donantes.cuenta(PERSONA)).thenThrow(new RestClientException("caído"));

    CuentaVista cuenta = cuentas.de("donante").orElseThrow();
    cuentas.de("donante");

    assertThat(cuenta.nombre()).isEqualTo(CuentaDeLaSesion.SIN_NOMBRE);
    assertThat(cuenta.rol()).isEqualTo("Donante");
    verify(donantes, times(2)).cuenta(PERSONA);
  }

  @Test
  void lasPaginasDeErrorSoloUsanLoQueLaSesionYaSabe() {
    ingresaComo("ENTIDAD", PERSONA);

    assertThat(cuentas.deLaSesion("entidad").orElseThrow().nombre())
        .isEqualTo(CuentaDeLaSesion.SIN_NOMBRE);
    verifyNoInteractions(entidades);

    when(entidades.cuenta(PERSONA))
        .thenReturn(new CuentaVista("Comedor Ejemplo", EntidadPerfilPuerto.ORGANIZACION));
    cuentas.de("entidad");
    assertThat(cuentas.deLaSesion("entidad").orElseThrow().nombre()).isEqualTo("Comedor Ejemplo");
  }

  @Test
  void unaPaginaPublicaNoTieneCuenta() {
    assertThat(cuentas.de("publico")).isEmpty();
  }
}
