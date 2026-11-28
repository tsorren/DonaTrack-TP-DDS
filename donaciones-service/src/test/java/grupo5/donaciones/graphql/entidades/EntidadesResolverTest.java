package grupo5.donaciones.graphql.entidades;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.dto.Pagina;
import grupo5.donaciones.dto.publico.EntidadPublicaDTO;
import grupo5.donaciones.graphql.comun.BaseSliceGraphQl;
import grupo5.donaciones.seguridad.TokensDePrueba;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;

/**
 * {@code Query.entidades} (SPEC-11, G2, lote {@code g2-entidades}): los filtros y la página llegan
 * a {@code buscarPublicas}, la respuesta trae solo los campos pedidos y se consulta sin token con
 * la seguridad encendida.
 */
@WebMvcTest(controllers = EntidadesResolver.class)
class EntidadesResolverTest extends BaseSliceGraphQl {

  private static final UUID ENTIDAD = UUID.fromString("00000000-0000-4000-8000-0000000000c1");
  private static final UUID OTRA = UUID.fromString("00000000-0000-4000-8000-0000000000c2");
  private static final UUID CATEGORIA = UUID.fromString("00000000-0000-4000-8000-0000000000d1");

  private static final String CONSULTA =
      "query($filtro: FiltroEntidades!, $pagina: Int, $tamanio: Int) {"
          + " entidades(filtro: $filtro, pagina: $pagina, tamanio: $tamanio) {"
          + " total pagina tamanio elementos { id razonSocial rubro } } }";
  private static final String SIN_ARGUMENTOS = "{ entidades { total elementos { id } } }";
  private static final String SOLO_NOMBRES = "{ entidades { elementos { razonSocial } } }";

  private static final EntidadPublicaDTO COMEDOR =
      new EntidadPublicaDTO(ENTIDAD, "Comedor de ejemplo", "Comedor");
  private static final EntidadPublicaDTO HOGAR =
      new EntidadPublicaDTO(OTRA, "Hogar de ejemplo", null);

  // --- acceso -------------------------------------------------------------------------------

  @Test
  void unVisitanteSinTokenConsultaLasEntidades() throws Exception {
    when(entidadBeneficiariaService.buscarPublicas(null, null, 0, 20))
        .thenReturn(new Pagina<>(List.of(COMEDOR), 1));

    consultar(SIN_ARGUMENTOS, null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.errors").doesNotExist())
        .andExpect(jsonPath("$.data.entidades.total").value(1))
        .andExpect(jsonPath("$.data.entidades.elementos[0].id").value(ENTIDAD.toString()));
  }

  @Test
  void conTokenDeCualquierRolTambienSeConsulta() throws Exception {
    when(entidadBeneficiariaService.buscarPublicas(null, null, 0, 20))
        .thenReturn(new Pagina<>(List.of(COMEDOR), 1));

    consultar(SIN_ARGUMENTOS, TokensDePrueba.DONANTE)
        .andExpect(jsonPath("$.errors").doesNotExist())
        .andExpect(jsonPath("$.data.entidades.total").value(1));
    consultar(SIN_ARGUMENTOS, TokensDePrueba.ENTIDAD)
        .andExpect(jsonPath("$.errors").doesNotExist())
        .andExpect(jsonPath("$.data.entidades.total").value(1));
  }

  // --- filtros ------------------------------------------------------------------------------

  @Test
  void sinFiltroNiPagina_pideLaPrimeraPaginaDe20SinFiltrar() throws Exception {
    when(entidadBeneficiariaService.buscarPublicas(null, null, 0, 20))
        .thenReturn(new Pagina<>(List.of(), 0));

    consultar(CONSULTA, Map.of("filtro", Map.of()), null)
        .andExpect(jsonPath("$.errors").doesNotExist())
        .andExpect(jsonPath("$.data.entidades.total").value(0))
        .andExpect(jsonPath("$.data.entidades.pagina").value(0))
        .andExpect(jsonPath("$.data.entidades.tamanio").value(20))
        .andExpect(jsonPath("$.data.entidades.elementos").isEmpty());

    verify(entidadBeneficiariaService).buscarPublicas(null, null, 0, 20);
  }

  @Test
  void elNombreYLaCategoriaLleganABuscarPublicas() throws Exception {
    when(entidadBeneficiariaService.buscarPublicas("comedor", CATEGORIA, 0, 20))
        .thenReturn(new Pagina<>(List.of(COMEDOR), 1));

    consultar(
            CONSULTA,
            Map.of("filtro", Map.of("nombre", "comedor", "categoriaId", CATEGORIA.toString())),
            null)
        .andExpect(jsonPath("$.errors").doesNotExist())
        .andExpect(jsonPath("$.data.entidades.total").value(1))
        .andExpect(
            jsonPath("$.data.entidades.elementos[0].razonSocial").value("Comedor de ejemplo"));

    verify(entidadBeneficiariaService).buscarPublicas("comedor", CATEGORIA, 0, 20);
  }

  @Test
  void soloElNombre_laCategoriaQuedaNula() throws Exception {
    when(entidadBeneficiariaService.buscarPublicas("hogar", null, 0, 20))
        .thenReturn(new Pagina<>(List.of(HOGAR), 1));

    consultar(CONSULTA, Map.of("filtro", Map.of("nombre", "hogar")), null)
        .andExpect(jsonPath("$.errors").doesNotExist())
        .andExpect(jsonPath("$.data.entidades.elementos[0].id").value(OTRA.toString()));

    verify(entidadBeneficiariaService).buscarPublicas("hogar", null, 0, 20);
  }

  @Test
  void unaCategoriaQueNoEsUnUuidEsErr003_sinLlamarAlServicio() throws Exception {
    consultar(CONSULTA, Map.of("filtro", Map.of("categoriaId", "no-es-un-uuid")), null)
        .andExpect(jsonPath("$.data.entidades").isEmpty())
        .andExpect(jsonPath("$.errors[0].path[0]").value("entidades"))
        .andExpect(jsonPath("$.errors[0].extensions.codigo").value("ERR-CSR-003"));

    verify(entidadBeneficiariaService, never()).buscarPublicas(any(), any(), any(), any());
  }

  @Test
  void unNombreQueElServicioRechazaVuelveConSuCodigo() throws Exception {
    String largo = "a".repeat(101);
    when(entidadBeneficiariaService.buscarPublicas(largo, null, 0, 20))
        .thenThrow(new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO));

    consultar(CONSULTA, Map.of("filtro", Map.of("nombre", largo)), null)
        .andExpect(jsonPath("$.data.entidades").isEmpty())
        .andExpect(jsonPath("$.errors[0].extensions.codigo").value("ERR-CSR-003"));
  }

