package grupo5.donaciones.seguridad;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import grupo5.common.seguridad.SeguridadAutoConfiguration;
import grupo5.donaciones.controllers.AbstractDonacionesWebMvcTest;
import grupo5.donaciones.dto.NecesidadDTO;
import grupo5.donaciones.dto.Pagina;
import grupo5.donaciones.dto.donaciones.outputs.DonacionOutputDTO;
import grupo5.donaciones.dto.donaciones.outputs.DonanteResumenDTO;
import grupo5.donaciones.dto.donacionesIndependientes.DonacionIndependienteResponseDTO;
import grupo5.donaciones.dto.donantes.DonanteOutputDTO;
import grupo5.donaciones.dto.donantes.PreferenciasDonanteDTO;
import grupo5.donaciones.dto.personas.AceptarLegalesDTO;
import grupo5.donaciones.dto.propuestas.ActualizarEstadoRequestDTO;
import grupo5.donaciones.models.entities.propuestas.EstadoPropuesta;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@TestPropertySource(properties = "donatrack.seguridad.habilitada=true")
@ImportAutoConfiguration(AopAutoConfiguration.class)
@Import({SeguridadAutoConfiguration.class, ReglasDeAccesoDonaciones.class, DuenioDe.class})
class SeguridadDonacionesTest extends AbstractDonacionesWebMvcTest {

  private static final UUID DONANTE = UUID.randomUUID();
  private static final UUID ENTIDAD = UUID.randomUUID();
  private static final UUID AJENO = UUID.randomUUID();
  private static final String NECESIDADES = "/api/necesidades";
  private static final String INDEPENDIENTES = "/donaciones-independientes";
  private static final String SUB_DE_ANA = "6f1c0d2e-sub-de-ana";

