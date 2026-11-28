package grupo5.donaciones.graphql.necesidades;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.donaciones.dto.Pagina;
import grupo5.donaciones.dto.publico.NecesidadPublicaDTO;
import grupo5.donaciones.graphql.comun.BaseSliceGraphQl;
import grupo5.donaciones.models.entities.necesidades.TipoNecesidad;
import grupo5.donaciones.seguridad.Acceso;
import grupo5.donaciones.seguridad.TokensDePrueba;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MvcResult;

/**
 * La consulta raíz {@code necesidades} (SPEC-11, G2, lote {@code g2-necesidades}): pública, sin
 * token y con la seguridad encendida; los cuatro filtros, la página con su tope y los campos del
 * tipo {@code Necesidad}. Lee por {@code INecesidadesService.buscarPublicas}, simulado.
 */
@WebMvcTest(controllers = NecesidadesResolver.class)
@Import(NecesidadesResolver.class)
class NecesidadesResolverTest extends BaseSliceGraphQl {

  private static final UUID NECESIDAD = UUID.fromString("00000000-0000-4000-8000-0000000000a1");
  private static final UUID RECURRENTE = UUID.fromString("00000000-0000-4000-8000-0000000000a2");
  private static final UUID ENTIDAD = UUID.fromString("00000000-0000-4000-8000-0000000000b1");
  private static final UUID CATEGORIA = UUID.fromString("00000000-0000-4000-8000-000000000001");
  private static final UUID SUBCATEGORIA = UUID.fromString("00000000-0000-4000-8000-000000000011");
  private static final LocalDate FIN = LocalDate.of(2026, 12, 31);

  private static final String CON_FILTROS =
      "query($entidad: ID, $categoria: ID, $subcategoria: ID, $tipo: TipoNecesidad,"
          + " $pagina: Int, $tamanio: Int) {"
          + " necesidades(filtro: { entidadId: $entidad, categoriaId: $categoria,"
          + " subcategoriaId: $subcategoria, tipo: $tipo }, pagina: $pagina, tamanio: $tamanio) {"
          + " total pagina tamanio"
          + " elementos { id entidadId subcategoriaId descripcion cantidadNecesitada tipo fechaFin"
          + " } } }";
  private static final String SIN_ARGUMENTOS =
      "{ necesidades { total pagina tamanio elementos { id descripcion } } }";

  private static NecesidadPublicaDTO extraordinaria() {
    return new NecesidadPublicaDTO(
        NECESIDAD, ENTIDAD, SUBCATEGORIA, "Frazadas para el invierno", 40, "EXTRAORDINARIA", null);
  }

  private static NecesidadPublicaDTO recurrente() {
    return new NecesidadPublicaDTO(
        RECURRENTE, ENTIDAD, SUBCATEGORIA, "Leche todas las semanas", 12, "RECURRENTE", FIN);
  }

  private void responde(NecesidadPublicaDTO... necesidades) {
    when(necesidadesService.buscarPublicas(any(), any(), any(), any(), anyInt(), anyInt()))
        .thenReturn(new Pagina<>(List.of(necesidades), 7));
  }

  private static Map<String, Object> variables(Object... claveValor) {
    Map<String, Object> variables = new HashMap<>();
    for (int i = 0; i < claveValor.length; i += 2) {
      variables.put((String) claveValor[i], claveValor[i + 1]);
    }
    return variables;
  }

