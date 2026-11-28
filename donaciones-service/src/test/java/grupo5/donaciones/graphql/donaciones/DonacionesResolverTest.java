package grupo5.donaciones.graphql.donaciones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.donaciones.dto.Pagina;
import grupo5.donaciones.dto.donacionesIndependientes.BienNormalizadoDTO;
import grupo5.donaciones.dto.donacionesIndependientes.BienResumenDTO;
import grupo5.donaciones.dto.donacionesIndependientes.CambioEstadoDIResponseDTO;
import grupo5.donaciones.dto.donacionesIndependientes.CategoriaResumenDTO;
import grupo5.donaciones.dto.donacionesIndependientes.DonacionIndependienteResponseDTO;
import grupo5.donaciones.dto.donacionesIndependientes.FiltroDonacionesIndependientes;
import grupo5.donaciones.dto.donacionesIndependientes.ItemDonacionIndependienteResponseDTO;
import grupo5.donaciones.dto.donacionesIndependientes.QuienDonoDTO;
import grupo5.donaciones.dto.donacionesIndependientes.SubcategoriaResumenDTO;
import grupo5.donaciones.graphql.comun.BaseSliceGraphQl;
import grupo5.donaciones.models.entities.donaciones.Estado;
import grupo5.donaciones.models.entities.donacionesIndependientes.TipoEstadoDonacion;
import grupo5.donaciones.seguridad.TokensDePrueba;
import grupo5.donaciones.services.ParametrosListado;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;

/**
 * La consulta {@code donaciones} (SPEC-11, G2, lote {@code g2-donaciones}): los filtros y la página
 * que llegan a {@code obtenerConFiltros}, los campos del tipo {@code Donacion} sin datos de
 * personas y la matriz de acceso con la seguridad encendida (D08, D15).
 */
@WebMvcTest(controllers = DonacionesResolver.class)
class DonacionesResolverTest extends BaseSliceGraphQl {

  private static final UUID DONACION = UUID.fromString("00000000-0000-4000-8000-0000000000c1");
  private static final UUID ORIGINAL = UUID.fromString("00000000-0000-4000-8000-0000000000c2");
  private static final UUID NECESIDAD = UUID.fromString("00000000-0000-4000-8000-0000000000c3");
  private static final UUID SUBCATEGORIA = UUID.fromString("00000000-0000-4000-8000-0000000000c4");
  private static final UUID CATEGORIA = UUID.fromString("00000000-0000-4000-8000-0000000000c5");
  private static final UUID AJENO = UUID.fromString("00000000-0000-4000-8000-0000000000f1");
  private static final String ACTOR = "actor-sintetico-que-no-sale";
  private static final String JUSTIFICACION = "justificacion-sintetica-que-no-sale";
  private static final String QUIEN_DONO = "Persona Sintetica";

  private static final String TOTAL =
      "query($filtro: FiltroDonaciones!) { donaciones(filtro: $filtro) { total } }";

  private static final String COMPLETA =
      "query($filtro: FiltroDonaciones!, $pagina: Int, $tamanio: Int, $orden: OrdenDonaciones) {"
          + " donaciones(filtro: $filtro, pagina: $pagina, tamanio: $tamanio, orden: $orden) {"
          + " total pagina tamanio elementos {"
          + " id descripcion estado fechaRegistro cantidad cancelacionPedida entidadId necesidadId"
          + " items { cantidad bien { descripcion fotoUrl fechaVencimiento estado }"
          + " subcategoria { id nombre } categoria { id nombre unidad } }"
          + " historial { estadoAnterior estadoNuevo fecha } } } }";

  @BeforeEach
  void unaDonacion() {
    when(donacionesIndependientesService.obtenerConFiltros(any(), any()))
        .thenReturn(new Pagina<>(List.of(donacion()), 41));
  }

