package grupo5.donaciones.seguridad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

/** El actor de la auditoría: el {@code sub} del token manda; sin token vale el encabezado. */
class AuditoriaDelActorTest {

  private static final String SUB = "7d5f0c3e-0000-4000-8000-00000000a001";

  private static Authentication conToken(Jwt token) {
    return new PreAuthenticatedAuthenticationToken("actor", token, List.of());
  }

  private static Jwt tokenConSub(String sub) {
    return Jwt.withTokenValue("t").header("alg", "none").subject(sub).build();
  }

  private static Jwt tokenSinSub() {
    return Jwt.withTokenValue("t").header("alg", "none").claim("roles", List.of("ADMIN")).build();
  }

  @Test
  void conTokenEsElSubDeLaCuenta() {
    assertEquals(SUB, AuditoriaDelActor.de(conToken(tokenConSub(SUB)), null));
  }

  @Test
  void conTokenElEncabezadoNoPuedeSuplantarAlActor() {
    assertEquals(SUB, AuditoriaDelActor.de(conToken(tokenConSub(SUB)), "intruso"));
  }

  @Test
  void sinAutenticacionVaOElEncabezadoLegado() {
    assertEquals("juan", AuditoriaDelActor.de(null, "juan"));
  }

  @Test
  void unaAutenticacionAnonimaNoEsUnaCuentaYVaOElEncabezado() {
    Authentication anonima =
        new AnonymousAuthenticationToken(
            "clave", "anonimo", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));

    assertEquals("juan", AuditoriaDelActor.de(anonima, "juan"));
  }

  @Test
  void unaCredencialQueNoEsUnTokenNoCuenta() {
    Authentication conClave = new UsernamePasswordAuthenticationToken("usuario", "clave");

    assertEquals("juan", AuditoriaDelActor.de(conClave, "juan"));
  }

  @Test
  void unTokenSinSubONoAlcanzaYVaOElEncabezado() {
    assertEquals("juan", AuditoriaDelActor.de(conToken(tokenSinSub()), "juan"));
    assertEquals("juan", AuditoriaDelActor.de(conToken(tokenConSub("   ")), "juan"));
  }

  @Test
  void sinTokenNiEncabezadoElActorEsObligatorio() {
    Authentication autenticacionSinSub = conToken(tokenSinSub());

    IllegalArgumentException sinNada =
        assertThrows(IllegalArgumentException.class, () -> AuditoriaDelActor.de(null, null));
    IllegalArgumentException enBlanco =
        assertThrows(IllegalArgumentException.class, () -> AuditoriaDelActor.de(null, "  "));
    IllegalArgumentException tokenSinSub =
        assertThrows(
            IllegalArgumentException.class, () -> AuditoriaDelActor.de(autenticacionSinSub, null));

    assertTrue(sinNada.getMessage().contains("X-Actor"));
    assertTrue(enBlanco.getMessage().contains("X-Actor"));
    assertTrue(tokenSinSub.getMessage().contains("X-Actor"));
  }
}
