package grupo5.cliente.cuentas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

class DirectorioKeycloakTest {

  private static final String BASE = "http://keycloak/admin/realms/donatrack";
  private static final String USUARIO = BASE + "/users/cuenta-1";
  private static final String ROLES =
      USUARIO + "/role-mappings/clients/7f3c2a9e-5b1d-4e8a-9c6f-2d4b8a1e0c53";
  private static final String CUENTA =
      "{\"id\":\"cuenta-1\",\"email\":\"ana@ejemplo.test\",\"firstName\":\"Ana\","
          + "\"attributes\":{\"locale\":[\"es\"]}}";

  private final RestClient.Builder base = RestClient.builder().baseUrl(BASE);
  private final MockRestServiceServer keycloak = MockRestServiceServer.bindTo(base).build();
  private final DirectorioKeycloak directorio = new DirectorioKeycloak(base.build());

  @Test
  void elCorreoSaleDeLaCuenta() {
    keycloak
        .expect(requestTo(USUARIO))
        .andExpect(method(GET))
        .andRespond(withSuccess(CUENTA, APPLICATION_JSON));

    assertThat(directorio.correoDe("cuenta-1")).isEqualTo("ana@ejemplo.test");
  }

  @Test
  void vincularSumaLosAtributosSinPerderLosQueTeniaYAsignaLaPlantilla() {
    keycloak.expect(requestTo(USUARIO)).andRespond(withSuccess(CUENTA, APPLICATION_JSON));
    keycloak
        .expect(requestTo(USUARIO))
        .andExpect(method(PUT))
        .andExpect(jsonPath("$.email").value("ana@ejemplo.test"))
        .andExpect(jsonPath("$.firstName").value("Ana"))
        .andExpect(jsonPath("$.attributes.locale[0]").value("es"))
        .andExpect(jsonPath("$.attributes.personaId[0]").value("p-1"))
        .andRespond(withNoContent());
    keycloak
        .expect(requestTo(ROLES + "/available"))
        .andRespond(
            withSuccess(
                "[{\"id\":\"r1\",\"name\":\"OPERADOR_ENTIDAD\"},"
                    + "{\"id\":\"r2\",\"name\":\"DONANTE_PERSONA\"}]",
                APPLICATION_JSON));
    keycloak
        .expect(requestTo(ROLES))
        .andExpect(method(POST))
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value("r2"))
        .andExpect(jsonPath("$[0].name").value("DONANTE_PERSONA"))
        .andRespond(withNoContent());

    directorio.vincular("cuenta-1", Map.of("personaId", "p-1"), Optional.of("DONANTE_PERSONA"));

    keycloak.verify();
  }

  @Test
  void sinPlantillaNoSeTocanLosRoles() {
    keycloak.expect(requestTo(USUARIO)).andRespond(withSuccess(CUENTA, APPLICATION_JSON));
    keycloak.expect(requestTo(USUARIO)).andExpect(method(PUT)).andRespond(withNoContent());

    directorio.vincular("cuenta-1", Map.of("solicitudEntidad", "pendiente"), Optional.empty());

    keycloak.verify();
  }

  @Test
  void unaPlantillaQueElRealmNoOfreceEsUnError() {
    keycloak.expect(requestTo(USUARIO)).andRespond(withSuccess(CUENTA, APPLICATION_JSON));
    keycloak.expect(requestTo(USUARIO)).andExpect(method(PUT)).andRespond(withNoContent());
    keycloak
        .expect(requestTo(ROLES + "/available"))
        .andRespond(withSuccess("[]", APPLICATION_JSON));

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> directorio.vincular("cuenta-1", Map.of("personaId", "p-1"), Optional.of("NO_ES")))
        .isInstanceOf(NoSuchElementException.class);
  }

  @Test
  void siKeycloakFallaElErrorLlegaAQuienLlama() {
    keycloak.expect(requestTo(USUARIO)).andRespond(withStatus(HttpStatus.BAD_GATEWAY));

    org.assertj.core.api.Assertions.assertThatThrownBy(() -> directorio.correoDe("cuenta-1"))
        .isInstanceOf(HttpServerErrorException.class);
  }
}
