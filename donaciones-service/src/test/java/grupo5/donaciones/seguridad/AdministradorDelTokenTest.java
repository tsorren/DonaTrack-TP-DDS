package grupo5.donaciones.seguridad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.common.seguridad.Actores;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

/** R11-26 (C2 de la spec): solo un token de ADMIN da autor; ningún otro deja actividad. */
class AdministradorDelTokenTest {

  private final AdministradorDelToken administrador = new AdministradorDelToken();

  @AfterEach
  void limpiar() {
    SecurityContextHolder.clearContext();
  }

  private static void conToken(String rol, String sub) {
    Jwt.Builder token =
        Jwt.withTokenValue("t")
            .header("alg", "none")
            .claim("roles", List.of(rol))
            .claim("personaId", UUID.randomUUID().toString());
    if (sub != null) {
      token.subject(sub);
    }
    Jwt jwt = token.build();
    SecurityContextHolder.getContext()
        .setAuthentication(
            new PreAuthenticatedAuthenticationToken(
                Actores.desde(jwt), jwt, Actores.autoridades(jwt)));
  }

  @Test
  void conTokenDeAdmin_daSuSub() {
    conToken("ADMIN", "sub-de-ana");

    assertEquals(Optional.of("sub-de-ana"), administrador.cuenta());
  }

  @Test
  void conTokenDeServicioDonanteUOrganizacion_noDaAutor() {
    for (String rol : List.of("SERVICIO", "DONANTE", "ENTIDAD")) {
      conToken(rol, "sub-" + rol);
      assertTrue(administrador.cuenta().isEmpty(), rol);
    }
  }

  @Test
  void sinTokenOSinSub_noDaAutor() {
    assertTrue(administrador.cuenta().isEmpty());

    conToken("ADMIN", null);
    assertTrue(administrador.cuenta().isEmpty());
  }
}