  // --- página -------------------------------------------------------------------------------

  @Test
  void laPaginaYElTamanioPedidosLleganAlServicioYVuelvenEnLaRespuesta() throws Exception {
    when(entidadBeneficiariaService.buscarPublicas(null, null, 2, 5))
        .thenReturn(new Pagina<>(List.of(COMEDOR, HOGAR), 12));

    consultar(CONSULTA, Map.of("filtro", Map.of(), "pagina", 2, "tamanio", 5), null)
        .andExpect(jsonPath("$.errors").doesNotExist())
        .andExpect(jsonPath("$.data.entidades.total").value(12))
        .andExpect(jsonPath("$.data.entidades.pagina").value(2))
        .andExpect(jsonPath("$.data.entidades.tamanio").value(5))
        .andExpect(jsonPath("$.data.entidades.elementos.length()").value(2))
        .andExpect(jsonPath("$.data.entidades.elementos[1].razonSocial").value("Hogar de ejemplo"))
        .andExpect(jsonPath("$.data.entidades.elementos[1].rubro").isEmpty());

    verify(entidadBeneficiariaService).buscarPublicas(null, null, 2, 5);
  }

  @Test
  void unTamanioPorEncimaDelTopeEsErr003_sinLlamarAlServicio() throws Exception {
    consultar(CONSULTA, Map.of("filtro", Map.of(), "tamanio", 51), null)
        .andExpect(jsonPath("$.data.entidades").isEmpty())
        .andExpect(jsonPath("$.errors[0].extensions.codigo").value("ERR-CSR-003"));

    verify(entidadBeneficiariaService, never()).buscarPublicas(any(), any(), any(), any());
  }

  @Test
  void unaPaginaNegativaEsErr003_sinLlamarAlServicio() throws Exception {
    consultar(CONSULTA, Map.of("filtro", Map.of(), "pagina", -1), null)
        .andExpect(jsonPath("$.data.entidades").isEmpty())
        .andExpect(jsonPath("$.errors[0].extensions.codigo").value("ERR-CSR-003"));

    verify(entidadBeneficiariaService, never()).buscarPublicas(any(), any(), any(), any());
  }

  // --- campos -------------------------------------------------------------------------------

  @Test
  void devuelveSoloLosCamposPedidos() throws Exception {
    when(entidadBeneficiariaService.buscarPublicas(null, null, 0, 20))
        .thenReturn(new Pagina<>(List.of(COMEDOR), 1));

    consultar(SOLO_NOMBRES, null)
        .andExpect(jsonPath("$.errors").doesNotExist())
        .andExpect(
            jsonPath("$.data.entidades.elementos[0].razonSocial").value("Comedor de ejemplo"))
        .andExpect(jsonPath("$.data.entidades.elementos[0].id").doesNotExist())
        .andExpect(jsonPath("$.data.entidades.elementos[0].rubro").doesNotExist())
        .andExpect(jsonPath("$.data.entidades.total").doesNotExist());
  }

  @Test
  void unCampoDeUnaPersonaNoExisteEnElTipoEntidad() throws Exception {
    consultar("{ entidades { elementos { documento direccion } } }", null)
        .andExpect(jsonPath("$.data").doesNotExist())
        .andExpect(jsonPath("$.errors[0].extensions.classification").value("ValidationError"));

    verify(entidadBeneficiariaService, never()).buscarPublicas(any(), any(), any(), any());
  }
}
