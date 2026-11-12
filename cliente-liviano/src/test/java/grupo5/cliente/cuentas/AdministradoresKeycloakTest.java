package grupo5.cliente.cuentas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.ExpectedCount.never;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withCreatedEntity;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import grupo5.cliente.puertos.DirectorioDeAdministradores.Administrador;
import grupo5.cliente.puertos.DirectorioDeAdministradores.Alta;
import grupo5.cliente.puertos.DirectorioDeAdministradores.AltaIncompleta;
import grupo5.cliente.puertos.DirectorioDeAdministradores.CorreoEnUso;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class AdministradoresKeycloakTest {

  private static final String BASE = "http://keycloak/admin/realms/donatrack";
  private static final String NUEVA = BASE + "/users/cuenta-nueva";
  private static final String ROLES = NUEVA + "/role-mappings/realm";
  // 2025-11-12 12:00 en la Argentina
  private static final long CREADA = 1_762_959_600_000L;

  private static final String ANA =
      "{\"id\":\"cuenta-ana\",\"email\":\"ana.perez@ejemplo.org\",\"firstName\":\"Ana\","
          + "\"lastName\":\"Pérez\",\"createdTimestamp\":"
          + CREADA
          + ",\"requiredActions\":[],\"attributes\":{\"administracion\":[\"si\"],"
          + "\"locale\":[\"es\"]}}";
  private static final String BRUNO =
      "{\"id\":\"cuenta-bruno\",\"email\":\"bruno.diaz@ejemplo.org\",\"firstName\":\"Bruno\","
          + "\"lastName\":\"Díaz\",\"createdTimestamp\":"
          + CREADA
          + ",\"requiredActions\":[\"UPDATE_PASSWORD\"],"
          + "\"attributes\":{\"administracion\":[\"si\"]}}";
  private static final String DONANTE =
      "{\"id\":\"cuenta-donante\",\"email\":\"dona@ejemplo.org\",\"requiredActions\":"
          + "[\"UPDATE_PASSWORD\"],\"attributes\":{\"personaId\":[\"p-1\"]}}";

  private final RestClient.Builder base = RestClient.builder().baseUrl(BASE);
  private final MockRestServiceServer keycloak = MockRestServiceServer.bindTo(base).build();
  private final AdministradoresKeycloak directorio = new AdministradoresKeycloak(base.build());

  // C5: el total con /users/count y la página con first/max, nunca la lista completa
  @Test
  void elTotalSaleDelConteoDelServicio() {
    keycloak
        .expect(requestTo(BASE + "/users/count?q=administracion:si"))
        .andExpect(method(GET))
        .andRespond(withSuccess("12", APPLICATION_JSON));

    assertThat(directorio.total()).isEqualTo(12);
  }

  @Test
  void cadaPaginaPideDe10AlServicio() {
    keycloak
        .expect(
            requestTo(
                BASE + "/users?q=administracion:si&first=10&max=10&briefRepresentation=false"))
        .andExpect(method(GET))
        .andRespond(withSuccess("[" + ANA + "," + BRUNO + "]", APPLICATION_JSON));

    List<Administrador> pagina = directorio.pagina(2);

    assertThat(pagina)
        .containsExactly(
            new Administrador(
                "cuenta-ana",
                "Ana",
                "Pérez",
                "ana.perez@ejemplo.org",
                LocalDate.of(2025, 11, 12),
                true),
            new Administrador(
                "cuenta-bruno",
                "Bruno",
                "Díaz",
                "bruno.diaz@ejemplo.org",
                LocalDate.of(2025, 11, 12),
                false));
    keycloak.verify();
  }

  // R11-26, C11: la búsqueda por administrador, acotada a la marca y con max; nunca la lista entera
  @Test
  void buscarPideLasMarcadasQueCoincidenConMaxYDescartaLasQueNoSonDeAdministracion() {
    keycloak
        .expect(
            requestTo(
                BASE + "/users?q=administracion:si&search=bruno&max=21&briefRepresentation=false"))
        .andExpect(method(GET))
        .andRespond(withSuccess("[" + BRUNO + "," + DONANTE + "]", APPLICATION_JSON));

    List<Administrador> encontradas = directorio.buscar("bruno", 21);

    assertThat(encontradas).extracting(Administrador::id).containsExactly("cuenta-bruno");
    keycloak.verify();
  }

  @Test
  void laCuentaSeBuscaPorSuIdYSoloSiEsDeAdministracion() {
    keycloak
        .expect(requestTo(BASE + "/users/cuenta-ana"))
        .andRespond(withSuccess(ANA, APPLICATION_JSON));
    keycloak
        .expect(requestTo(BASE + "/users/cuenta-donante"))
        .andRespond(withSuccess(DONANTE, APPLICATION_JSON));
    keycloak
        .expect(requestTo(BASE + "/users/no-existe"))
        .andRespond(withStatus(HttpStatus.NOT_FOUND));

    assertThat(directorio.cuenta("cuenta-ana"))
        .map(Administrador::correo)
        .contains("ana.perez@ejemplo.org");
    assertThat(directorio.cuenta("cuenta-donante")).isEmpty();
    assertThat(directorio.cuenta("no-existe")).isEmpty();
  }

  // C9: cambia el nombre y el apellido, el resto de la cuenta queda igual
  @Test
  void cambiarElNombreConservaElRestoDeLaCuenta() {
    keycloak
        .expect(requestTo(BASE + "/users/cuenta-ana"))
        .andExpect(method(GET))
        .andRespond(withSuccess(ANA, APPLICATION_JSON));
    keycloak
        .expect(requestTo(BASE + "/users/cuenta-ana"))
        .andExpect(method(PUT))
        .andExpect(jsonPath("$.firstName").value("Ana María"))
        .andExpect(jsonPath("$.lastName").value("Pérez"))
        .andExpect(jsonPath("$.email").value("ana.perez@ejemplo.org"))
        .andExpect(jsonPath("$.attributes.administracion[0]").value("si"))
        .andExpect(jsonPath("$.attributes.locale[0]").value("es"))
        .andRespond(withNoContent());

    directorio.cambiarNombre("cuenta-ana", "Ana María", "Pérez");

    keycloak.verify();
  }

  // C1: la cuenta nueva queda marcada, con la contraseña provisoria y solo con ADMIN
  @Test
  void elAltaCreaLaCuentaLeDaAdminYLeSacaElRolPorDefecto() {
    keycloak
        .expect(requestTo(BASE + "/users"))
        .andExpect(method(POST))
        .andExpect(jsonPath("$.username").value("bruno.diaz@ejemplo.org"))
        .andExpect(jsonPath("$.email").value("bruno.diaz@ejemplo.org"))
        .andExpect(jsonPath("$.firstName").value("Bruno"))
        .andExpect(jsonPath("$.lastName").value("Díaz"))
        .andExpect(jsonPath("$.enabled").value(true))
        .andExpect(jsonPath("$.emailVerified").value(true))
        .andExpect(jsonPath("$.attributes.administracion[0]").value("si"))
        .andExpect(jsonPath("$.attributes.altaPor[0]").value("cuenta-ana"))
        .andExpect(jsonPath("$.credentials[0].type").value("password"))
        .andExpect(jsonPath("$.credentials[0].value").value("K7pQ-m4Rt-x9Wa-2Lse"))
        .andExpect(jsonPath("$.credentials[0].temporary").value(true))
        .andRespond(withCreatedEntity(URI.create(NUEVA)));
    keycloak
        .expect(requestTo(ROLES + "/available"))
        .andRespond(
            withSuccess(
                "[{\"id\":\"r-1\",\"name\":\"ENTIDAD\"},{\"id\":\"r-2\",\"name\":\"ADMIN\"}]",
                APPLICATION_JSON));
    keycloak
        .expect(requestTo(ROLES))
        .andExpect(method(POST))
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].name").value("ADMIN"))
        .andRespond(withNoContent());
    keycloak
        .expect(requestTo(ROLES))
        .andExpect(method(GET))
        .andRespond(
            withSuccess(
                "[{\"id\":\"r-2\",\"name\":\"ADMIN\"},"
                    + "{\"id\":\"r-9\",\"name\":\"default-roles-donatrack\"}]",
                APPLICATION_JSON));
    keycloak
        .expect(requestTo(ROLES))
        .andExpect(method(DELETE))
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].name").value("default-roles-donatrack"))
        .andRespond(withNoContent());

    String id =
        directorio.alta(
            new Alta("Bruno", "Díaz", "bruno.diaz@ejemplo.org"),
            "K7pQ-m4Rt-x9Wa-2Lse",
            "cuenta-ana");

    assertThat(id).isEqualTo("cuenta-nueva");
    keycloak.verify();
  }

  // C3: correo repetido, no se crea nada
  @Test
  void elAltaConUnCorreoUsadoAvisaSinCrearNada() {
    keycloak
        .expect(requestTo(BASE + "/users"))
        .andExpect(method(POST))
        .andRespond(withStatus(HttpStatus.CONFLICT));

    assertThatThrownBy(
            () ->
                directorio.alta(
                    new Alta("Bruno", "Díaz", "usado@ejemplo.org"), "K7pQ-m4Rt-x9Wa-2Lse", "a"))
        .isInstanceOf(CorreoEnUso.class);
    keycloak.verify();
  }

  // C4: si falla al darle el rol, la cuenta creada se borra
  @Test
  void unAltaAMediasSeDeshace() {
    keycloak
        .expect(requestTo(BASE + "/users"))
        .andExpect(method(POST))
        .andRespond(withCreatedEntity(URI.create(NUEVA)));
    keycloak.expect(requestTo(ROLES + "/available")).andRespond(withServerError());
    keycloak.expect(requestTo(NUEVA)).andExpect(method(DELETE)).andRespond(withNoContent());

    assertThatThrownBy(
            () ->
                directorio.alta(
                    new Alta("Bruno", "Díaz", "bruno.diaz@ejemplo.org"),
                    "K7pQ-m4Rt-x9Wa-2Lse",
                    "cuenta-ana"))
        .isInstanceOf(AltaIncompleta.class);
    keycloak.verify();
  }

  // C11: otra contraseña provisoria solo para la cuenta que todavía no entró
  @Test
  void otraContrasenaReemplazaLaProvisoriaSiLaCuentaNoEntro() {
    keycloak
        .expect(requestTo(BASE + "/users/cuenta-bruno"))
        .andRespond(withSuccess(BRUNO, APPLICATION_JSON));
    keycloak
        .expect(requestTo(BASE + "/users/cuenta-bruno/reset-password"))
        .andExpect(method(PUT))
        .andExpect(jsonPath("$.type").value("password"))
        .andExpect(jsonPath("$.value").value("Nuev-aCon-tras-e8a9"))
        .andExpect(jsonPath("$.temporary").value(true))
        .andRespond(withNoContent());

    assertThat(directorio.otraContrasena("cuenta-bruno", "Nuev-aCon-tras-e8a9")).isTrue();
    keycloak.verify();
  }

  @Test
  void otraContrasenaNoCambiaNadaSiLaCuentaYaEntroONoEsDeAdministracion() {
    keycloak
        .expect(requestTo(BASE + "/users/cuenta-ana"))
        .andRespond(withSuccess(ANA, APPLICATION_JSON));
    keycloak
        .expect(requestTo(BASE + "/users/cuenta-donante"))
        .andRespond(withSuccess(DONANTE, APPLICATION_JSON));
    keycloak.expect(never(), requestTo(BASE + "/users/cuenta-ana/reset-password"));

    assertThat(directorio.otraContrasena("cuenta-ana", "Nuev-aCon-tras-e8a9")).isFalse();
    assertThat(directorio.otraContrasena("cuenta-donante", "Nuev-aCon-tras-e8a9")).isFalse();
    keycloak.verify();
  }
}
