package grupo5.donaciones.graphql.relaciones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.donaciones.dto.Pagina;
import grupo5.donaciones.dto.donacionesIndependientes.CambioEstadoDIResponseDTO;
import grupo5.donaciones.dto.donacionesIndependientes.DonacionIndependienteResponseDTO;
import grupo5.donaciones.dto.donacionesIndependientes.QuienDonoDTO;
import grupo5.donaciones.dto.publico.EntidadPublicaDTO;
import grupo5.donaciones.dto.publico.NecesidadPublicaDTO;
import grupo5.donaciones.graphql.comun.BaseSliceGraphQl;
import grupo5.donaciones.graphql.donaciones.DonacionGraphQl;
import grupo5.donaciones.graphql.donaciones.DonacionesResolver;
import grupo5.donaciones.graphql.necesidades.NecesidadesResolver;
import grupo5.donaciones.seguridad.Acceso;
import grupo5.donaciones.seguridad.TokensDePrueba;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * {@code Donacion.necesidad} y {@code Donacion.entidad} (SPEC-11, G2, lote {@code g2-relaciones}):
 * cargadas por lotes con una sola lectura al servicio por página, con la regla {@code CON_SESION}
 * puesta en cada método y la seguridad encendida, y sin datos de una persona (contrato de GraphQL,
 * §4.5 y §4.6).
 *
 * <p>Que la página llegue sin errores con un token de entidad prueba que el contexto de seguridad
 * del pedido llega al {@code @BatchMapping}: si no llegara, {@code isAuthenticated()} fallaría.
 */
@WebMvcTest(
    controllers = {
      DonacionesResolver.class,
      NecesidadesResolver.class,
      RelacionesDeDonacion.class,
      RelacionesDeNecesidad.class
    })
class RelacionesDeDonacionTest extends BaseSliceGraphQl {

  private static final UUID ENTIDAD = TokensDePrueba.ENTIDAD_ID;
  private static final UUID OTRA_ENTIDAD = UUID.fromString("00000000-0000-4000-8000-0000000000b2");
  private static final UUID NECESIDAD = UUID.fromString("00000000-0000-4000-8000-0000000000a1");
  private static final UUID OTRA_NECESIDAD =
      UUID.fromString("00000000-0000-4000-8000-0000000000a2");
  private static final UUID SUBCATEGORIA = UUID.fromString("00000000-0000-4000-8000-000000000011");
  private static final UUID DONANTE = UUID.fromString("00000000-0000-4000-8000-0000000000f1");
  private static final String QUIEN_DONO = "Persona Sintetica Que No Sale";
  private static final String ACTOR = "actor-sintetico-que-no-sale";
  private static final String JUSTIFICACION = "justificacion-sintetica-que-no-sale";

  private static final Map<String, Object> DE_MI_ENTIDAD =
      Map.of("filtro", Map.of("entidadId", ENTIDAD.toString()));

  private static final String CON_RELACIONES =
      "query($filtro: FiltroDonaciones!) { donaciones(filtro: $filtro) { elementos { id"
          + " necesidad { id descripcion cantidadNecesitada tipo fechaFin entidadId }"
          + " entidad { id razonSocial rubro } } } }";

  private static final String HASTA_LA_ENTIDAD_DE_LA_NECESIDAD =
      "query($filtro: FiltroDonaciones!) { donaciones(filtro: $filtro) { elementos { id"
          + " necesidad { id entidad { id razonSocial } } } } }";

  @Autowired private RelacionesDeDonacion relaciones;

  private static DonacionIndependienteResponseDTO donacion(
      String id, UUID entidad, UUID necesidad) {
    return new DonacionIndependienteResponseDTO(
        UUID.fromString(id),
        UUID.randomUUID(),
        "Donacion sintetica",
        "EnTraslado",
        LocalDateTime.of(2026, 11, 2, 10, 30),
        List.of(
            new CambioEstadoDIResponseDTO(
                null, "EnDeposito", LocalDateTime.of(2026, 11, 2, 10, 30), JUSTIFICACION, ACTOR)),
        List.of(),
        3,
        entidad,
        necesidad,
        false,
        new QuienDonoDTO("PERSONA", QUIEN_DONO, DONANTE));
  }

