package grupo5.common.responses;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class ErrorResponseTest {

  @BeforeEach
  void setUp() {
    MDC.clear();
  }

  @AfterEach
  void tearDown() {
    MDC.clear();
  }

  @Test
  void constructor_conExcepcionDonaTrack_deberiaMapearCamposYResolverTraceId() {
    MDC.put("traceId", "test-trace-123");

    ValidationException ex = new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    ErrorResponse response = new ErrorResponse(ex);

    assertEquals(ex.getErrorCode(), response.code());
    assertEquals("ValidationException", response.type());
    assertEquals("test-trace-123", response.traceId());
    assertNotNull(response.timestamp());
  }

  @Test
  void constructor_sinMdc_deberiaGenerarTraceIdAleatorio() {
    ValidationException ex = new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    ErrorResponse response = new ErrorResponse(ex);

    assertNotNull(response.traceId());
    assertEquals(32, response.traceId().length());
  }

  @Test
  void constructor_conListaDeErrores_deberiaMapearCorrectamente() {
    List<FieldErrorDTO> errors =
        List.of(
            new FieldErrorDTO("nombre", "no puede estar vacío", ""),
            new FieldErrorDTO("unidad", "es requerida", null));

    ErrorResponse response =
        new ErrorResponse(
            "ERR-TEST", "MethodArgumentNotValidException", "Validación fallida", errors);

    assertEquals("ERR-TEST", response.code());
    assertEquals("Validación fallida", response.details());
    assertEquals(2, response.errors().size());
    assertEquals("nombre", response.errors().get(0).field());
  }
}
