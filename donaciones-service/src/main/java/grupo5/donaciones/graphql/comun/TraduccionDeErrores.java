package grupo5.donaciones.graphql.comun;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.DonaTrackException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.common.exceptions.ValidationException;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Traduce los errores de un resolver a errores de GraphQL con el código del catálogo en {@code
 * extensions.codigo} y el {@code traceId} en {@code extensions.traceId} (contrato de GraphQL,
 * §2.1). El campo que falló queda en {@code null}.
 *
 * <p>Va antes que el traductor de seguridad de Spring for GraphQL, para que el 401 y el 403 lleven
 * también su código.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraduccionDeErrores extends DataFetcherExceptionResolverAdapter {

  static final String CODIGO = "codigo";
  static final String TRACE_ID = "traceId";

  private static final Logger log = LoggerFactory.getLogger(TraduccionDeErrores.class);

  private static final Traduccion NO_AUTENTICADO =
      new Traduccion(
          ErrorType.UNAUTHORIZED,
          ErrorCatalog.NO_AUTENTICADO.getCode(),
          "Falta el token o no es válido");
  private static final Traduccion DENEGADO =
      new Traduccion(
          ErrorType.FORBIDDEN, ErrorCatalog.ACCESO_DENEGADO.getCode(), "Acceso denegado");

  private final AuthenticationTrustResolver confianza = new AuthenticationTrustResolverImpl();

  public TraduccionDeErrores() {
    // El 401 y el 403 se distinguen por la autenticación, que vive en un ThreadLocal
    setThreadLocalContextAware(true);
  }

  @Override
  protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
    Traduccion traduccion = traducir(ex);
    Map<String, Object> extensiones = new LinkedHashMap<>();
    extensiones.put(CODIGO, traduccion.codigo());
    String traceId = TraceIdDelPedido.de(env.getGraphQlContext());
    if (traceId != null) {
      extensiones.put(TRACE_ID, traceId);
    }
    return GraphqlErrorBuilder.newError(env)
        .errorType(traduccion.tipo())
        .message(traduccion.mensaje())
        .extensions(extensiones)
        .build();
  }

  private Traduccion traducir(Throwable ex) {
    if (ex instanceof AuthenticationException) {
      return NO_AUTENTICADO;
    }
    if (ex instanceof AccessDeniedException) {
      return esAnonimo() ? NO_AUTENTICADO : DENEGADO;
    }
    if (ex instanceof ValidationException validacion) {
      return new Traduccion(ErrorType.BAD_REQUEST, validacion.getErrorCode(), "Argumento inválido");
    }
    if (ex instanceof RecursoNoEncontradoException noEncontrado) {
      return new Traduccion(
          ErrorType.NOT_FOUND, noEncontrado.getErrorCode(), "Recurso no encontrado");
    }
    if (ex instanceof BusinessStateException estado) {
      return new Traduccion(
          ErrorType.BAD_REQUEST, estado.getErrorCode(), "La operación no es válida en este estado");
    }
    if (ex instanceof DateTimeParseException || ex instanceof IllegalArgumentException) {
      return new Traduccion(
          ErrorType.BAD_REQUEST, ErrorCatalog.ARGUMENTO_INVALIDO.getCode(), "Argumento inválido");
    }
    String codigo =
        ex instanceof DonaTrackException propia
            ? propia.getErrorCode()
            : ErrorCatalog.ERROR_INTERNO.getCode();
    log.error(
        "[GRAPHQL] [ERROR-CODE: {}] [EXCEPTION: {}] - Error al resolver un campo",
        codigo,
        ex.getClass().getSimpleName(),
        ex);
    return new Traduccion(ErrorType.INTERNAL_ERROR, codigo, "Error interno");
  }

  private boolean esAnonimo() {
    var autenticacion = SecurityContextHolder.getContext().getAuthentication();
    return autenticacion == null || confianza.isAnonymous(autenticacion);
  }

  private record Traduccion(ErrorType tipo, String codigo, String mensaje) {}
}
