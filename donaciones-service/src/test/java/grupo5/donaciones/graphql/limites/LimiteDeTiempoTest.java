package grupo5.donaciones.graphql.limites;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.donaciones.dto.Pagina;
import grupo5.donaciones.dto.publico.EntidadPublicaDTO;
import grupo5.donaciones.graphql.comun.BaseSliceGraphQl;
import grupo5.donaciones.graphql.comun.PaginaGraphQl;
import grupo5.donaciones.graphql.comun.TopeDePagina;
import grupo5.donaciones.seguridad.Acceso;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;

/**
 * El tiempo máximo por consulta (SPEC-11, G3, lote {@code g3-limites}): una consulta que tarda más
 * que la propiedad la corta Donaciones con un error, antes de la espera de lectura del gateway. El
 * tope baja a 200 ms por propiedad y un resolver de prueba sobre {@code entidades} tarda 5 s.
 */
@WebMvcTest(controllers = LimiteDeTiempoTest.ResolverLento.class)
@Import({
  LimiteDeTiempoTest.ResolverLento.class,
  MetricasEnMemoria.class,
  LimiteDeTiempo.class,
  RechazosDeConsulta.class
})
@TestPropertySource(properties = "donatrack.graphql.limites.tiempo-maximo-ms=200")
class LimiteDeTiempoTest extends BaseSliceGraphQl {

  private static final Duration DEMORA = Duration.ofSeconds(5);
  private static final String TRAZA = "traza-de-tiempo";

  @Autowired private MeterRegistry registro;

  /** Resolver de prueba: {@code entidades} responde después de {@link #DEMORA}. */
  @Controller
  public static class ResolverLento {

    private final TopeDePagina tope;

    public ResolverLento(TopeDePagina tope) {
      this.tope = tope;
    }

    @QueryMapping
    @PreAuthorize(Acceso.PUBLICO)
    public Mono<PaginaGraphQl<EntidadPublicaDTO>> entidades(
        @Argument Integer pagina, @Argument Integer tamanio) {
      PaginaGraphQl<EntidadPublicaDTO> respuesta =
          PaginaGraphQl.de(
              new Pagina<>(List.<EntidadPublicaDTO>of(), 0), tope.validar(pagina, tamanio), e -> e);
      return Mono.delay(DEMORA).map(listo -> respuesta);
    }
  }

  @BeforeEach
  void unRegistroVacio() {
    registro.clear();
  }

  @Test
  void unaConsultaQueTardaMasQueElTopeSeCortaConUnError() throws Exception {
    long inicio = System.nanoTime();

    ejecutar(conTraza(pedido("{ entidades { total } }", Map.of(), null), TRAZA))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").doesNotExist())
        .andExpect(jsonPath("$.errors.length()").value(1))
        .andExpect(jsonPath("$.errors[0].extensions.codigo").value("ERR-CSR-500"))
        .andExpect(jsonPath("$.errors[0].extensions.motivo").value("tiempo"))
        .andExpect(jsonPath("$.errors[0].extensions.traceId").value(TRAZA))
        .andExpect(
            jsonPath("$.errors[0].message").value("La consulta superó el tiempo máximo de 200 ms"));

    assertThat(Duration.ofNanos(System.nanoTime() - inicio)).isLessThan(DEMORA);
    assertThat(registro.get("donatrack.graphql.rechazos").tag("motivo", "tiempo").counter().count())
        .isEqualTo(1.0);
  }

  @Test
  void unTiempoMaximoQueNoEsPositivoNoArranca() {
    RechazosDeConsulta rechazos = new RechazosDeConsulta(registro);

    assertThatThrownBy(() -> new LimiteDeTiempo(0, rechazos))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new LimiteDeTiempo(-1, rechazos))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