  @BeforeEach
  void unaPaginaDeCuatroDonaciones() {
    when(donacionesIndependientesService.obtenerConFiltros(any(), any()))
        .thenReturn(
            new Pagina<>(
                List.of(
                    donacion("00000000-0000-4000-8000-0000000000c1", ENTIDAD, NECESIDAD),
                    donacion("00000000-0000-4000-8000-0000000000c2", ENTIDAD, OTRA_NECESIDAD),
                    donacion("00000000-0000-4000-8000-0000000000c3", OTRA_ENTIDAD, NECESIDAD),
                    // Sin asignar y sin necesidad: las dos relaciones quedan nulas
                    donacion("00000000-0000-4000-8000-0000000000c4", null, null)),
                4));
    when(necesidadesService.buscarPublicasPorIds(any()))
        .thenReturn(
            List.of(
                new NecesidadPublicaDTO(
                    NECESIDAD, ENTIDAD, SUBCATEGORIA, "Frazadas", 40, "EXTRAORDINARIA", null),
                new NecesidadPublicaDTO(
                    OTRA_NECESIDAD, OTRA_ENTIDAD, SUBCATEGORIA, "Leche", 12, "RECURRENTE", null)));
    List<EntidadPublicaDTO> entidades =
        List.of(
            new EntidadPublicaDTO(ENTIDAD, "Comedor de ejemplo", "Comedor"),
            new EntidadPublicaDTO(OTRA_ENTIDAD, "Hogar de ejemplo", "Hogar"));
    when(entidadBeneficiariaService.buscarPublicasPorIds(any())).thenReturn(entidades);
    // Necesidad.entidad lee solo las activas (la misma vista que GET /api/entidades/publicas)
    when(entidadBeneficiariaService.buscarActivasPorIds(any())).thenReturn(entidades);
  }

  @AfterEach
  void limpiarContexto() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void conElTokenDeLaEntidad_resuelveLasDosRelaciones() throws Exception {
    consultar(CON_RELACIONES, DE_MI_ENTIDAD, TokensDePrueba.ENTIDAD)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.errors").doesNotExist())
        .andExpect(
            jsonPath("$.data.donaciones.elementos[0].necesidad.id").value(NECESIDAD.toString()))
        .andExpect(
            jsonPath("$.data.donaciones.elementos[0].necesidad.descripcion").value("Frazadas"))
        .andExpect(
            jsonPath("$.data.donaciones.elementos[0].necesidad.tipo").value("EXTRAORDINARIA"))
        .andExpect(
            jsonPath("$.data.donaciones.elementos[0].entidad.razonSocial")
                .value("Comedor de ejemplo"))
        .andExpect(
            jsonPath("$.data.donaciones.elementos[1].necesidad.id")
                .value(OTRA_NECESIDAD.toString()))
        .andExpect(
            jsonPath("$.data.donaciones.elementos[2].entidad.id").value(OTRA_ENTIDAD.toString()))
        .andExpect(jsonPath("$.data.donaciones.elementos[3].necesidad").isEmpty())
        .andExpect(jsonPath("$.data.donaciones.elementos[3].entidad").isEmpty());
  }

  @Test
  void elAdministradorTambienLasRecibe() throws Exception {
    consultar(CON_RELACIONES, Map.of("filtro", Map.of()), TokensDePrueba.ADMINISTRADOR)
        .andExpect(jsonPath("$.errors").doesNotExist())
        .andExpect(
            jsonPath("$.data.donaciones.elementos[0].necesidad.id").value(NECESIDAD.toString()));
  }

  @Test
  @SuppressWarnings("unchecked")
  void unaSolaLlamadaPorRelacionYPorPagina_sinIdsNulosNiRepetidos() throws Exception {
    consultar(CON_RELACIONES, DE_MI_ENTIDAD, TokensDePrueba.ENTIDAD)
        .andExpect(jsonPath("$.errors").doesNotExist());

    ArgumentCaptor<Collection<UUID>> necesidades = ArgumentCaptor.forClass(Collection.class);
    ArgumentCaptor<Collection<UUID>> entidades = ArgumentCaptor.forClass(Collection.class);
    verify(necesidadesService, times(1)).buscarPublicasPorIds(necesidades.capture());
    verify(entidadBeneficiariaService, times(1)).buscarPublicasPorIds(entidades.capture());
    assertThat(necesidades.getValue()).containsExactlyInAnyOrder(NECESIDAD, OTRA_NECESIDAD);
    assertThat(entidades.getValue()).containsExactlyInAnyOrder(ENTIDAD, OTRA_ENTIDAD);
  }

