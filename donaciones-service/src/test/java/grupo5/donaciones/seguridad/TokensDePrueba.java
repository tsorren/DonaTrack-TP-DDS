package grupo5.donaciones.seguridad;

import java.util.List;
import java.util.UUID;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/**
 * {@link JwtDecoder} falso y reutilizable para los slices con la seguridad encendida (SPEC-11, G2).
 * Cada valor de token es un actor sintético: {@link #DONANTE}, {@link #ENTIDAD} y {@link
 * #ADMINISTRADOR}. Cualquier otro valor es un token inválido y da 401.
 *
 * <p>Se importa con {@code @Import(TokensDePrueba.class)} junto con {@code
 * SeguridadAutoConfiguration} y {@code donatrack.seguridad.habilitada=true}.
 */
@TestConfiguration
@EnableWebSecurity
public class TokensDePrueba {

  /** Token de un donante con permiso para ver y registrar sus donaciones. */
  public static final String DONANTE = "donante";

  /** Token de una entidad beneficiaria con permiso para ver sus donaciones y necesidades. */
  public static final String ENTIDAD = "entidad";

  /** Token de un administrador. */
  public static final String ADMINISTRADOR = "administrador";

  public static final UUID DONANTE_ID = UUID.fromString("00000000-0000-4000-8000-0000000000d1");
  public static final UUID ENTIDAD_ID = UUID.fromString("00000000-0000-4000-8000-0000000000e1");
  public static final UUID ADMINISTRADOR_ID =
      UUID.fromString("00000000-0000-4000-8000-0000000000a1");

  @Bean
  JwtDecoder decodificadorDePrueba() {
    return valor ->
        switch (valor) {
          case DONANTE -> token("DONANTE", DONANTE_ID, "VER_DONACIONES", "REGISTRAR_DONACIONES");
          case ENTIDAD -> token("ENTIDAD", ENTIDAD_ID, "VER_DONACIONES", "VER_NECESIDADES");
          case ADMINISTRADOR -> token("ADMIN", ADMINISTRADOR_ID);
          default -> throw new BadJwtException("token desconocido");
        };
  }

  private static Jwt token(String rol, UUID personaId, String... permisos) {
    return Jwt.withTokenValue("t")
        .header("alg", "none")
        .claim("roles", List.of(rol))
        .claim("personaId", personaId.toString())
        .claim("permisos", List.of(permisos))
        .build();
  }
}
