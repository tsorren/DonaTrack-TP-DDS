package grupo5.common.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class ActoresTest {

  enum PermisoDePrueba implements Permiso {
    VER_COSAS,
    BORRAR_COSAS
  }

  private static final UUID ORGANIZACION = UUID.randomUUID();

  static Jwt token(String rol, UUID personaId, String... permisos) {
    Jwt.Builder token = Jwt.withTokenValue("token").header("alg", "none");
    token.claim("roles", List.of(rol)).claim("personaId", personaId.toString());
    return token.claim("permisos", List.of(permisos)).build();
  }

  @Test
  void tokenDeMiembroArmaUnMiembroConSuPersonaYSusPermisos() {
    Actor actor = Actores.desde(token("ENTIDAD", ORGANIZACION, "VER_COSAS"));

    assertThat(actor).isEqualTo(new Miembro(ORGANIZACION, Set.of("VER_COSAS")));
    assertThat(actor.tiene(PermisoDePrueba.VER_COSAS)).isTrue();
    assertThat(actor.tiene(PermisoDePrueba.BORRAR_COSAS)).isFalse();
  }

  @Test
  void tokenConRolAdminArmaUnAdministrador() {
    assertThat(Actores.desde(token("ADMIN", ORGANIZACION))).isInstanceOf(Administrador.class);
  }

  @Test
  void tokenSinClaimsArmaUnMiembroSinPersonaNiPermisos() {
    Actor actor = Actores.desde(Jwt.withTokenValue("t").header("alg", "none").subject("x").build());

    assertThat(actor.tiene(PermisoDePrueba.VER_COSAS)).isFalse();
    assertThat(actor.puedeOperarSobre(ORGANIZACION)).isFalse();
  }

  @Test
  void miembroOperaSoloSobreSuPersona() {
    Miembro miembro = new Miembro(ORGANIZACION, Set.of());

    assertThat(miembro.puedeOperarSobre(ORGANIZACION)).isTrue();
    assertThat(miembro.puedeOperarSobre(UUID.randomUUID())).isFalse();
    assertThat(miembro.puedeOperarSobre(null)).isFalse();
  }

  @Test
  void miembroSinPersonaNoOperaNiSobreUnDuenioNulo() {
    assertThat(new Miembro(null, Set.of()).puedeOperarSobre(null)).isFalse();
  }

  @Test
  void administradorOperaSobreCualquierOrganizacionYTieneTodosLosPermisos() {
    Administrador admin = new Administrador();

    assertThat(admin.puedeOperarSobre(UUID.randomUUID())).isTrue();
    assertThat(admin.tiene(PermisoDePrueba.BORRAR_COSAS)).isTrue();
  }

  @Test
  void rolesQuedanConPrefijoYPermisosTalCual() {
    var autoridades = Actores.autoridades(token("ENTIDAD", ORGANIZACION, "VER_COSAS"));

    assertThat(autoridades)
        .extracting(GrantedAuthority::getAuthority)
        .containsExactly("ROLE_ENTIDAD", "VER_COSAS");
  }
}
