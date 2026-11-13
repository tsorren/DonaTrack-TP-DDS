package grupo5.incentivos.seguridad;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.common.CommonLibAutoConfiguration;
import grupo5.common.logging.LoggingAutoConfiguration;
import grupo5.common.seguridad.SeguridadAutoConfiguration;
import grupo5.incentivos.controllers.DonanteIncentivosController;
import grupo5.incentivos.controllers.InsigniasController;
import grupo5.incentivos.controllers.MetricasIncentivosController;
import grupo5.incentivos.controllers.MisionesDonacionController;
import grupo5.incentivos.controllers.ProcesosIncentivosController;
import grupo5.incentivos.controllers.RankingController;
import grupo5.incentivos.dto.HistorialDeRankings;
import grupo5.incentivos.services.IGestionDonanteService;
import grupo5.incentivos.services.IInactividadService;
import grupo5.incentivos.services.IInsigniasService;
import grupo5.incentivos.services.IMetricasIncentivosService;
import grupo5.incentivos.services.IMisionesDonacionService;
import grupo5.incentivos.services.IRankingService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Autorización de Incentivos con la seguridad encendida (matriz de permisos, §7): sin token es 401;
 * con token, el rol, el permiso y el dueño del recurso. Los servicios van simulados, así que un
 * 200, un 204 o un 201 quiere decir "pasó la autorización", y un 403, que no.
 */
@WebMvcTest({
  DonanteIncentivosController.class,
  MisionesDonacionController.class,
  InsigniasController.class,
  MetricasIncentivosController.class,
  RankingController.class,
  ProcesosIncentivosController.class
})
@Import({
  CommonLibAutoConfiguration.class,
  LoggingAutoConfiguration.class,
  SeguridadAutoConfiguration.class
})
@ImportAutoConfiguration(AopAutoConfiguration.class)
@TestPropertySource(properties = "donatrack.seguridad.habilitada=true")
class SeguridadIncentivosTest {

  private static final UUID DONANTE = UUID.randomUUID();
  private static final UUID AJENO = UUID.randomUUID();
  private static final String INCENTIVOS = "/api/incentivos";

  @Autowired private MockMvc mockMvc;

  @MockitoBean private IGestionDonanteService gestionDonanteService;
  @MockitoBean private IMisionesDonacionService misionesDonacionService;
  @MockitoBean private IInsigniasService insigniasService;
  @MockitoBean private IMetricasIncentivosService metricasIncentivosService;
  @MockitoBean private IRankingService rankingService;
  @MockitoBean private IInactividadService inactividadService;

  @TestConfiguration
  @EnableWebSecurity
  static class Tokens {

