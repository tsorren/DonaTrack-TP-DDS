package grupo5.cliente.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import grupo5.cliente.web.Pantallas;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

class UsuarioActualTest {

  private static final UUID PERSONA = UUID.randomUUID();

  @Test
  void leeRolesPermisosYPersonaDelToken() {
    UsuarioActual usuario =
        UsuarioActual.desde(
            Map.of(
                "roles", List.of("ENTIDAD"),
                "permisos", List.of("VER_NECESIDADES"),
                "personaId", PERSONA.toString()));

    assertThat(usuario)
        .isEqualTo(new UsuarioActual(Set.of("ENTIDAD"), Set.of("VER_NECESIDADES"), PERSONA, null));
    assertThat(usuario.autoridades().stream().map(GrantedAuthority::getAuthority))
        .containsExactlyInAnyOrder("ROLE_ENTIDAD", "VER_NECESIDADES");
  }

  @Test
  void unaCuentaSinPerfilNoTienePersonaNiPermisos() {
    UsuarioActual usuario = UsuarioActual.desde(Map.of("roles", List.of("DONANTE")));

    assertThat(usuario.personaId()).isNull();
    assertThat(usuario.permisos()).isEmpty();
    assertThat(usuario.area()).contains(Area.DONANTE);
  }

  @Test
  void laCuentaDeLaPlataformaNoCompletaPerfil() {
    assertThat(UsuarioActual.desde(Map.of("roles", List.of("ADMIN"))).estado())
        .isEqualTo(EstadoDeCuenta.ACTIVA);
    assertThat(UsuarioActual.desde(Map.of("roles", List.of("DONANTE"))).estado())
        .isEqualTo(EstadoDeCuenta.SIN_PERFIL);
    assertThat(UsuarioActual.desde(Map.of()).estado()).isEqualTo(EstadoDeCuenta.SIN_PERFIL);
  }

  @Test
  void conVariosRolesGanaElAreaDeMasPrecedencia() {
    assertThat(Area.de(Set.of("DONANTE", "ENTIDAD"))).contains(Area.ENTIDAD);
    assertThat(Area.de(Set.of("DONANTE", "ENTIDAD", "ADMIN"))).contains(Area.ADMIN);
    assertThat(Area.de(Set.of("SERVICIO"))).isEmpty();
  }

  @Test
  void cadaAreaEsUnNombreDePantallaDelSitio() {
    for (Area area : Area.values()) {
      assertThat(area.inicio().substring(1)).matches(Pantallas.NOMBRE);
    }
  }

  @Test
  void cadaAreaTieneSuInicioSegunElPrefijoDeSusPantallas() {
    assertThat(Area.deRuta("donante")).contains(Area.DONANTE);
    assertThat(Area.deRuta("publico")).isEmpty();
    assertThat(Area.ADMIN.inicio()).isEqualTo("/admin-inicio");
  }
}