  private static DonacionIndependienteResponseDTO donacion() {
    BienNormalizadoDTO bien =
        new BienNormalizadoDTO(
            new BienResumenDTO("Frazadas", "foto.jpg", LocalDate.of(2027, 1, 31), Estado.NUEVO),
            new SubcategoriaResumenDTO(SUBCATEGORIA, "Abrigo"),
            new CategoriaResumenDTO(CATEGORIA, "Textiles", "unidades"));
    return new DonacionIndependienteResponseDTO(
        DONACION,
        ORIGINAL,
        "Frazadas para el invierno",
        "EnTraslado",
        LocalDateTime.of(2026, 11, 2, 10, 30, 0, 123_000_000),
        List.of(
            new CambioEstadoDIResponseDTO(
                null, "EnDeposito", LocalDateTime.of(2026, 11, 2, 10, 30), JUSTIFICACION, ACTOR),
            new CambioEstadoDIResponseDTO(
                "EnDeposito",
                "EnTraslado",
                LocalDateTime.of(2026, 11, 3, 8, 0),
                JUSTIFICACION,
                ACTOR)),
        List.of(new ItemDonacionIndependienteResponseDTO(bien, 4)),
        4,
        TokensDePrueba.ENTIDAD_ID,
        NECESIDAD,
        true,
        new QuienDonoDTO("PERSONA", QUIEN_DONO, AJENO));
  }

  private static Map<String, Object> filtro(String clave, Object valor) {
    Map<String, Object> filtro = new HashMap<>();
    filtro.put(clave, valor);
    return Map.of("filtro", filtro);
  }

  private static Map<String, Object> sinFiltro() {
    return Map.of("filtro", Map.of());
  }

  // --- la matriz de acceso (D08) --------------------------------------------------------------

