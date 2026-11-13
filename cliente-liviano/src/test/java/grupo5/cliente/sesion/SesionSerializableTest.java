package grupo5.cliente.sesion;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;

/**
 * Spring Session JDBC guarda los atributos de la sesión serializados: si algo que va a parar a la
 * sesión no es {@code Serializable}, el ingreso se rompe recién al ejecutar, con una sesión real.
 * Acá se comprueba, sin base de datos, con los tipos que {@code oauth2Login} deja en la sesión: el
 * contexto de seguridad con el usuario OIDC y el cliente autorizado con los tokens.
 */
class SesionSerializableTest {

  private static final Instant AHORA = Instant.parse("2026-11-12T12:00:00Z");
  private static final String PERSONA = "7d5f0c3e-0000-4000-8000-00000000a001";

  private static ClientRegistration registro() {
    return ClientRegistration.withRegistrationId("keycloak")
        .clientId("cliente-liviano")
        .clientSecret("secreto")
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
        .authorizationUri("http://localhost:8180/realms/donatrack/protocol/openid-connect/auth")
        .tokenUri("http://localhost:8180/realms/donatrack/protocol/openid-connect/token")
        .scope("openid")
        .build();
  }

  private static OidcIdToken idToken() {
    return new OidcIdToken(
        "id-token",
        AHORA,
        AHORA.plusSeconds(300),
        Map.of("sub", "cuenta-1", "email", "donante@example.com", "personaId", PERSONA));
  }

  private static OAuth2AuthenticationToken autenticacion() {
    OidcIdToken idToken = idToken();
    DefaultOidcUser usuario =
        new DefaultOidcUser(
            List.of(new OidcUserAuthority(idToken), new SimpleGrantedAuthority("ROLE_DONANTE")),
            idToken);
    return new OAuth2AuthenticationToken(usuario, usuario.getAuthorities(), "keycloak");
  }

  private static OAuth2AuthorizedClient clienteAutorizado() {
    OAuth2AccessToken acceso =
        new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER, "acceso", AHORA, AHORA.plusSeconds(900));
    OAuth2RefreshToken refresco = new OAuth2RefreshToken("refresco", AHORA);
    return new OAuth2AuthorizedClient(registro(), "cuenta-1", acceso, refresco);
  }

  @SuppressWarnings("unchecked")
  private static <T> T viajar(T objeto) throws IOException, ClassNotFoundException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (ObjectOutputStream salida = new ObjectOutputStream(bytes)) {
      salida.writeObject(objeto);
    }
    try (ObjectInputStream entrada =
        new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
      return (T) entrada.readObject();
    }
  }

  @Test
  void elContextoDeSeguridadDeUnIngresoSobreviveAlViaje() throws Exception {
    SecurityContextImpl contexto = new SecurityContextImpl(autenticacion());

    SecurityContextImpl recuperado = viajar(contexto);

    OAuth2AuthenticationToken autenticacion =
        (OAuth2AuthenticationToken) recuperado.getAuthentication();
    OidcUser usuario = (OidcUser) autenticacion.getPrincipal();
    assertThat(autenticacion.getName()).isEqualTo("cuenta-1");
    assertThat(autenticacion.getAuthorizedClientRegistrationId()).isEqualTo("keycloak");
    assertThat(autenticacion.getAuthorities())
        .extracting(Object::toString)
        .contains("ROLE_DONANTE");
    assertThat(usuario.getClaimAsString("personaId")).isEqualTo(PERSONA);
    assertThat(usuario.getIdToken().getTokenValue()).isEqualTo("id-token");
  }

  @Test
  void elClienteAutorizadoConSusTokensSobreviveAlViaje() throws Exception {
    // Así lo deja HttpSessionOAuth2AuthorizedClientRepository: un mapa por id de registro.
    Map<String, OAuth2AuthorizedClient> clientes = new HashMap<>();
    clientes.put("keycloak", clienteAutorizado());

    Map<String, OAuth2AuthorizedClient> recuperados = viajar(clientes);

    OAuth2AuthorizedClient cliente = recuperados.get("keycloak");
    assertThat(cliente.getPrincipalName()).isEqualTo("cuenta-1");
    assertThat(cliente.getClientRegistration().getRegistrationId()).isEqualTo("keycloak");
    assertThat(cliente.getAccessToken().getTokenValue()).isEqualTo("acceso");
    assertThat(cliente.getRefreshToken().getTokenValue()).isEqualTo("refresco");
  }

  @Test
  void loQueElSitioGuardaPorSuCuentaEnLaSesionEsSerializable() throws Exception {
    // CuentaPorParametro y CuentaDeEjemplo guardan un UUID o un texto, y los flash attributes que
    // deja el sitio entre un POST y su redirección son un texto (el aviso), un long (el número del
    // mensaje de contacto), un boolean ("guardado") y la contraseña provisoria cifrada, que es un
    // texto: todos tipos del JDK.
    UUID id = UUID.randomUUID();
    String provisoria =
        new CifradoDeLaSesion("", false).cifrar("K7pQ-m4Rt-x9Wa-2Lse".getBytes(UTF_8));
    Map<String, Object> atributos = new HashMap<>();
    atributos.put("cuenta", id);
    atributos.put("perfil", "pendiente");
    atributos.put("numeroDelMensaje", 4821L);
    atributos.put("guardado", true);
    atributos.put("provisoria", provisoria);

    Map<String, Object> recuperados = viajar(atributos);

    assertThat(recuperados)
        .containsEntry("cuenta", id)
        .containsEntry("perfil", "pendiente")
        .containsEntry("numeroDelMensaje", 4821L)
        .containsEntry("guardado", true)
        .containsEntry("provisoria", provisoria);
  }
}
