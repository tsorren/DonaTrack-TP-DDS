package grupo5.donaciones.graphql.limites;

import graphql.ExecutionResult;
import graphql.analysis.QueryTraverser;
import graphql.analysis.QueryVisitorFieldEnvironment;
import graphql.analysis.QueryVisitorStub;
import graphql.execution.AbortExecutionException;
import graphql.execution.ExecutionContext;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.SimplePerformantInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationExecuteOperationParameters;
import grupo5.donaciones.graphql.comun.TraceIdDelPedido;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * La profundidad máxima y el tope de complejidad de una consulta (SPEC-11, G3), leídos de {@code
 * donatrack.graphql.limites.*}. Se miden sobre el documento ya validado, antes de que corra ningún
 * resolver: una consulta que se pasa no lee la base. El rechazo es un error con {@code ERR-CSR-003}
 * y su motivo, y suma uno al contador de {@link RechazosDeConsulta}.
 *
 * <p>La profundidad cuenta los campos anidados desde la raíz (la raíz es 1); la complejidad cuenta
 * un punto por campo pedido, alias incluidos. Los campos de introspección ({@code __schema}, {@code
 * __type}, {@code __typename}) no cuentan: los acota graphql-java con {@code
 * GoodFaithIntrospection} y GraphiQL, donde está encendido, los necesita más profundos que
 * cualquier consulta de datos.
 */
@Component
public class LimitesDeConsulta extends SimplePerformantInstrumentation {

  private static final String INTROSPECCION = "__";

  private final int profundidadMaxima;
  private final int complejidadMaxima;
  private final RechazosDeConsulta rechazos;

  public LimitesDeConsulta(
      @Value("${donatrack.graphql.limites.profundidad-maxima}") int profundidadMaxima,
      @Value("${donatrack.graphql.limites.complejidad-maxima}") int complejidadMaxima,
      RechazosDeConsulta rechazos) {
    if (profundidadMaxima < 1 || complejidadMaxima < 1) {
      throw new IllegalArgumentException(
          "La profundidad y la complejidad máximas de GraphQL tienen que ser al menos 1");
    }
    this.profundidadMaxima = profundidadMaxima;
    this.complejidadMaxima = complejidadMaxima;
    this.rechazos = rechazos;
  }

  @Override
  public InstrumentationContext<ExecutionResult> beginExecuteOperation(
      InstrumentationExecuteOperationParameters parametros, InstrumentationState estado) {
    ExecutionContext contexto = parametros.getExecutionContext();
    Medida medida = medir(contexto);
    if (medida.profundidad > profundidadMaxima) {
      rechazar(
          MotivoDeRechazo.PROFUNDIDAD,
          String.format(
              "La consulta supera la profundidad máxima: %d niveles, el tope es %d",
              medida.profundidad, profundidadMaxima),
          contexto);
    }
    if (medida.complejidad > complejidadMaxima) {
      rechazar(
          MotivoDeRechazo.COMPLEJIDAD,
          String.format(
              "La consulta supera la complejidad máxima: %d campos, el tope es %d",
              medida.complejidad, complejidadMaxima),
          contexto);
    }
    return super.beginExecuteOperation(parametros, estado);
  }

  public int profundidadMaxima() {
    return profundidadMaxima;
  }

  public int complejidadMaxima() {
    return complejidadMaxima;
  }

  private void rechazar(MotivoDeRechazo motivo, String mensaje, ExecutionContext contexto) {
    rechazos.contar(motivo);
    throw new AbortExecutionException(
        List.of(motivo.error(mensaje, TraceIdDelPedido.de(contexto.getGraphQLContext()))));
  }

  private static Medida medir(ExecutionContext contexto) {
    Medida medida = new Medida();
    QueryTraverser.newQueryTraverser()
        .schema(contexto.getGraphQLSchema())
        .document(contexto.getDocument())
        .operationName(contexto.getOperationDefinition().getName())
        .coercedVariables(contexto.getCoercedVariables())
        .build()
        .visitPreOrder(
            new QueryVisitorStub() {
              @Override
              public void visitField(QueryVisitorFieldEnvironment campo) {
                int nivel = nivelSinIntrospeccion(campo);
                if (nivel > 0) {
                  medida.complejidad++;
                  medida.profundidad = Math.max(medida.profundidad, nivel);
                }
              }
            });
    return medida;
  }

  /** El nivel del campo desde la raíz, o 0 si él o alguno de sus padres es de introspección. */
  private static int nivelSinIntrospeccion(QueryVisitorFieldEnvironment campo) {
    int nivel = 0;
    for (QueryVisitorFieldEnvironment actual = campo;
        actual != null;
        actual = actual.getParentEnvironment()) {
      if (actual.getField().getName().startsWith(INTROSPECCION)) {
        return 0;
      }
      nivel++;
    }
    return nivel;
  }

  /** Lo que mide el recorrido: el nivel más hondo y la cantidad de campos. */
  private static final class Medida {
    private int profundidad;
    private int complejidad;
  }
}