  @Test
  void sinToken_noRecibeDonaciones_err401() throws Exception {
    consultar(TOTAL, sinFiltro(), null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.donaciones").isEmpty())
        .andExpect(jsonPath("$.errors[0].extensions.codigo").value("ERR-CSR-401"));
    consultar(TOTAL, filtro("donanteId", TokensDePrueba.DONANTE_ID.toString()), null)
        .andExpect(jsonPath("$.errors[0].extensions.codigo").value("ERR-CSR-401"));
    verify(donacionesIndependientesService, never()).obtenerConFiltros(any(), any());
  }

  @Test
  void unDonante_recibeLasSuyas() throws Exception {
    consultar(
            TOTAL,
            filtro("donanteId", TokensDePrueba.DONANTE_ID.toString()),
            TokensDePrueba.DONANTE)
        .andExpect(jsonPath("$.errors").doesNotExist())
        .andExpect(jsonPath("$.data.donaciones.total").value(41));
  }

  @Test
  void unDonante_noRecibeLasDeOtraPersonaNiSinFiltro_err403() throws Exception {
    consultar(TOTAL, filtro("donanteId", AJENO.toString()), TokensDePrueba.DONANTE)
        .andExpect(jsonPath("$.data.donaciones").isEmpty())
        .andExpect(jsonPath("$.errors[0].extensions.codigo").value("ERR-CSR-403"));
    consultar(TOTAL, sinFiltro(), TokensDePrueba.DONANTE)
        .andExpect(jsonPath("$.errors[0].extensions.codigo").value("ERR-CSR-403"));
    consultar("{ donaciones { total } }", TokensDePrueba.DONANTE)
        .andExpect(jsonPath("$.errors[0].extensions.codigo").value("ERR-CSR-403"));
    consultar(
            TOTAL,
            filtro("entidadId", TokensDePrueba.DONANTE_ID.toString()),
            TokensDePrueba.DONANTE)
        .andExpect(jsonPath("$.errors[0].extensions.codigo").value("ERR-CSR-403"));
    verify(donacionesIndependientesService, never()).obtenerConFiltros(any(), any());
  }

  @Test
  void unaEntidad_recibeLasSuyas() throws Exception {
    consultar(
            TOTAL,
            filtro("entidadId", TokensDePrueba.ENTIDAD_ID.toString()),
            TokensDePrueba.ENTIDAD)
        .andExpect(jsonPath("$.errors").doesNotExist())
        .andExpect(jsonPath("$.data.donaciones.total").value(41));
  }

  @Test
  void unaEntidad_noRecibeLasDeOtraEntidadNiSinFiltro_err403() throws Exception {
    consultar(TOTAL, filtro("entidadId", AJENO.toString()), TokensDePrueba.ENTIDAD)
        .andExpect(jsonPath("$.data.donaciones").isEmpty())
        .andExpect(jsonPath("$.errors[0].extensions.codigo").value("ERR-CSR-403"));
    consultar(TOTAL, sinFiltro(), TokensDePrueba.ENTIDAD)
        .andExpect(jsonPath("$.errors[0].extensions.codigo").value("ERR-CSR-403"));
    consultar(
            TOTAL,
            filtro("donanteId", TokensDePrueba.ENTIDAD_ID.toString()),
            TokensDePrueba.ENTIDAD)
        .andExpect(jsonPath("$.errors[0].extensions.codigo").value("ERR-CSR-403"));
    verify(donacionesIndependientesService, never()).obtenerConFiltros(any(), any());
  }

  @Test
  void elAdministrador_recibeTodasSinFiltro() throws Exception {
    consultar("{ donaciones { total } }", TokensDePrueba.ADMINISTRADOR)
        .andExpect(jsonPath("$.errors").doesNotExist())
        .andExpect(jsonPath("$.data.donaciones.total").value(41));
    consultar(TOTAL, filtro("entidadId", AJENO.toString()), TokensDePrueba.ADMINISTRADOR)
        .andExpect(jsonPath("$.errors").doesNotExist());
  }

  // --- los filtros ----------------------------------------------------------------------------

  @Test
  void todosLosFiltrosLleganAObtenerConFiltros() throws Exception {
    Map<String, Object> filtro = new HashMap<>();
    filtro.put("donanteId", AJENO.toString());
    filtro.put("entidadId", TokensDePrueba.ENTIDAD_ID.toString());
    filtro.put("estado", List.of("EN_TRASLADO", "EN_PUERTA"));
    filtro.put("necesidadId", NECESIDAD.toString());
    filtro.put("subcategoriaId", SUBCATEGORIA.toString());
    filtro.put("categoriaId", CATEGORIA.toString());
    filtro.put("desde", "2026-11-01");
    filtro.put("hasta", "2026-11-30");
    filtro.put("venceHasta", "2027-01-31");
    filtro.put("id", DONACION.toString());
    filtro.put("texto", "  frazada  ");

    consultar(
            COMPLETA,
            Map.of("filtro", filtro, "pagina", 2, "tamanio", 5, "orden", "ATENCION"),
            TokensDePrueba.ADMINISTRADOR)
        .andExpect(jsonPath("$.errors").doesNotExist());

    ArgumentCaptor<FiltroDonacionesIndependientes> filtros =
        ArgumentCaptor.forClass(FiltroDonacionesIndependientes.class);
    ArgumentCaptor<ParametrosListado> listados = ArgumentCaptor.forClass(ParametrosListado.class);
    verify(donacionesIndependientesService)
        .obtenerConFiltros(filtros.capture(), listados.capture());
    assertThat(filtros.getValue())
        .isEqualTo(
            new FiltroDonacionesIndependientes(
                List.of(TipoEstadoDonacion.EN_TRASLADO, TipoEstadoDonacion.EN_PUERTA),
                SUBCATEGORIA,
                CATEGORIA,
                AJENO,
                TokensDePrueba.ENTIDAD_ID,
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 30),
                NECESIDAD,
                DONACION,
                LocalDate.of(2027, 1, 31)));
    assertThat(listados.getValue()).isEqualTo(new ParametrosListado(2, 5, "frazada", "ATENCION"));
  }

  @Test
  void sinFiltrosNiArgumentos_pideLaPrimeraPaginaDe20EnOrdenRecientes() throws Exception {
    consultar("{ donaciones { total } }", TokensDePrueba.ADMINISTRADOR)
        .andExpect(jsonPath("$.errors").doesNotExist());

    ArgumentCaptor<FiltroDonacionesIndependientes> filtros =
        ArgumentCaptor.forClass(FiltroDonacionesIndependientes.class);
    ArgumentCaptor<ParametrosListado> listados = ArgumentCaptor.forClass(ParametrosListado.class);
    verify(donacionesIndependientesService)
        .obtenerConFiltros(filtros.capture(), listados.capture());
    assertThat(filtros.getValue())
        .isEqualTo(
            new FiltroDonacionesIndependientes(
                null, null, null, null, null, null, null, null, null, null));
    assertThat(listados.getValue()).isEqualTo(new ParametrosListado(0, 20, null, "RECIENTES"));
  }

