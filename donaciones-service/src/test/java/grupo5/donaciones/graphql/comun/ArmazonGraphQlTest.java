package grupo5.donaciones.graphql.comun;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLSchema;
import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.common.logging.TraceResponseHeaderFilter;
import grupo5.donaciones.dto.Pagina;
import grupo5.donaciones.dto.publico.EntidadPublicaDTO;
import grupo5.donaciones.seguridad.Acceso;
import grupo5.donaciones.seguridad.TokensDePrueba;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.execution.GraphQlSource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MvcResult;

/**
 * El armazón de GraphQL (SPEC-11, G2, lote {@code g2-armazon}): el esquema aprobado, la ruta, el
 * acceso sin token, el tope de página leído de la propiedad y la traducción de errores. Usa un
 * resolver de prueba sobre las raíces {@code entidades} y {@code donaciones}; los reales los dejan
 * los lotes de cada raíz.
 */
@WebMvcTest(controllers = ArmazonGraphQlTest.ResolverDePrueba.class)
@Import(ArmazonGraphQlTest.ResolverDePrueba.class)
class ArmazonGraphQlTest extends BaseSliceGraphQl {

  private static final UUID ENTIDAD = UUID.fromString("00000000-0000-4000-8000-0000000000b1");
  private static final String ENTIDADES =
      "query($nombre: String, $tamanio: Int, $pagina: Int) {"
          + " entidades(filtro: { nombre: $nombre }, tamanio: $tamanio, pagina: $pagina) {"
          + " total pagina tamanio elementos { id razonSocial } } }";
  private static final String DONACIONES = "{ donaciones { total } }";
  private static final String TRAZA = "traza-de-prueba";

  @Autowired private GraphQlSource graphQlSource;

  /** Resolver de prueba: responde según {@code filtro.nombre} para provocar cada error. */
  @Controller
  public static class ResolverDePrueba {

    private final TopeDePagina tope;

    public ResolverDePrueba(TopeDePagina tope) {
      this.tope = tope;
    }

    @QueryMapping
    @PreAuthorize(Acceso.PUBLICO)
    public PaginaGraphQl<EntidadPublicaDTO> entidades(
        @Argument FiltroDePrueba filtro, @Argument Integer pagina, @Argument Integer tamanio) {
      PedidoDePagina pedido = tope.validar(pagina, tamanio);
      String nombre = filtro.nombre() == null ? "" : filtro.nombre();
      switch (nombre) {
        case "no-encontrado" -> throw new RecursoNoEncontradoException(ENTIDAD);
        case "estado" ->
            throw new BusinessStateException(ErrorCatalog.ENTIDAD_BENEFICIARIA_INACTIVA);
        case "fecha" -> throw new DateTimeParseException("fecha mal escrita", "2026-13-40", 5);
        case "interno" -> throw new IllegalStateException("detalle interno que no debe salir");
        default -> {
          // sin error: devuelve una página
        }
      }
      EntidadPublicaDTO entidad = new EntidadPublicaDTO(ENTIDAD, "Comedor de ejemplo", "Comedor");
      return PaginaGraphQl.de(new Pagina<>(List.of(entidad), 1), pedido, e -> e);
    }

    @QueryMapping
    @PreAuthorize(Acceso.ADMIN)
    public PaginaGraphQl<Object> donaciones(@Argument Integer pagina, @Argument Integer tamanio) {
      return PaginaGraphQl.de(new Pagina<>(List.of(), 0), tope.validar(pagina, tamanio), e -> e);
    }
  }

  public record FiltroDePrueba(String nombre, String categoriaId) {}

  // --- el esquema ---------------------------------------------------------------------------

  @Test
  void cargaElEsquemaAprobado_soloConsultasYSinDatosDePersonas() {
    GraphQLSchema esquema = graphQlSource.schema();

    assertThat(esquema.getQueryType().getFieldDefinitions())
        .extracting(GraphQLFieldDefinition::getName)
        .containsExactlyInAnyOrder("necesidades", "entidades", "donaciones");
    assertThat(esquema.getMutationType()).isNull();
    assertThat(esquema.getSubscriptionType()).isNull();
    assertThat(esquema.getType("Donante")).isNull();
    assertThat(esquema.getObjectType("Donacion").getFieldDefinition("quienDono")).isNull();
    assertThat(esquema.getObjectType("CambioEstado").getFieldDefinition("actor")).isNull();
  }

  // --- la ruta y el acceso ------------------------------------------------------------------