    @Bean
    JwtDecoder decodificador() {
      return valor ->
          switch (valor) {
            case "donante" -> token("DONANTE", DONANTE, "VER_INCENTIVOS", "CONFIGURAR_INSIGNIAS");
            case "donante-solo-ver" -> token("DONANTE", DONANTE, "VER_INCENTIVOS");
            case "donante-sin-permisos" -> token("DONANTE", DONANTE);
            case "entidad" -> token("ENTIDAD", DONANTE, "VER_INCENTIVOS", "CONFIGURAR_INSIGNIAS");
            case "admin" -> token("ADMIN", AJENO);
            case "servicio" -> token("SERVICIO", AJENO);
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

  private ResultActions pedir(MockHttpServletRequestBuilder pedido, String token) throws Exception {
    return mockMvc.perform(
        token == null ? pedido : pedido.header("Authorization", "Bearer " + token));
  }

  private static String metricasDe(UUID donante) {
    return INCENTIVOS + "/donantes/" + donante + "/metricas";
  }

  @Test
  void sinTokenResponde401ConElErrorComun() throws Exception {
    pedir(get(metricasDe(DONANTE)), null)
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("ERR-CSR-401"));
  }

  @Test
  void tokenInvalidoResponde401() throws Exception {
    pedir(get(metricasDe(DONANTE)), "trucho").andExpect(status().isUnauthorized());
  }

  @Test
  void elDonanteVeSusIncentivos() throws Exception {
    pedir(get(metricasDe(DONANTE)), "donante").andExpect(status().isOk());
    pedir(get(INCENTIVOS + "/donantes/" + DONANTE + "/insignias"), "donante")
        .andExpect(status().isOk());
    pedir(get(INCENTIVOS + "/donantes/" + DONANTE + "/misiones"), "donante")
        .andExpect(status().isOk());
    pedir(get(INCENTIVOS + "/ranking/posicion/" + DONANTE), "donante")
        .andExpect(status().isNoContent());
  }

  @Test
  void elDonanteNoVeLosIncentivosDeOtro() throws Exception {
    pedir(get(metricasDe(AJENO)), "donante").andExpect(status().isForbidden());
    pedir(get(INCENTIVOS + "/donantes/" + AJENO + "/insignias"), "donante")
        .andExpect(status().isForbidden());
    pedir(get(INCENTIVOS + "/donantes/" + AJENO + "/misiones"), "donante")
        .andExpect(status().isForbidden());
    pedir(get(INCENTIVOS + "/donantes/" + AJENO), "donante").andExpect(status().isForbidden());
    pedir(get(INCENTIVOS + "/ranking/posicion/" + AJENO), "donante")
        .andExpect(status().isForbidden());
  }

  @Test
  void sinElPermisoResponde403AunqueSeaSuId() throws Exception {
    pedir(get(metricasDe(DONANTE)), "donante-sin-permisos").andExpect(status().isForbidden());
  }

  @Test
  void elRolEquivocadoResponde403AunqueTengaElPermiso() throws Exception {
    pedir(get(metricasDe(DONANTE)), "entidad").andExpect(status().isForbidden());
  }

  @Test
  void elAdminVeLosIncentivosDeCualquiera() throws Exception {
    pedir(get(metricasDe(DONANTE)), "admin").andExpect(status().isOk());
    pedir(get(INCENTIVOS + "/donantes/" + DONANTE + "/misiones"), "admin")
        .andExpect(status().isOk());
  }

  @Test
  void elRankingGeneralPideRolYPermisoPeroNoDueno() throws Exception {
    when(rankingService.obtenerHistorial(0, 12)).thenReturn(new HistorialDeRankings(List.of(), 0));
    pedir(get(INCENTIVOS + "/ranking/historial"), "donante").andExpect(status().isOk());
    pedir(get(INCENTIVOS + "/ranking/ultimo"), "donante").andExpect(status().isNoContent());
    pedir(get(INCENTIVOS + "/ranking/historial"), "donante-sin-permisos")
        .andExpect(status().isForbidden());
    pedir(get(INCENTIVOS + "/ranking/historial"), "entidad").andExpect(status().isForbidden());
  }

  @Test
  void configurarInsigniasPideSuPropioPermiso() throws Exception {
    String visibilidad = INCENTIVOS + "/donantes/" + DONANTE + "/insignias/PRIMERA/visibilidad";
    pedir(put(visibilidad).param("visible", "true"), "donante").andExpect(status().isOk());
    pedir(put(visibilidad).param("visible", "true"), "donante-solo-ver")
        .andExpect(status().isForbidden());
  }

  @Test
  void elDonanteNoConfiguraLasInsigniasDeOtro() throws Exception {
    String ajena = INCENTIVOS + "/donantes/" + AJENO + "/insignias/PRIMERA/visibilidad";
    pedir(put(ajena).param("visible", "true"), "donante").andExpect(status().isForbidden());
  }

  @Test
  void elDonanteNoCalculaElRankingNiCorreLosProcesos() throws Exception {
    pedir(post(INCENTIVOS + "/ranking/calcular"), "donante").andExpect(status().isForbidden());
    pedir(post(INCENTIVOS + "/evaluaciones-inactividad"), "donante")
        .andExpect(status().isForbidden());
    pedir(post(INCENTIVOS + "/verificaciones-racha"), "donante").andExpect(status().isForbidden());
  }

  @Test
  void elServicioYElAdminCorrenLosProcesos() throws Exception {
    for (String quien : List.of("servicio", "admin")) {
      pedir(post(INCENTIVOS + "/ranking/calcular"), quien).andExpect(status().isOk());
      pedir(post(INCENTIVOS + "/evaluaciones-inactividad"), quien).andExpect(status().isOk());
      pedir(post(INCENTIVOS + "/verificaciones-racha"), quien).andExpect(status().isOk());
    }
  }

  @Test
  void soloElServicioYElAdminDanDeBajaUnDonante() throws Exception {
    pedir(delete(INCENTIVOS + "/donantes/" + DONANTE), "donante").andExpect(status().isForbidden());
    pedir(delete(INCENTIVOS + "/donantes/" + DONANTE), "servicio")
        .andExpect(status().isNoContent());
    pedir(delete(INCENTIVOS + "/donantes/" + DONANTE), "admin").andExpect(status().isNoContent());
  }

  @Test
  void elResumenDelSistemaEsSoloDelAdmin() throws Exception {
    String resumen = INCENTIVOS + "/admin/resumen";
    pedir(get(resumen), "donante").andExpect(status().isForbidden());
    pedir(get(resumen), "servicio").andExpect(status().isForbidden());
    pedir(get(resumen), "admin").andExpect(status().isOk());
  }

  @Test
  void laCuentaDeServicioNoLeeIncentivosDeUnDonante() throws Exception {
    pedir(get(metricasDe(DONANTE)), "servicio").andExpect(status().isForbidden());
  }
}
