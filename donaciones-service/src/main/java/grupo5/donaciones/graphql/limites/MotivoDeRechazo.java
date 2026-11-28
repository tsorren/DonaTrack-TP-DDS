package grupo5.donaciones.graphql.limites;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import grupo5.common.exceptions.ErrorCatalog;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.graphql.execution.ErrorType;

/**
 * Por qué se rechaza una consulta (SPEC-11, G3). Cada motivo va en {@code extensions.motivo} del
 * error y en la etiqueta del contador de {@link RechazosDeConsulta}.
 */
public enum MotivoDeRechazo {
  PROFUNDIDAD("profundidad", ErrorType.BAD_REQUEST, ErrorCatalog.ARGUMENTO_INVALIDO),
  COMPLEJIDAD("complejidad", ErrorType.BAD_REQUEST, ErrorCatalog.ARGUMENTO_INVALIDO),
  TIEMPO("tiempo", ErrorType.INTERNAL_ERROR, ErrorCatalog.ERROR_INTERNO);

  static final String CODIGO = "codigo";
  static final String MOTIVO = "motivo";
  static final String TRACE_ID = "traceId";

  private final String etiqueta;
  private final ErrorType tipo;
  private final ErrorCatalog codigo;

  MotivoDeRechazo(String etiqueta, ErrorType tipo, ErrorCatalog codigo) {
    this.etiqueta = etiqueta;
    this.tipo = tipo;
    this.codigo = codigo;
  }

  public String etiqueta() {
    return etiqueta;
  }

  /**
   * El error de GraphQL del rechazo, con el código del catálogo, el motivo y el {@code traceId}
   * (contrato de GraphQL, §2.1). Un {@code traceId} nulo no se agrega.
   */
  GraphQLError error(String mensaje, String traceId) {
    Map<String, Object> extensiones = new LinkedHashMap<>();
    extensiones.put(CODIGO, codigo.getCode());
    extensiones.put(MOTIVO, etiqueta);
    if (traceId != null) {
      extensiones.put(TRACE_ID, traceId);
    }
    return GraphqlErrorBuilder.newError()
        .errorType(tipo)
        .message(mensaje)
        .extensions(extensiones)
        .build();
  }
}
