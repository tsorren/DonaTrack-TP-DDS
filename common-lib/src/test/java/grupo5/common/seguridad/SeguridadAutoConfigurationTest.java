package grupo5.common.seguridad;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.common.CommonLibAutoConfiguration;
import jakarta.servlet.Filter;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

class SeguridadAutoConfigurationTest {

  private static final UUID ORGANIZACION = UUID.randomUUID();

  private final WebApplicationContextRunner contexto =
      new WebApplicationContextRunner()
          .withConfiguration(
              AutoConfigurations.of(
                  CommonLibAutoConfiguration.class, SeguridadAutoConfiguration.class))
          .withUserConfiguration(ServicioDePrueba.class);

  @Configuration(proxyBeanMethods = false)
  @EnableWebMvc
  @EnableWebSecurity
  static class ServicioDePrueba {

    @Bean
    ReglasDeAcceso reglas() {
      return () -> List.of("/publica");
    }

    @Bean
    JwtDecoder tokens() {
      return valor ->
          switch (valor) {
            case "miembro" -> ActoresTest.token("ENTIDAD", ORGANIZACION, "VER_COSAS");
            case "sin-permisos" -> ActoresTest.token("ENTIDAD", ORGANIZACION);
            case "admin" -> ActoresTest.token("ADMIN", ORGANIZACION);
            default -> throw new BadJwtException("token desconocido");
          };
    }

    @Bean
    Recursos recursos() {
      return new Recursos();
    }
  }

  @RestController
  static class Recursos {

    @GetMapping("/publica")
    String publica() {
      return "publica";
    }

    @GetMapping("/cosas/{duenio}")
    @PreAuthorize("hasAuthority('VER_COSAS') or hasRole('ADMIN')")
    String cosas(@ActorActual Actor actor, @PathVariable UUID duenio) {
      return actor.getClass().getSimpleName() + ":" + actor.puedeOperarSobre(duenio);
    }
  }

  private static MockMvc mvc(WebApplicationContext ctx) {
    return MockMvcBuilders.webAppContextSetup(ctx)
        .addFilters(ctx.getBean("springSecurityFilterChain", Filter.class))
        .build();
  }

  private static MockHttpServletRequestBuilder cosasDe(UUID duenio, String token) {
    return get("/cosas/" + duenio).header("Authorization", "Bearer " + token);
  }

  private void conSeguridad(MockHttpServletRequestBuilder pedido, ResultMatcher... esperado) {
    contexto
        .withPropertyValues("donatrack.seguridad.habilitada=true")
        .run(ctx -> mvc(ctx).perform(pedido).andExpectAll(esperado));
  }

  @Test
  void habilitadaDejaPasarLasRutasPublicasSinToken() {
    conSeguridad(get("/publica"), status().isOk());
  }

  @Test
  void habilitadaSinTokenResponde401ConElErrorComun() {
    conSeguridad(
        get("/cosas/" + ORGANIZACION),
        status().isUnauthorized(),
        jsonPath("$.code").value("ERR-CSR-401"));
  }

  @Test
  void habilitadaConTokenInvalidoResponde401() {
    conSeguridad(
        cosasDe(ORGANIZACION, "trucho"),
        status().isUnauthorized(),
        jsonPath("$.code").value("ERR-CSR-401"));
  }

  @Test
  void habilitadaSinElPermisoResponde403ConElErrorComun() {
    conSeguridad(
        cosasDe(ORGANIZACION, "sin-permisos"),
        status().isForbidden(),
        jsonPath("$.code").value("ERR-CSR-403"));
  }

  @Test
  void habilitadaConElPermisoInyectaAlMiembroSobreSuOrganizacion() {
    conSeguridad(cosasDe(ORGANIZACION, "miembro"), content().string("Miembro:true"));
  }

  @Test
  void habilitadaElMiembroNoPerteneceAOtraOrganizacion() {
    conSeguridad(cosasDe(UUID.randomUUID(), "miembro"), content().string("Miembro:false"));
  }

  @Test
  void habilitadaElAdminOperaSobreOtraOrganizacion() {
    conSeguridad(cosasDe(UUID.randomUUID(), "admin"), content().string("Administrador:true"));
  }

  @Test
  void sinPropiedadDejaPasarTodoComoAdministrador() {
    contexto.run(
        ctx ->
            mvc(ctx)
                .perform(get("/cosas/" + UUID.randomUUID()))
                .andExpectAll(status().isOk(), content().string("Administrador:true")));
  }
}
