package grupo5.incentivos.dto;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class DTOValidationTest {

  private static Validator validator;

  @BeforeAll
  static void setUpValidator() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @Test
  void registrarDonanteRequest_cuandoEsValido_noDeberiaTenerViolaciones() {
    RegistrarDonanteRequest request =
        new RegistrarDonanteRequest(UUID.randomUUID(), UUID.randomUUID(), "Donante Valido");

    Set<ConstraintViolation<RegistrarDonanteRequest>> violations = validator.validate(request);

    assertTrue(violations.isEmpty());
  }

  @Test
  void registrarDonanteRequest_conCamposNulosOInvalidos_deberiaReportarViolaciones() {
    RegistrarDonanteRequest request = new RegistrarDonanteRequest(null, null, "   ");

    Set<ConstraintViolation<RegistrarDonanteRequest>> violations = validator.validate(request);

    assertEquals(3, violations.size());
    assertTrue(
        violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("idDonante")));
    assertTrue(
        violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("idPersona")));
    assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("nombre")));
  }

  @Test
  void registrarDonanteRequest_conNombreCortoOLargo_deberiaReportarViolacion() {
    RegistrarDonanteRequest corto =
        new RegistrarDonanteRequest(UUID.randomUUID(), UUID.randomUUID(), "A");
    RegistrarDonanteRequest largo =
        new RegistrarDonanteRequest(UUID.randomUUID(), UUID.randomUUID(), "A".repeat(101));

    assertEquals(1, validator.validate(corto).size());
    assertEquals(1, validator.validate(largo).size());
  }

  @Test
  void nuevaDonacionRequest_cuandoEsValido_noDeberiaTenerViolaciones() {
    NuevaDonacionRequest request =
        new NuevaDonacionRequest(
            UUID.randomUUID(), List.of("alimentos", "ropa"), 5, LocalDate.now());

    Set<ConstraintViolation<NuevaDonacionRequest>> violations = validator.validate(request);

    assertTrue(violations.isEmpty());
  }

  @Test
  void nuevaDonacionRequest_conFechaFutura_deberiaReportarViolacion() {
    NuevaDonacionRequest request =
        new NuevaDonacionRequest(
            UUID.randomUUID(), List.of("alimentos"), 5, LocalDate.now().plusDays(2));

    Set<ConstraintViolation<NuevaDonacionRequest>> violations = validator.validate(request);

    assertEquals(1, violations.size());
    assertEquals("fecha", violations.iterator().next().getPropertyPath().toString());
  }

  @Test
  void nuevaDonacionRequest_conCategoriasVaciasOElementosEnBlanco_deberiaReportarViolacion() {
    NuevaDonacionRequest vacia =
        new NuevaDonacionRequest(UUID.randomUUID(), List.of(), 5, LocalDate.now());
    NuevaDonacionRequest blanco =
        new NuevaDonacionRequest(UUID.randomUUID(), List.of("   "), 5, LocalDate.now());

    assertFalse(validator.validate(vacia).isEmpty());
    assertFalse(validator.validate(blanco).isEmpty());
  }

  @Test
  void nuevaDonacionRequest_conCantidadBienesCeroONegativa_deberiaReportarViolacion() {
    NuevaDonacionRequest cero =
        new NuevaDonacionRequest(UUID.randomUUID(), List.of("alimentos"), 0, LocalDate.now());
    NuevaDonacionRequest negativa =
        new NuevaDonacionRequest(UUID.randomUUID(), List.of("alimentos"), -3, LocalDate.now());

    assertEquals(1, validator.validate(cero).size());
    assertEquals(1, validator.validate(negativa).size());
  }

  @Test
  void donacionExitosaRequest_cuandoEsValido_noDeberiaTenerViolaciones() {
    DonacionExitosaRequest request =
        new DonacionExitosaRequest(UUID.randomUUID(), UUID.randomUUID());

    Set<ConstraintViolation<DonacionExitosaRequest>> violations = validator.validate(request);

    assertTrue(violations.isEmpty());
  }

  @Test
  void donacionExitosaRequest_conCamposNulos_deberiaReportarViolaciones() {
    DonacionExitosaRequest request = new DonacionExitosaRequest(null, null);

    Set<ConstraintViolation<DonacionExitosaRequest>> violations = validator.validate(request);

    assertEquals(2, violations.size());
  }

  @Test
  void modificarDonanteRequest_cuandoEsValido_noDeberiaTenerViolaciones() {
    ModificarDonanteRequest request = new ModificarDonanteRequest("Nuevo Nombre");

    Set<ConstraintViolation<ModificarDonanteRequest>> violations = validator.validate(request);

    assertTrue(violations.isEmpty());
  }

  @Test
  void modificarDonanteRequest_conNombreInvalido_deberiaReportarViolaciones() {
    ModificarDonanteRequest blanco = new ModificarDonanteRequest("   ");
    ModificarDonanteRequest nulo = new ModificarDonanteRequest(null);
    ModificarDonanteRequest corto = new ModificarDonanteRequest("X");

    assertFalse(validator.validate(blanco).isEmpty());
    assertFalse(validator.validate(nulo).isEmpty());
    assertFalse(validator.validate(corto).isEmpty());
  }
}
