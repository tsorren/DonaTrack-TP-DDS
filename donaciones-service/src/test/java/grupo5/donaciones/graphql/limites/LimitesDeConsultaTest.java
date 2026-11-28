package grupo5.donaciones.graphql.limites;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.donaciones.dto.Pagina;
import grupo5.donaciones.dto.publico.EntidadPublicaDTO;
import grupo5.donaciones.dto.publico.NecesidadPublicaDTO;
import grupo5.donaciones.graphql.comun.BaseSliceGraphQl;
import grupo5.donaciones.graphql.entidades.EntidadesResolver;
import grupo5.donaciones.graphql.necesidades.NecesidadesResolver;
import grupo5.donaciones.graphql.relaciones.RelacionesDeNecesidad;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.graphql.autoconfigure.observation.GraphQlObservationAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

/**
 * Los límites de las consultas (SPEC-11, G3, lote {@code g3-limites}): el tope de página, la
 * profundidad y la complejidad rechazan la consulta antes de leer la base, con un error claro; y
 * M08 deja una medición por operación y un contador de rechazos con su motivo, en un registro en
 * memoria (D12).
 *
 * <p>La profundidad baja a 3 por propiedad: con el esquema sin ciclos ninguna consulta legítima
 * pasa de 5 niveles, así que el valor real (6) no se puede superar sin la introspección. La
 * complejidad usa el valor de {@code application.properties}.
 */
@WebMvcTest(
    controllers = {NecesidadesResolver.class, RelacionesDeNecesidad.class, EntidadesResolver.class})
@ImportAutoConfiguration(GraphQlObservationAutoConfiguration.class)
@Import({MetricasEnMemoria.class, LimitesDeConsulta.class, RechazosDeConsulta.class})
@TestPropertySource(properties = "donatrack.graphql.limites.profundidad-maxima=3")
class LimitesDeConsultaTest extends BaseSliceGraphQl {

  private static final UUID ENTIDAD = UUID.fromString("00000000-0000-4000-8000-0000000000e1");
  private static final UUID NECESIDAD = UUID.fromString("00000000-0000-4000-8000-0000000000e2");
  private static final UUID SUBCATEGORIA = UUID.fromString("00000000-0000-4000-8000-0000000000e3");

  /** Cuatro niveles: {@code necesidades → elementos → entidad → razonSocial}. */
  private static final String CUATRO_NIVELES =
      "{ necesidades { elementos { entidad { razonSocial } } } }";

  private static final String TRES_NIVELES = "{ necesidades { elementos { id } } }";

  /** Dos campos por raíz: {@code entidades} y {@code total}. */
  private static final String UNA_RAIZ_DE_ENTIDADES = "entidades { total }";

  private static final String INTROSPECCION_PROFUNDA =
      "{ __schema { types { fields { type { name } } } } }";

  private static final String RECHAZOS = "donatrack.graphql.rechazos";

  @Autowired private MeterRegistry registro;

  @Autowired private LimitesDeConsulta limites;

  @BeforeEach
  void unRegistroVacioYServiciosQueResponden() {
    registro.clear();
    when(necesidadesService.buscarPublicas(any(), any(), any(), any(), anyInt(), anyInt()))
        .thenReturn(
            new Pagina<>(
                List.of(
                    new NecesidadPublicaDTO(
                        NECESIDAD,
                        ENTIDAD,
                        SUBCATEGORIA,
                        "Necesidad sintetica",
                        3,
                        "RECURRENTE",
                        null)),
                1));
    when(entidadBeneficiariaService.buscarPublicas(any(), any(), anyInt(), anyInt()))
        .thenReturn(new Pagina<>(List.of(new EntidadPublicaDTO(ENTIDAD, "Comedor", null)), 1));
  }

  // --- los valores salen de las propiedades -------------------------------------------------

  @Test
  void losTopesSonLosDeLasPropiedades() {
    assertThat(limites.profundidadMaxima()).isEqualTo(3);
    assertThat(limites.complejidadMaxima()).isEqualTo(100);
    assertThat(limites.raicesMaximas()).isEqualTo(5);
  }

  // --- el tope de página --------------------------------------------------------------------