  @Test
  @SuppressWarnings("unchecked")
  void laEntidadDeLaNecesidadTambienSaleEnUnaSolaLlamada() throws Exception {
    consultar(HASTA_LA_ENTIDAD_DE_LA_NECESIDAD, DE_MI_ENTIDAD, TokensDePrueba.ENTIDAD)
        .andExpect(jsonPath("$.errors").doesNotExist())
        .andExpect(
            jsonPath("$.data.donaciones.elementos[0].necesidad.entidad.razonSocial")
                .value("Comedor de ejemplo"))
        .andExpect(
            jsonPath("$.data.donaciones.elementos[1].necesidad.entidad.id")
                .value(OTRA_ENTIDAD.toString()));

    ArgumentCaptor<Collection<UUID>> entidades = ArgumentCaptor.forClass(Collection.class);
    verify(necesidadesService, times(1)).buscarPublicasPorIds(any());
    verify(entidadBeneficiariaService, times(1)).buscarActivasPorIds(entidades.capture());
    verify(entidadBeneficiariaService, never()).buscarPublicasPorIds(any());
    assertThat(entidades.getValue()).containsExactlyInAnyOrder(ENTIDAD, OTRA_ENTIDAD);
  }

  @Test
  void sinTokenNiLaRaizNiLasRelacionesLlamanAlServicio() throws Exception {
    consultar(CON_RELACIONES, DE_MI_ENTIDAD, null)
        .andExpect(jsonPath("$.data.donaciones").isEmpty())
        .andExpect(jsonPath("$.errors[0].extensions.codigo").value("ERR-CSR-401"));

    verify(necesidadesService, never()).buscarPublicasPorIds(any());
    verify(entidadBeneficiariaService, never()).buscarPublicasPorIds(any());
  }

  @Test
  void laReglaDeCadaRelacionSeAplica_sinSesionElMetodoNoLee() {
    List<DonacionGraphQl> pagina = List.of();
    SecurityContextHolder.clearContext();

    assertThatThrownBy(() -> relaciones.necesidad(pagina))
        .isInstanceOfAny(AuthenticationException.class, AccessDeniedException.class);
    assertThatThrownBy(() -> relaciones.entidad(pagina))
        .isInstanceOfAny(AuthenticationException.class, AccessDeniedException.class);

    verify(necesidadesService, never()).buscarPublicasPorIds(any());
    verify(entidadBeneficiariaService, never()).buscarPublicasPorIds(any());
  }

  @Test
  void lasRelacionesSonBatchMappingConLaReglaConSesion() throws Exception {
    for (String campo : List.of("necesidad", "entidad")) {
      Method metodo = RelacionesDeDonacion.class.getMethod(campo, List.class);
      BatchMapping mapeo = metodo.getAnnotation(BatchMapping.class);
      PreAuthorize regla = metodo.getAnnotation(PreAuthorize.class);

      assertThat(mapeo).isNotNull();
      assertThat(mapeo.typeName()).isEqualTo("Donacion");
      assertThat(regla).isNotNull();
      assertThat(regla.value()).isEqualTo(Acceso.CON_SESION);
    }
  }

  @Test
  void ningunaRelacionExponeDatosDeUnaPersona() throws Exception {
    consultar(CON_RELACIONES, DE_MI_ENTIDAD, TokensDePrueba.ENTIDAD)
        .andExpect(jsonPath("$.errors").doesNotExist())
        .andExpect(content().string(not(containsString(QUIEN_DONO))))
        .andExpect(content().string(not(containsString(DONANTE.toString()))))
        .andExpect(content().string(not(containsString(ACTOR))))
        .andExpect(content().string(not(containsString(JUSTIFICACION))));

    for (String consulta :
        List.of(
            "{ donaciones { elementos { entidad { documento } } } }",
            "{ donaciones { elementos { entidad { direccion } } } }",
            "{ donaciones { elementos { necesidad { donaciones { id } } } } }",
            "{ donaciones { elementos { necesidad { entidad { representantes } } } } }",
            "{ donaciones { elementos { quienDono { nombre } } } }")) {
      consultar(consulta, TokensDePrueba.ADMINISTRADOR)
          .andExpect(jsonPath("$.data").doesNotExist())
          .andExpect(jsonPath("$.errors[0].extensions.classification").value("ValidationError"));
    }
  }
}