  @Test
  void unVisitanteSinTokenRecibeLaPaginaConLosCamposDelTipo() throws Exception {
    responde(extraordinaria(), recurrente());

    consultar(CON_FILTROS, null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.errors").doesNotExist())
        .andExpect(jsonPath("$.data.necesidades.total").value(7))
        .andExpect(jsonPath("$.data.necesidades.pagina").value(0))
        .andExpect(jsonPath("$.data.necesidades.tamanio").value(20))
        .andExpect(jsonPath("$.data.necesidades.elementos[0].id").value(NECESIDAD.toString()))
        .andExpect(jsonPath("$.data.necesidades.elementos[0].entidadId").value(ENTIDAD.toString()))
        .andExpect(
            jsonPath("$.data.necesidades.elementos[0].subcategoriaId")
                .value(SUBCATEGORIA.toString()))
        .andExpect(
            jsonPath("$.data.necesidades.elementos[0].descripcion")
                .value("Frazadas para el invierno"))
        .andExpect(jsonPath("$.data.necesidades.elementos[0].cantidadNecesitada").value(40))
        .andExpect(jsonPath("$.data.necesidades.elementos[0].tipo").value("EXTRAORDINARIA"))
        .andExpect(jsonPath("$.data.necesidades.elementos[0].fechaFin").isEmpty())
        .andExpect(jsonPath("$.data.necesidades.elementos[1].tipo").value("RECURRENTE"))
        .andExpect(jsonPath("$.data.necesidades.elementos[1].fechaFin").value("2026-12-31"));
  }

  @Test
  void conToken_tambienResponde() throws Exception {
    responde(extraordinaria());

    consultar(SIN_ARGUMENTOS, TokensDePrueba.DONANTE)
        .andExpect(jsonPath("$.errors").doesNotExist())
        .andExpect(jsonPath("$.data.necesidades.total").value(7));
  }

  @Test
  void devuelveSoloLosCamposPedidos() throws Exception {
    responde(extraordinaria());

    consultar(SIN_ARGUMENTOS, null)
        .andExpect(jsonPath("$.data.necesidades.elementos[0].id").value(NECESIDAD.toString()))
        .andExpect(jsonPath("$.data.necesidades.elementos[0].entidadId").doesNotExist())
        .andExpect(jsonPath("$.data.necesidades.elementos[0].cantidadNecesitada").doesNotExist());
  }

  @Test
  void sinArgumentos_noFiltraYPideLaPrimeraPaginaDeVeinte() throws Exception {
    responde();

    consultar(SIN_ARGUMENTOS, null)
        .andExpect(jsonPath("$.data.necesidades.pagina").value(0))
        .andExpect(jsonPath("$.data.necesidades.tamanio").value(20))
        .andExpect(jsonPath("$.data.necesidades.elementos").isEmpty());

    verify(necesidadesService).buscarPublicas(null, null, null, null, 0, 20);
  }

  @Test
  void pasaLosCuatroFiltrosYLaPaginaAlServicio() throws Exception {
    responde();

    consultar(
            CON_FILTROS,
            variables(
                "entidad",
                ENTIDAD.toString(),
                "categoria",
                CATEGORIA.toString(),
                "subcategoria",
                SUBCATEGORIA.toString(),
                "tipo",
                "RECURRENTE",
                "pagina",
                3,
                "tamanio",
                5),
            null)
        .andExpect(jsonPath("$.errors").doesNotExist())
        .andExpect(jsonPath("$.data.necesidades.pagina").value(3))
        .andExpect(jsonPath("$.data.necesidades.tamanio").value(5));

    verify(necesidadesService)
        .buscarPublicas(ENTIDAD, CATEGORIA, SUBCATEGORIA, TipoNecesidad.RECURRENTE, 3, 5);
  }

  @Test
  void unTamanioFueraDelTopeOUnaPaginaNegativaEsErr003SinLlamarAlServicio() throws Exception {
    consultar(CON_FILTROS, variables("tamanio", 51), null)
        .andExpect(jsonPath("$.data.necesidades").isEmpty())
        .andExpect(jsonPath("$.errors[0].path[0]").value("necesidades"))
        .andExpect(jsonPath("$.errors[0].extensions.codigo").value("ERR-CSR-003"));
    consultar(CON_FILTROS, variables("pagina", -1), null)
        .andExpect(jsonPath("$.errors[0].extensions.codigo").value("ERR-CSR-003"));

    verify(necesidadesService, never())
        .buscarPublicas(any(), any(), any(), any(), anyInt(), anyInt());
  }

  @Test
  void unIdMalEscritoEsErr003SinLlamarAlServicio() throws Exception {
    consultar(CON_FILTROS, variables("categoria", "no-es-un-uuid"), null)
        .andExpect(jsonPath("$.data.necesidades").isEmpty())
        .andExpect(jsonPath("$.errors[0].extensions.codigo").value("ERR-CSR-003"));

    verify(necesidadesService, never())
        .buscarPublicas(any(), any(), any(), any(), anyInt(), anyInt());
  }

  @Test
  void unTokenInvalidoSeCortaEnLaCadenaCon401() throws Exception {
    MvcResult resultado = consultar(SIN_ARGUMENTOS, "trucho").andReturn();

    assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
  }

  @Test
  void laRaizLlevaLaReglaPublica() throws Exception {
    PreAuthorize regla =
        NecesidadesResolver.class
            .getMethod("necesidades", FiltroNecesidadesGraphQl.class, Integer.class, Integer.class)
            .getAnnotation(PreAuthorize.class);

    assertThat(regla).isNotNull();
    assertThat(regla.value()).isEqualTo(Acceso.PUBLICO);
  }
}