  private final ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule());

  @TestConfiguration
  @EnableWebSecurity
  static class Tokens {

    @Bean
    JwtDecoder decodificador() {
      return valor ->
          switch (valor) {
            case "donante" -> token("DONANTE", DONANTE, "VER_DONACIONES", "REGISTRAR_DONACIONES");
            case "entidad" ->
                token(
                    "ENTIDAD",
                    ENTIDAD,
                    "VER_DONACIONES",
                    "VER_NECESIDADES",
                    "GESTIONAR_NECESIDADES",
                    "EDITAR_DATOS_ORGANIZACION");
            case "entidad-sin-permisos" -> token("ENTIDAD", ENTIDAD);
            case "admin" -> token("ADMIN", AJENO);
            case "admin-ana" ->
                Jwt.withTokenValue("t")
                    .header("alg", "none")
                    .subject(SUB_DE_ANA)
                    .claim("roles", List.of("ADMIN"))
                    .build();
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

  // R11-01: los listados devuelven una página; sin datos, una vacía
  @BeforeEach
  void listadosVacios() {
    when(donantesService.buscar(any(), any())).thenReturn(new Pagina<>(List.of(), 0));
    when(donacionesIndependientesService.obtenerConFiltros(any(), any()))
        .thenReturn(new Pagina<>(List.of(), 0));
    when(donacionesService.buscar(any(), any())).thenReturn(new Pagina<>(List.of(), 0));
  }

  private ResultActions pedir(MockHttpServletRequestBuilder pedido, String token) throws Exception {
    return mockMvc.perform(
        token == null ? pedido : pedido.header("Authorization", "Bearer " + token));
  }

  private static NecesidadDTO necesidadDe(UUID entidad) {
    return new NecesidadDTO(
        UUID.randomUUID(), "ALIMENTO", entidad, UUID.randomUUID(), 5, "d", false, null, null);
  }

  private MockHttpServletRequestBuilder conCuerpo(MockHttpServletRequestBuilder pedido, Object dto)
      throws Exception {
    return pedido.contentType(APPLICATION_JSON).content(json.writeValueAsString(dto));
  }

  @Test
  void sinTokenResponde401ConElErrorComun() throws Exception {
    pedir(get(NECESIDADES).param("entidadId", ENTIDAD.toString()), null)
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("ERR-CSR-401"));
  }

  @Test
  void tokenInvalidoResponde401() throws Exception {
    pedir(get(NECESIDADES), "trucho").andExpect(status().isUnauthorized());
  }

  @Test
  void lasRutasPublicasNoPidenToken() throws Exception {
    pedir(get(NECESIDADES + "/publicas"), null).andExpect(status().isOk());
    pedir(get("/api/donaciones/contadores"), null).andExpect(status().isOk());
  }

  @Test
  void laEntidadVeLosPeriodosSoloDeSusNecesidades() throws Exception {
    NecesidadDTO propia = necesidadDe(ENTIDAD);
    NecesidadDTO ajena = necesidadDe(AJENO);
    when(necesidadesService.obtenerPorId(propia.id())).thenReturn(propia);
    when(necesidadesService.obtenerPorId(ajena.id())).thenReturn(ajena);

    pedir(get(NECESIDADES + "/" + propia.id() + "/periodos"), "entidad").andExpect(status().isOk());
    pedir(get(NECESIDADES + "/" + ajena.id() + "/periodos"), "entidad")
        .andExpect(status().isForbidden());
  }

  @Test
  void laEntidadVeSusNecesidades() throws Exception {
    pedir(get(NECESIDADES).param("entidadId", ENTIDAD.toString()), "entidad")
        .andExpect(status().isOk());
  }

  @Test
  void laEntidadNoVeLasNecesidadesDeOtra() throws Exception {
    pedir(get(NECESIDADES).param("entidadId", AJENO.toString()), "entidad")
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ERR-CSR-403"));
  }

  @Test
  void laEntidadDebeIndicarSuIdParaListar() throws Exception {
    pedir(get(NECESIDADES), "entidad").andExpect(status().isForbidden());
  }

  @Test
  void sinElPermisoResponde403AunqueSeaSuOrganizacion() throws Exception {
    pedir(get(NECESIDADES).param("entidadId", ENTIDAD.toString()), "entidad-sin-permisos")
        .andExpect(status().isForbidden());
  }

  @Test
  void elRolEquivocadoResponde403() throws Exception {
    pedir(get(NECESIDADES).param("entidadId", DONANTE.toString()), "donante")
        .andExpect(status().isForbidden());
  }

  @Test
  void elAdminListaLasNecesidadesDeCualquiera() throws Exception {
    pedir(get(NECESIDADES), "admin").andExpect(status().isOk());
  }

  @Test
  void laEntidadPublicaNecesidadesSoloEnSuNombre() throws Exception {
    when(necesidadesService.guardar(any())).thenReturn(necesidadDe(ENTIDAD));

    pedir(conCuerpo(post(NECESIDADES), necesidadDe(ENTIDAD)), "entidad")
        .andExpect(status().isCreated());
    pedir(conCuerpo(post(NECESIDADES), necesidadDe(AJENO)), "entidad")
        .andExpect(status().isForbidden());
  }

  @Test
  void laEntidadCambiaSoloLasNecesidadesQueSonSuyas() throws Exception {
    NecesidadDTO propia = necesidadDe(ENTIDAD);
    NecesidadDTO ajena = necesidadDe(AJENO);
    when(necesidadesService.obtenerPorId(propia.id())).thenReturn(propia);
    when(necesidadesService.obtenerPorId(ajena.id())).thenReturn(ajena);
    when(necesidadesService.actualizar(any(), any())).thenReturn(propia);

    pedir(conCuerpo(put(NECESIDADES + "/" + propia.id()), propia), "entidad")
        .andExpect(status().isOk());
    pedir(conCuerpo(put(NECESIDADES + "/" + ajena.id()), ajena), "entidad")
        .andExpect(status().isForbidden());
  }

  @Test
  void elAdminCambiaLasNecesidadesDeOtraOrganizacion() throws Exception {
    NecesidadDTO ajena = necesidadDe(ENTIDAD);
    when(necesidadesService.actualizar(any(), any())).thenReturn(ajena);

    pedir(conCuerpo(put(NECESIDADES + "/" + ajena.id()), ajena), "admin")
        .andExpect(status().isOk());
  }

  @Test
  void laEntidadDaDeBajaSoloSusNecesidades() throws Exception {
    NecesidadDTO ajena = necesidadDe(AJENO);
    when(necesidadesService.obtenerPorId(ajena.id())).thenReturn(ajena);

    pedir(delete(NECESIDADES + "/" + ajena.id()), "entidad").andExpect(status().isForbidden());
  }

  @Test
  void elDonanteListaSusDonacionesYNoLasDeOtro() throws Exception {
    pedir(get(INDEPENDIENTES).param("donanteId", DONANTE.toString()), "donante")
        .andExpect(status().isOk());
    pedir(get(INDEPENDIENTES).param("donanteId", AJENO.toString()), "donante")
        .andExpect(status().isForbidden());
    pedir(get(INDEPENDIENTES), "donante").andExpect(status().isForbidden());
  }

  @Test
  void laEntidadListaLasDonacionesQueLeAsignaron() throws Exception {
    pedir(get(INDEPENDIENTES).param("entidadId", ENTIDAD.toString()), "entidad")
        .andExpect(status().isOk());
    pedir(get(INDEPENDIENTES).param("entidadId", AJENO.toString()), "entidad")
        .andExpect(status().isForbidden());
  }

  @Test
  void elDonanteVeElDetalleSoloSiLaDonacionOriginalEsSuya() throws Exception {
    UUID propia = detalleDeDonacionDe(DONANTE);
    UUID ajena = detalleDeDonacionDe(AJENO);

    pedir(get(INDEPENDIENTES + "/" + propia), "donante").andExpect(status().isOk());
    pedir(get(INDEPENDIENTES + "/" + ajena), "donante").andExpect(status().isForbidden());
    pedir(get(INDEPENDIENTES + "/" + ajena), "admin").andExpect(status().isOk());
  }

  private UUID detalleDeDonacionDe(UUID donante) {
    UUID original = UUID.randomUUID();
    UUID id = UUID.randomUUID();
    DonanteResumenDTO resumen = new DonanteResumenDTO(donante, donante, null);
    when(donacionesService.obtenerDonacion(original))
        .thenReturn(new DonacionOutputDTO(original, resumen, null, null, null, null, null, null));
    when(donacionesIndependientesService.obtener(id))
        .thenReturn(
            new DonacionIndependienteResponseDTO(
                id, original, null, null, null, null, null, 0, null));
    return id;
  }

  @Test
  void elDonanteVeSuPerfilYNoElDeOtro() throws Exception {
    when(donantesService.obtenerPorId(DONANTE))
        .thenReturn(new DonanteOutputDTO(DONANTE, null, true));

    pedir(get("/api/donantes/" + DONANTE), "donante").andExpect(status().isOk());
    pedir(get("/api/donantes/" + AJENO), "donante").andExpect(status().isForbidden());
  }

  @Test
  void elResumenDelInicioEsSoloDelAdmin_R1101() throws Exception {
    pedir(get("/api/donaciones/resumen"), "donante").andExpect(status().isForbidden());
    pedir(get("/api/donaciones/resumen"), "entidad").andExpect(status().isForbidden());
    pedir(get("/api/donaciones/resumen"), "admin").andExpect(status().isOk());
  }

  @Test
  void loQueNoEstaEnLaMatrizExigeAdmin() throws Exception {
    pedir(get("/api/donantes"), "entidad").andExpect(status().isForbidden());
    pedir(get("/api/donantes"), "servicio").andExpect(status().isForbidden());
    pedir(get("/api/donantes"), "admin").andExpect(status().isOk());
  }

  @Test
  void elServicioCompensaElRegistroPeroUnDonanteNo() throws Exception {
    pedir(delete("/api/personas/" + AJENO), "servicio").andExpect(status().isNoContent());
    pedir(delete("/api/personas/" + AJENO), "donante").andExpect(status().isForbidden());
  }

  @Test
  void lasCategoriasPidenSesionPeroNingunRolEnParticular() throws Exception {
    pedir(get("/api/categorias"), null).andExpect(status().isUnauthorized());
    pedir(get("/api/categorias"), "donante").andExpect(status().isOk());
  }

  @Test
  void elDonanteCambiaSoloSusPreferencias() throws Exception {
    PreferenciasDonanteDTO ocultar = new PreferenciasDonanteDTO(false);

    pedir(conCuerpo(patch("/api/donantes/" + DONANTE + "/preferencias"), ocultar), "donante")
        .andExpect(status().isOk());
    pedir(conCuerpo(patch("/api/donantes/" + AJENO + "/preferencias"), ocultar), "donante")
        .andExpect(status().isForbidden());
    pedir(conCuerpo(patch("/api/donantes/" + DONANTE + "/preferencias"), ocultar), "servicio")
        .andExpect(status().isForbidden());
  }

  @Test
  void losLegalesLosAceptaLaPropiaPersonaOElSitioAlCompletarElPerfil() throws Exception {
    AceptarLegalesDTO version = new AceptarLegalesDTO(1);

    pedir(conCuerpo(put("/api/personas/" + DONANTE + "/legales"), version), "donante")
        .andExpect(status().isOk());
    pedir(conCuerpo(put("/api/personas/" + AJENO + "/legales"), version), "servicio")
        .andExpect(status().isOk());
    pedir(get("/api/personas/" + ENTIDAD + "/legales"), "entidad-sin-permisos")
        .andExpect(status().isOk());
    pedir(get("/api/personas/" + AJENO + "/legales"), "donante").andExpect(status().isForbidden());
    pedir(get("/api/personas/" + DONANTE + "/legales"), null).andExpect(status().isUnauthorized());
  }

  @Test
  void sinElCampoObligatorio_preferenciasYLegalesResponden400() throws Exception {
    pedir(
            patch("/api/donantes/" + DONANTE + "/preferencias")
                .contentType(APPLICATION_JSON)
                .content("{}"),
            "donante")
        .andExpect(status().isBadRequest());
    pedir(
            put("/api/personas/" + DONANTE + "/legales")
                .contentType(APPLICATION_JSON)
                .content("{}"),
            "donante")
        .andExpect(status().isBadRequest());
  }

  // R11-10: aprobar o descartar queda a nombre de la cuenta del token, no de "SISTEMA"
  @Test
  void decidirUnaPropuesta_quedaANombreDelSubDelToken() throws Exception {
    UUID propuesta = UUID.randomUUID();

    pedir(
            conCuerpo(
                put("/api/asignaciones/propuestas/" + propuesta + "/estado"),
                new ActualizarEstadoRequestDTO(EstadoPropuesta.APROBADA, null)),
            "admin-ana")
        .andExpect(status().isOk());

    verify(propuestaDeAsignacionService)
        .actualizarEstado(propuesta, EstadoPropuesta.APROBADA, SUB_DE_ANA);
  }

  @Test
  void decidirUnaPropuesta_esSoloDelAdmin() throws Exception {
    pedir(
            conCuerpo(
                put("/api/asignaciones/propuestas/" + UUID.randomUUID() + "/estado"),
                new ActualizarEstadoRequestDTO(EstadoPropuesta.APROBADA, null)),
            "donante")
        .andExpect(status().isForbidden());
  }
}