  @Test
  void unVisitanteSinTokenConsultaLaRaizPublica_yRecibeSoloLosCamposPedidos() throws Exception {
    consultar(ENTIDADES, null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.errors").doesNotExist())
        .andExpect(jsonPath("$.data.entidades.total").value(1))
        .andExpect(jsonPath("$.data.entidades.pagina").value(0))
        .andExpect(jsonPath("$.data.entidades.tamanio").value(20))
        .andExpect(jsonPath("$.data.entidades.elementos[0].id").value(ENTIDAD.toString()))
        .andExpect(
            jsonPath("$.data.entidades.elementos[0].razonSocial").value("Comedor de ejemplo"))
        .andExpect(jsonPath("$.data.entidades.elementos[0].rubro").doesNotExist());
  }

  @Test
  void unTokenInvalidoSeCortaEnLaCadenaCon401() throws Exception {
    consultar(ENTIDADES, "trucho").andExpect(status().isUnauthorized());
  }

  @Test
  void sinToken_laRaizConRolVuelveNulaConErr401YElTraceId() throws Exception {
    ejecutar(conTraza(pedido(DONACIONES, Map.of(), null), TRAZA))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.donaciones").isEmpty())
        .andExpect(jsonPath("$.errors[0].path[0]").value("donaciones"))
        .andExpect(jsonPath("$.errors[0].extensions.codigo").value("ERR-CSR-401"))
        .andExpect(jsonPath("$.errors[0].extensions.traceId").value(TRAZA));
  }

  @Test
  void conUnRolQueLaReglaNoAdmite_vuelveErr403() throws Exception {
    ejecutar(conTraza(pedido(DONACIONES, Map.of(), TokensDePrueba.DONANTE), TRAZA))
        .andExpect(jsonPath("$.data.donaciones").isEmpty())
        .andExpect(jsonPath("$.errors[0].message").value("Acceso denegado"))
        .andExpect(jsonPath("$.errors[0].extensions.codigo").value("ERR-CSR-403"))
        .andExpect(jsonPath("$.errors[0].extensions.traceId").value(TRAZA));
    consultar(DONACIONES, TokensDePrueba.ENTIDAD)
        .andExpect(jsonPath("$.errors[0].extensions.codigo").value("ERR-CSR-403"));
  }

  @Test
  void conElRolQueLaReglaPide_resuelveLaRaiz() throws Exception {
    consultar(DONACIONES, TokensDePrueba.ADMINISTRADOR)
        .andExpect(jsonPath("$.errors").doesNotExist())
        .andExpect(jsonPath("$.data.donaciones.total").value(0));
  }

  @Test
  void sinEncabezado_elTraceIdDelErrorEsElDeLaRespuesta() throws Exception {
    MvcResult resultado =
        consultar(DONACIONES, null)
            .andExpect(header().exists(TraceResponseHeaderFilter.TRACE_HEADER))
            .andReturn();
    String traceId = resultado.getResponse().getHeader(TraceResponseHeaderFilter.TRACE_HEADER);

    assertThat(resultado.getResponse().getContentAsString())
        .contains("\"traceId\":\"" + traceId + "\"");
  }

  // --- el tope de página --------------------------------------------------------------------

  @Test
  void elTopeDePaginaSaleDeLaPropiedad_50Entra51No() throws Exception {
    consultar(ENTIDADES, Map.of("tamanio", 50), null)
        .andExpect(jsonPath("$.errors").doesNotExist())
        .andExpect(jsonPath("$.data.entidades.tamanio").value(50));
    consultar(ENTIDADES, Map.of("tamanio", 51), null)
        .andExpect(jsonPath("$.data.entidades").isEmpty())
        .andExpect(jsonPath("$.errors[0].extensions.codigo").value("ERR-CSR-003"));
  }

  @Test
  void unaPaginaNegativaOUnTamanioCeroVuelvenErr003() throws Exception {
    consultar(ENTIDADES, Map.of("pagina", -1), null)
        .andExpect(jsonPath("$.errors[0].extensions.codigo").value("ERR-CSR-003"));
    consultar(ENTIDADES, Map.of("tamanio", 0), null)
        .andExpect(jsonPath("$.errors[0].extensions.codigo").value("ERR-CSR-003"));
  }

  // --- la traducción de los errores del servicio --------------------------------------------

  @Test
  void unRecursoQueNoExisteLlevaSuCodigoDelCatalogo() throws Exception {
    consultar(ENTIDADES, Map.of("nombre", "no-encontrado"), null)
        .andExpect(jsonPath("$.data.entidades").isEmpty())
        .andExpect(jsonPath("$.errors[0].extensions.codigo").value("ERR-CSR-001"))
        .andExpect(jsonPath("$.errors[0].extensions.classification").value("NOT_FOUND"));
  }

  @Test
  void unErrorDeEstadoDelServicioLlevaElCodigoDeLaExcepcion() throws Exception {
    consultar(ENTIDADES, Map.of("nombre", "estado"), null)
        .andExpect(jsonPath("$.errors[0].extensions.codigo").value("ERR-EST-519"));
  }

  @Test
  void unaFechaMalEscritaEsErr003() throws Exception {
    consultar(ENTIDADES, Map.of("nombre", "fecha"), null)
        .andExpect(jsonPath("$.errors[0].extensions.codigo").value("ERR-CSR-003"))
        .andExpect(jsonPath("$.errors[0].extensions.classification").value("BAD_REQUEST"));
  }

  @Test
  void cualquierOtroErrorEsErr500SinElDetalleInterno() throws Exception {
    consultar(ENTIDADES, Map.of("nombre", "interno"), null)
        .andExpect(jsonPath("$.data.entidades").isEmpty())
        .andExpect(jsonPath("$.errors[0].extensions.codigo").value("ERR-CSR-500"))
        .andExpect(jsonPath("$.errors[0].message", not(containsString("detalle interno"))));
  }
}