  @Test
  void unaFechaMalEscrita_err003SinLlamarAlServicio() throws Exception {
    consultar(TOTAL, filtro("desde", "2026-13-40"), TokensDePrueba.ADMINISTRADOR)
        .andExpect(jsonPath("$.data.donaciones").isEmpty())
        .andExpect(jsonPath("$.errors[0].extensions.codigo").value("ERR-CSR-003"));
    consultar(TOTAL, filtro("venceHasta", "31/01/2027"), TokensDePrueba.ADMINISTRADOR)
        .andExpect(jsonPath("$.errors[0].extensions.codigo").value("ERR-CSR-003"));
    verify(donacionesIndependientesService, never()).obtenerConFiltros(any(), any());
  }

  @Test
  void unIdMalEscrito_noLlamaAlServicio() throws Exception {
    consultar(TOTAL, filtro("entidadId", "no-es-un-uuid"), TokensDePrueba.ADMINISTRADOR)
        .andExpect(jsonPath("$.data.donaciones").isEmpty())
        .andExpect(jsonPath("$.errors[0].extensions.codigo").value("ERR-CSR-003"));
    consultar(TOTAL, filtro("donanteId", "no-es-un-uuid"), TokensDePrueba.DONANTE)
        .andExpect(jsonPath("$.data.donaciones").isEmpty())
        .andExpect(jsonPath("$.errors[0].extensions.codigo").value("ERR-CSR-003"));
    verify(donacionesIndependientesService, never()).obtenerConFiltros(any(), any());
  }

  @Test
  void unTextoDeMasDe100Caracteres_err003() throws Exception {
    consultar(TOTAL, filtro("texto", "x".repeat(101)), TokensDePrueba.ADMINISTRADOR)
        .andExpect(jsonPath("$.data.donaciones").isEmpty())
        .andExpect(jsonPath("$.errors[0].extensions.codigo").value("ERR-CSR-003"));
    verify(donacionesIndependientesService, never()).obtenerConFiltros(any(), any());
  }

  @Test
  void unEstadoQueNoEstaEnElEsquema_esRechazadoPorLaValidacion() throws Exception {
    consultar(TOTAL, filtro("estado", List.of("EnTraslado")), TokensDePrueba.ADMINISTRADOR)
        .andExpect(jsonPath("$.errors[0].extensions.classification").value("ValidationError"));
    verify(donacionesIndependientesService, never()).obtenerConFiltros(any(), any());
  }

  // --- la página ------------------------------------------------------------------------------

  @Test
  void laPaginaDevuelveElTotalYLoPedido() throws Exception {
    consultar(
            COMPLETA,
            Map.of("filtro", Map.of(), "pagina", 3, "tamanio", 10),
            TokensDePrueba.ADMINISTRADOR)
        .andExpect(jsonPath("$.data.donaciones.total").value(41))
        .andExpect(jsonPath("$.data.donaciones.pagina").value(3))
        .andExpect(jsonPath("$.data.donaciones.tamanio").value(10))
        .andExpect(jsonPath("$.data.donaciones.elementos.length()").value(1));
  }

  @Test
  void elTopeDePagina_50Entra51No() throws Exception {
    consultar(COMPLETA, Map.of("filtro", Map.of(), "tamanio", 50), TokensDePrueba.ADMINISTRADOR)
        .andExpect(jsonPath("$.errors").doesNotExist())
        .andExpect(jsonPath("$.data.donaciones.tamanio").value(50));
    consultar(COMPLETA, Map.of("filtro", Map.of(), "tamanio", 51), TokensDePrueba.ADMINISTRADOR)
        .andExpect(jsonPath("$.data.donaciones").isEmpty())
        .andExpect(jsonPath("$.errors[0].extensions.codigo").value("ERR-CSR-003"));
    consultar(COMPLETA, Map.of("filtro", Map.of(), "pagina", -1), TokensDePrueba.ADMINISTRADOR)
        .andExpect(jsonPath("$.errors[0].extensions.codigo").value("ERR-CSR-003"));
  }

