package grupo5.common.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.InfrastructureException;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.common.exceptions.ValidationException;
import grupo5.common.responses.ErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

class GlobalExceptionHandlerTest {

  private GlobalExceptionHandler handler;

  @BeforeEach
  void setUp() {
    handler = new GlobalExceptionHandler();
  }

  @Test
  void handleRecursoNoEncontrado_deberiaRetornarNotFound() {
    UUID id = UUID.randomUUID();
    RecursoNoEncontradoException ex = new RecursoNoEncontradoException(id);

    ResponseEntity<ErrorResponse> response = handler.handleRecursoNoEncontrado(ex);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(id.toString(), response.getBody().details());
  }

  @Test
  void handleValidation_deberiaRetornarBadRequest() {
    ValidationException ex = new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);

    ResponseEntity<ErrorResponse> response = handler.handleValidation(ex);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(ErrorCatalog.ARGUMENTO_INVALIDO.getCode(), response.getBody().code());
  }

  @Test
  void handleMethodArgumentNotValid_deberiaRetornarBadRequestConFieldErrors() {
    MethodParameter parameter = mock(MethodParameter.class);
    BindingResult bindingResult = mock(BindingResult.class);
    FieldError fieldError = new FieldError("dto", "nombre", "el nombre es obligatorio");
    when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

    MethodArgumentNotValidException ex =
        new MethodArgumentNotValidException(parameter, bindingResult);

    ResponseEntity<ErrorResponse> response = handler.handleMethodArgumentNotValid(ex);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertNotNull(response.getBody().errors());
    assertEquals(1, response.getBody().errors().size());
    assertEquals("nombre", response.getBody().errors().get(0).field());
  }

  @Test
  void handleMissingRequestHeader_deberiaRetornarBadRequest() {
    MethodParameter parameter = mock(MethodParameter.class);
    MissingRequestHeaderException ex = new MissingRequestHeaderException("X-Actor", parameter);

    ResponseEntity<ErrorResponse> response = handler.handleMissingRequestHeader(ex);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().details().contains("X-Actor"));
  }

  @Test
  void handleMissingServletRequestParameter_deberiaRetornarBadRequest() {
    MissingServletRequestParameterException ex =
        new MissingServletRequestParameterException("canal", "String");

    ResponseEntity<ErrorResponse> response = handler.handleMissingParameter(ex);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().details().contains("canal"));
  }

  @Test
  void handleHttpMessageNotReadable_deberiaRetornarBadRequest() {
    HttpMessageNotReadableException ex =
        new HttpMessageNotReadableException("JSON malformado", (HttpInputMessage) null);

    ResponseEntity<ErrorResponse> response = handler.handleHttpMessageNotReadable(ex);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
  }

  @Test
  void handleBusinessState_deberiaRetornarConflict() {
    BusinessStateException ex =
        new BusinessStateException(ErrorCatalog.ESTADO_DONACION_TRANSICION_INVALIDA);

    ResponseEntity<ErrorResponse> response = handler.handleBusinessState(ex);

    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(
        ErrorCatalog.ESTADO_DONACION_TRANSICION_INVALIDA.getCode(), response.getBody().code());
  }

  @Test
  void handleFeignException_deberiaMapearStatusRemoto() {
    Request request =
        Request.create(
            Request.HttpMethod.POST,
            "http://localhost:8083/api/entregas",
            Collections.emptyMap(),
            null,
            new RequestTemplate());
    FeignException.NotFound ex =
        new FeignException.NotFound("Recurso remoto no encontrado", request, null, null);

    ResponseEntity<ErrorResponse> response = handler.handleFeignException(ex);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertNotNull(response.getBody());
  }

  @Test
  void handleFeignException_noDeberiaExponerElMensajeCrudoDelServicioRemoto() {
    Request request =
        Request.create(
            Request.HttpMethod.POST,
            "http://localhost:8083/api/entregas",
            Collections.emptyMap(),
            null,
            new RequestTemplate());
    FeignException.NotFound ex =
        new FeignException.NotFound(
            "[404] during POST to http://localhost:8083/api/entregas: [{\"secreto\":\"dato interno\"}]",
            request,
            null,
            null);

    ResponseEntity<ErrorResponse> response = handler.handleFeignException(ex);

    assertNotNull(response.getBody());
    assertFalse(response.getBody().details().contains("dato interno"));
  }

  @Test
  void handleInfrastructure_deberiaRetornarInternalServerError() {
    InfrastructureException ex =
        new InfrastructureException(
            ErrorCatalog.ERROR_INTERNO, new RuntimeException("Fallo en base de datos"));

    ResponseEntity<ErrorResponse> response = handler.handleInfrastructure(ex);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertNotNull(response.getBody());
  }

  @Test
  void handleGeneric_deberiaRetornarInternalServerError() {
    RuntimeException ex = new RuntimeException("Error inesperado");

    ResponseEntity<ErrorResponse> response = handler.handleGeneric(ex);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(ErrorCatalog.ERROR_INTERNO.getCode(), response.getBody().code());
  }

  @Test
  void handleHandlerMethodValidation_deberiaRetornarBadRequest() {
    HandlerMethodValidationException ex = mock(HandlerMethodValidationException.class);

    ResponseEntity<ErrorResponse> response = handler.handleHandlerMethodValidation(ex);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(ErrorCatalog.ARGUMENTO_INVALIDO.getCode(), response.getBody().code());
    assertEquals("Parámetros de solicitud no válidos.", response.getBody().details());
  }

  @Test
  void handleConstraintViolation_deberiaRetornarBadRequestConFieldErrors() {
    Path propertyPath = mock(Path.class);
    when(propertyPath.toString()).thenReturn("cantidad");
    ConstraintViolation<?> violation = mock(ConstraintViolation.class);
    when(violation.getPropertyPath()).thenReturn(propertyPath);
    when(violation.getMessage()).thenReturn("debe ser mayor a cero");
    when(violation.getInvalidValue()).thenReturn(-1);

    ConstraintViolationException ex =
        new ConstraintViolationException("Violación de restricciones", Set.of(violation));

    ResponseEntity<ErrorResponse> response = handler.handleConstraintViolation(ex);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertNotNull(response.getBody().errors());
    assertEquals(1, response.getBody().errors().size());
    assertEquals("cantidad", response.getBody().errors().get(0).field());
    assertEquals("debe ser mayor a cero", response.getBody().errors().get(0).message());
  }
}