  @Test
  void unaPaginaMasGrandeQueElTopeSeRechazaSinLeerLaBase() throws Exception {
    consultar("{ entidades(tamanio: 51) { total } }", null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.entidades").isEmpty())
        .andExpect(jsonPath("$.errors[0].extensions.codigo").value("ERR-CSR-003"));

    verifyNoInteractions(entidadBeneficiariaService);
  }

  // --- la profundidad -----------------------------------------------------------------------

  @Test
  void unaConsultaMasProfundaQueElTopeSeRechazaSinLeerLaBase() throws Exception {
    consultar(CUATRO_NIVELES, null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").doesNotExist())
        .andExpect(jsonPath("$.errors.length()").value(1))
        .andExpect(jsonPath("$.errors[0].extensions.codigo").value("ERR-CSR-003"))
        .andExpect(jsonPath("$.errors[0].extensions.motivo").value("profundidad"))
        .andExpect(jsonPath("$.errors[0].extensions.classification").value("BAD_REQUEST"))
        .andExpect(
            jsonPath("$.errors[0].message")
                .value("La consulta supera la profundidad máxima: 4 niveles, el tope es 3"));

    verifyNoInteractions(necesidadesService, entidadBeneficiariaService);
    assertThat(rechazos("profundidad")).isEqualTo(1.0);
  }

  @Test
  void unaConsultaEnElTopeDeProfundidadSeResuelve() throws Exception {
    consultar(TRES_NIVELES, null)
        .andExpect(jsonPath("$.errors").doesNotExist())
        .andExpect(jsonPath("$.data.necesidades.elementos[0].id").value(NECESIDAD.toString()));

    assertThat(registro.find(RECHAZOS).counters()).isEmpty();
  }

  @Test
  void laIntrospeccionNoCuentaParaLosLimites() throws Exception {
    consultar(INTROSPECCION_PROFUNDA, null)
        .andExpect(jsonPath("$.errors[?(@.extensions.motivo)]").isEmpty());

    assertThat(registro.find(RECHAZOS).counters()).isEmpty();
  }

  // --- la complejidad -----------------------------------------------------------------------

  @Test
  void unaConsultaMasComplejaQueElTopeSeRechazaSinLeerLaBase() throws Exception {
    consultar(conCampos(105), null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").doesNotExist())
        .andExpect(jsonPath("$.errors[0].extensions.codigo").value("ERR-CSR-003"))
        .andExpect(jsonPath("$.errors[0].extensions.motivo").value("complejidad"))
        .andExpect(
            jsonPath("$.errors[0].message")
                .value("La consulta supera la complejidad máxima: 105 campos, el tope es 100"));

    verifyNoInteractions(entidadBeneficiariaService);
    assertThat(rechazos("complejidad")).isEqualTo(1.0);
    assertThat(registro.find(RECHAZOS).tag("motivo", "profundidad").counter()).isNull();
  }

  @Test
  void unaConsultaEnElTopeDeComplejidadSeResuelve() throws Exception {
    consultar(conCampos(100), null).andExpect(jsonPath("$.errors").doesNotExist());

    verify(entidadBeneficiariaService, times(1)).buscarPublicas(any(), any(), anyInt(), anyInt());
  }

  // --- las raíces -----------------------------------------------------------------------------

  @Test
  void masRaicesQueElTopeSeRechazanSinLeerLaBase_aunqueEntrenEnLaComplejidad() throws Exception {
    consultar(conRaices(6), null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").doesNotExist())
        .andExpect(jsonPath("$.errors.length()").value(1))
        .andExpect(jsonPath("$.errors[0].extensions.codigo").value("ERR-CSR-003"))
        .andExpect(jsonPath("$.errors[0].extensions.motivo").value("complejidad"))
        .andExpect(
            jsonPath("$.errors[0].message")
                .value("La consulta pide demasiadas consultas raíz: 6, el tope es 5"));

    verifyNoInteractions(entidadBeneficiariaService);
    assertThat(rechazos("complejidad")).isEqualTo(1.0);
  }

  @Test
  void lasRaicesDelTopeSeResuelven() throws Exception {
    consultar(conRaices(5), null).andExpect(jsonPath("$.errors").doesNotExist());

    verify(entidadBeneficiariaService, times(5)).buscarPublicas(any(), any(), anyInt(), anyInt());
  }

  // --- M08 ----------------------------------------------------------------------------------

  @Test
  void cadaOperacionResueltaDejaUnaMedicionConSuDuracion() throws Exception {
    consultar(TRES_NIVELES, null).andExpect(jsonPath("$.errors").doesNotExist());

    Timer medicion = registro.get("graphql.request").timer();
    assertThat(medicion.count()).isEqualTo(1);
    assertThat(medicion.totalTime(TimeUnit.NANOSECONDS)).isPositive();

    consultar("{ entidades { total } }", Map.of(), null)
        .andExpect(jsonPath("$.errors").doesNotExist());
    assertThat(registro.get("graphql.request").timer().count()).isEqualTo(2);
  }

  @Test
  void cadaRechazoSumaUnoASuMotivo() throws Exception {
    consultar(CUATRO_NIVELES, null);
    consultar(CUATRO_NIVELES, null);
    consultar(conCampos(120), null);

    assertThat(rechazos("profundidad")).isEqualTo(2.0);
    assertThat(rechazos("complejidad")).isEqualTo(1.0);
  }

  private double rechazos(String motivo) {
    return registro.get(RECHAZOS).tag("motivo", motivo).counter().count();
  }

  /**
   * Una sola raíz con {@code campos} campos en total: {@code entidades}, {@code elementos} y alias
   * de {@code id}.
   */
  private static String conCampos(int campos) {
    return IntStream.rangeClosed(1, campos - 2)
        .mapToObj(i -> "c" + i + ": id")
        .collect(Collectors.joining(" ", "{ entidades { elementos { ", " } } }"));
  }

  private static String conRaices(int cantidad) {
    return IntStream.rangeClosed(1, cantidad)
        .mapToObj(i -> "e" + i + ": " + UNA_RAIZ_DE_ENTIDADES)
        .collect(Collectors.joining(" ", "{ ", " }"));
  }
}