  // --- los campos -----------------------------------------------------------------------------

  @Test
  void losCamposDeLaDonacionSalenMapeadosAlEsquema() throws Exception {
    String base = "$.data.donaciones.elementos[0].";
    consultar(COMPLETA, sinFiltro(), TokensDePrueba.ADMINISTRADOR)
        .andExpect(jsonPath("$.errors").doesNotExist())
        .andExpect(jsonPath(base + "id").value(DONACION.toString()))
        .andExpect(jsonPath(base + "descripcion").value("Frazadas para el invierno"))
        .andExpect(jsonPath(base + "estado").value("EN_TRASLADO"))
        .andExpect(jsonPath(base + "fechaRegistro").value("2026-11-02T10:30:00"))
        .andExpect(jsonPath(base + "cantidad").value(4))
        .andExpect(jsonPath(base + "cancelacionPedida").value(true))
        .andExpect(jsonPath(base + "entidadId").value(TokensDePrueba.ENTIDAD_ID.toString()))
        .andExpect(jsonPath(base + "necesidadId").value(NECESIDAD.toString()))
        .andExpect(jsonPath(base + "items[0].cantidad").value(4))
        .andExpect(jsonPath(base + "items[0].bien.descripcion").value("Frazadas"))
        .andExpect(jsonPath(base + "items[0].bien.fotoUrl").value("foto.jpg"))
        .andExpect(jsonPath(base + "items[0].bien.fechaVencimiento").value("2027-01-31"))
        .andExpect(jsonPath(base + "items[0].bien.estado").value("NUEVO"))
        .andExpect(jsonPath(base + "items[0].subcategoria.id").value(SUBCATEGORIA.toString()))
        .andExpect(jsonPath(base + "items[0].subcategoria.nombre").value("Abrigo"))
        .andExpect(jsonPath(base + "items[0].categoria.id").value(CATEGORIA.toString()))
        .andExpect(jsonPath(base + "items[0].categoria.nombre").value("Textiles"))
        .andExpect(jsonPath(base + "items[0].categoria.unidad").value("unidades"))
        .andExpect(jsonPath(base + "historial.length()").value(2))
        .andExpect(jsonPath(base + "historial[0].estadoAnterior").isEmpty())
        .andExpect(jsonPath(base + "historial[0].estadoNuevo").value("EN_DEPOSITO"))
        .andExpect(jsonPath(base + "historial[0].fecha").value("2026-11-02T10:30:00"))
        .andExpect(jsonPath(base + "historial[1].estadoAnterior").value("EN_DEPOSITO"))
        .andExpect(jsonPath(base + "historial[1].estadoNuevo").value("EN_TRASLADO"));
  }

  @Test
  void laRespuestaNoLlevaQuienDonoNiElActorNiLaJustificacionNiLaDonacionOriginal()
      throws Exception {
    consultar(COMPLETA, sinFiltro(), TokensDePrueba.ADMINISTRADOR)
        .andExpect(jsonPath("$.errors").doesNotExist())
        .andExpect(content().string(not(containsString(QUIEN_DONO))))
        .andExpect(content().string(not(containsString(ACTOR))))
        .andExpect(content().string(not(containsString(JUSTIFICACION))))
        .andExpect(content().string(not(containsString(ORIGINAL.toString()))))
        .andExpect(content().string(not(containsString(AJENO.toString()))));
  }

  @Test
  void pedirQuienDonoOElActor_esUnErrorDeValidacion() throws Exception {
    consultar("{ donaciones { elementos { quienDono { nombre } } } }", TokensDePrueba.ADMINISTRADOR)
        .andExpect(jsonPath("$.data").doesNotExist())
        .andExpect(jsonPath("$.errors[0].extensions.classification").value("ValidationError"));
    consultar("{ donaciones { elementos { historial { actor } } } }", TokensDePrueba.ADMINISTRADOR)
        .andExpect(jsonPath("$.errors[0].extensions.classification").value("ValidationError"));
    verify(donacionesIndependientesService, never()).obtenerConFiltros(any(), any());
  }
}
