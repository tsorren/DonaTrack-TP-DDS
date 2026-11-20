package grupo5.donaciones.graphql.comun;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import grupo5.common.CommonLibAutoConfiguration;
import grupo5.common.logging.LoggingAutoConfiguration;
import grupo5.common.logging.TraceResponseHeaderFilter;
import grupo5.common.seguridad.SeguridadAutoConfiguration;
import grupo5.donaciones.seguridad.ReglasDeAccesoDonaciones;
import grupo5.donaciones.seguridad.TokensDePrueba;
import grupo5.donaciones.services.IDonacionesIndependientesService;
import grupo5.donaciones.services.IEntidadBeneficiariaService;
import grupo5.donaciones.services.INecesidadesService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.graphql.autoconfigure.GraphQlAutoConfiguration;
import org.springframework.boot.graphql.autoconfigure.security.GraphQlWebMvcSecurityAutoConfiguration;
import org.springframework.boot.graphql.autoconfigure.servlet.GraphQlWebMvcAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Base de los slices de GraphQL (SPEC-11, G2): la ruta {@value #RUTA} con el esquema real, la
 * seguridad encendida, el {@link TokensDePrueba} y los servicios de aplicación simulados.
 *
 * <p>Cada test declara sus resolvers con {@code @WebMvcTest(controllers = MiResolver.class)}: así
 * solo carga los suyos y no los de otros lotes. Los componentes del paquete {@code graphql} que no
 * son {@code @Controller} (mapeos, cargas por lote) se suman con {@code @Import} en el test.
 */
@ImportAutoConfiguration({
  AopAutoConfiguration.class,
  GraphQlAutoConfiguration.class,
  GraphQlWebMvcAutoConfiguration.class,
  GraphQlWebMvcSecurityAutoConfiguration.class
})
@Import({
  CommonLibAutoConfiguration.class,
  LoggingAutoConfiguration.class,
  SeguridadAutoConfiguration.class,
  ReglasDeAccesoDonaciones.class,
  TokensDePrueba.class,
  TopeDePagina.class,
  TraduccionDeErrores.class,
  TraceIdDelPedido.class
})
@TestPropertySource(properties = "donatrack.seguridad.habilitada=true")
@Execution(ExecutionMode.SAME_THREAD)
public abstract class BaseSliceGraphQl {

  /** La ruta de GraphQL del servicio (SPEC-11, §2). */
  public static final String RUTA = "/api/donaciones/graphql";

  private final ObjectMapper json = new ObjectMapper();

  @Autowired protected MockMvc mockMvc;

  @MockitoBean protected INecesidadesService necesidadesService;

  @MockitoBean protected IEntidadBeneficiariaService entidadBeneficiariaService;

  @MockitoBean protected IDonacionesIndependientesService donacionesIndependientesService;

  /** Una consulta sin variables; {@code token} nulo es un visitante sin token. */
  protected ResultActions consultar(String consulta, String token) throws Exception {
    return consultar(consulta, Map.of(), token);
  }

  /** Una consulta con variables; {@code token} nulo es un visitante sin token. */
  protected ResultActions consultar(String consulta, Map<String, Object> variables, String token)
      throws Exception {
    return ejecutar(pedido(consulta, variables, token));
  }

  /** El pedido HTTP de una consulta, para sumarle encabezados antes de {@link #ejecutar}. */
  protected MockHttpServletRequestBuilder pedido(
      String consulta, Map<String, Object> variables, String token) throws Exception {
    Map<String, Object> cuerpo = new LinkedHashMap<>();
    cuerpo.put("query", consulta);
    cuerpo.put("variables", variables);
    MockHttpServletRequestBuilder pedido =
        post(RUTA)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(cuerpo));
    return token == null ? pedido : pedido.header("Authorization", "Bearer " + token);
  }

  /** Un pedido con un {@code X-Trace-Id} propio, como lo propaga el gateway. */
  protected MockHttpServletRequestBuilder conTraza(
      MockHttpServletRequestBuilder pedido, String traceId) {
    return pedido.header(TraceResponseHeaderFilter.TRACE_HEADER, traceId);
  }

  /** Ejecuta el pedido y, si GraphQL lo respondió de forma asíncrona, espera la respuesta. */
  protected ResultActions ejecutar(MockHttpServletRequestBuilder pedido) throws Exception {
    ResultActions acciones = mockMvc.perform(pedido);
    MvcResult resultado = acciones.andReturn();
    return resultado.getRequest().isAsyncStarted()
        ? mockMvc.perform(asyncDispatch(resultado))
        : acciones;
  }
}
