package grupo5.logistica.dto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.logistica.dto.callback.CallbackPlanificacionRequestDTO;
import grupo5.logistica.dto.callback.RutaPlanificadaDTO;
import grupo5.logistica.dto.camiones.CambioEstadoCamionRequestDTO;
import grupo5.logistica.dto.camiones.CamionRequestDTO;
import grupo5.logistica.dto.choferes.CambioEstadoChoferRequestDTO;
import grupo5.logistica.dto.choferes.ChoferRequestDTO;
import grupo5.logistica.dto.entregas.*;
import grupo5.logistica.dto.rutas.AgregarEntregaRutaRequestDTO;
import grupo5.logistica.dto.rutas.CambioEstadoRutaRequestDTO;
import grupo5.logistica.dto.rutas.DireccionDTO;
import grupo5.logistica.dto.rutas.IniciarRutaRequestDTO;
import grupo5.logistica.models.entities.camiones.EstadoCamion;
import grupo5.logistica.models.entities.choferes.EstadoChofer;
import grupo5.logistica.models.entities.entregas.EstadoEntrega;
import grupo5.logistica.models.entities.rutas.EstadoRuta;
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

class DtoValidationTest {

  private static Validator validator;

  @BeforeAll
  static void setUp() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  // ======================== CamionRequestDTO ========================

  @Test
  void camionRequest_valido_sinViolaciones() {
    assertTrue(validator.validate(new CamionRequestDTO("AB123CD", 20f, 3f, 5000f)).isEmpty());
  }

  @Test
  void camionRequest_patenteNula_violacion() {
    assertFalse(validator.validate(new CamionRequestDTO(null, 20f, 3f, 5000f)).isEmpty());
  }

  @Test
  void camionRequest_patenteVacia_violacion() {
    assertFalse(validator.validate(new CamionRequestDTO("  ", 20f, 3f, 5000f)).isEmpty());
  }

  @Test
  void camionRequest_capacidadNula_violacion() {
    assertFalse(validator.validate(new CamionRequestDTO("AB123CD", null, 3f, 5000f)).isEmpty());
  }

  @Test
  void camionRequest_capacidadNegativa_violacion() {
    assertFalse(validator.validate(new CamionRequestDTO("AB123CD", -1f, 3f, 5000f)).isEmpty());
  }

  // ======================== CambioEstadoCamionRequestDTO ========================

  @Test
  void cambioEstadoCamion_valido_sinViolaciones() {
    assertTrue(
        validator
            .validate(new CambioEstadoCamionRequestDTO(EstadoCamion.DISPONIBLE, null))
            .isEmpty());
  }

  @Test
  void cambioEstadoCamion_estadoNulo_violacion() {
    assertFalse(validator.validate(new CambioEstadoCamionRequestDTO(null, null)).isEmpty());
  }

  // ======================== ChoferRequestDTO ========================

  @Test
  void choferRequest_valido_sinViolaciones() {
    assertTrue(
        validator.validate(new ChoferRequestDTO("Ada", "Lovelace", "LIC-1", "1111")).isEmpty());
  }

  @Test
  void choferRequest_nombreNulo_violacion() {
    assertFalse(
        validator.validate(new ChoferRequestDTO(null, "Lovelace", "LIC-1", "1111")).isEmpty());
  }

  @Test
  void choferRequest_apellidoVacio_violacion() {
    assertFalse(validator.validate(new ChoferRequestDTO("Ada", "  ", "LIC-1", "1111")).isEmpty());
  }

  // ======================== CambioEstadoChoferRequestDTO ========================

  @Test
  void cambioEstadoChofer_valido_sinViolaciones() {
    assertTrue(
        validator
            .validate(new CambioEstadoChoferRequestDTO(EstadoChofer.DISPONIBLE, null))
            .isEmpty());
  }

  @Test
  void cambioEstadoChofer_estadoNulo_violacion() {
    assertFalse(validator.validate(new CambioEstadoChoferRequestDTO(null, null)).isEmpty());
  }

  // ======================== CrearEntregaRequestDTO ========================

  @Test
  void crearEntrega_valido_sinViolaciones() {
    DireccionDTO dir =
        new DireccionDTO("Calle", 123, null, null, "1000", "CABA", "Buenos Aires", "Argentina");
    assertTrue(
        validator
            .validate(
                new CrearEntregaRequestDTO(UUID.randomUUID(), UUID.randomUUID(), dir, 10f, 2f))
            .isEmpty());
  }

  @Test
  void crearEntrega_idDonacionNulo_violacion() {
    DireccionDTO dir =
        new DireccionDTO("Calle", 123, null, null, "1000", "CABA", "Buenos Aires", "Argentina");
    assertFalse(
        validator
            .validate(new CrearEntregaRequestDTO(null, UUID.randomUUID(), dir, 10f, 2f))
            .isEmpty());
  }

  @Test
  void crearEntrega_destinoNulo_violacion() {
    assertFalse(
        validator
            .validate(
                new CrearEntregaRequestDTO(UUID.randomUUID(), UUID.randomUUID(), null, 10f, 2f))
            .isEmpty());
  }

  @Test
  void crearEntrega_volumenNulo_violacion() {
    DireccionDTO dir =
        new DireccionDTO("Calle", 123, null, null, "1000", "CABA", "Buenos Aires", "Argentina");
    assertFalse(
        validator
            .validate(
                new CrearEntregaRequestDTO(UUID.randomUUID(), UUID.randomUUID(), dir, 10f, null))
            .isEmpty());
  }

  @Test
  void crearEntrega_pesoNulo_violacion() {
    DireccionDTO dir =
        new DireccionDTO("Calle", 123, null, null, "1000", "CABA", "Buenos Aires", "Argentina");
    assertFalse(
        validator
            .validate(
                new CrearEntregaRequestDTO(UUID.randomUUID(), UUID.randomUUID(), dir, null, 2f))
            .isEmpty());
  }

  @Test
  void crearEntrega_pesoNegativo_violacion() {
    DireccionDTO dir =
        new DireccionDTO("Calle", 123, null, null, "1000", "CABA", "Buenos Aires", "Argentina");
    assertFalse(
        validator
            .validate(
                new CrearEntregaRequestDTO(UUID.randomUUID(), UUID.randomUUID(), dir, -1f, 2f))
            .isEmpty());
  }

  // ======================== CambioEstadoEntregaRequestDTO ========================
  @Test
  void cambioEstadoEntrega_valido_sinViolaciones() {
    assertTrue(
        validator
            .validate(
                new CambioEstadoEntregaRequestDTO(
                    EstadoEntrega.ENTREGADA, "Comedor Infantil", null, null))
            .isEmpty());
  }

  @Test
  void cambioEstadoEntrega_estadoNulo_violacion() {
    assertFalse(
        validator
            .validate(new CambioEstadoEntregaRequestDTO(null, "Comedor Infantil", null, null))
            .isEmpty());
  }

  @Test
  void cambioEstadoEntrega_actorVacio_violacion() {
    assertFalse(
        validator
            .validate(new CambioEstadoEntregaRequestDTO(EstadoEntrega.ENTREGADA, "   ", null, null))
            .isEmpty());
  }

  @Test
  void cambioEstadoEntrega_actorNulo_violacion() {
    assertFalse(
        validator
            .validate(new CambioEstadoEntregaRequestDTO(EstadoEntrega.ENTREGADA, null, null, null))
            .isEmpty());
  }

  // ======================== DireccionDTO ========================

  @Test
  void direccion_calleVacia_violacion() {
    DireccionDTO dir =
        new DireccionDTO("  ", 123, null, null, "1000", "CABA", "Buenos Aires", "Argentina");
    assertFalse(validator.validate(dir).isEmpty());
  }

  @Test
  void direccion_alturaNula_violacion() {
    DireccionDTO dir =
        new DireccionDTO("Calle", null, null, null, "1000", "CABA", "Buenos Aires", "Argentina");
    assertFalse(validator.validate(dir).isEmpty());
  }

  @Test
  void direccion_paisVacio_violacion() {
    DireccionDTO dir =
        new DireccionDTO("Calle", 123, null, null, "1000", "CABA", "Buenos Aires", "  ");
    assertFalse(validator.validate(dir).isEmpty());
  }

  // ======================== Solicitudes de cambio de estado de Entrega ========================

  @Test
  void confirmarRecepcion_actorNulo_violacion() {
    assertFalse(validator.validate(new ConfirmarRecepcionRequestDTO(null)).isEmpty());
  }

  @Test
  void reportarNoRecepcion_justificacionVacia_violacion() {
    assertFalse(
        validator.validate(new ReportarNoRecepcionRequestDTO("actor", "  ", null)).isEmpty());
  }

  @Test
  void regresarAlDeposito_actorVacio_violacion() {
    assertFalse(validator.validate(new RegresarAlDepositoRequestDTO("  ")).isEmpty());
  }

  @Test
  void adjuntarFoto_urlNula_violacion() {
    assertFalse(validator.validate(new AdjuntarFotoRecepcionRequestDTO(null)).isEmpty());
  }

  // ======================== AgregarEntregaRutaRequestDTO ========================

  @Test
  void agregarEntrega_idNulo_violacion() {
    assertFalse(validator.validate(new AgregarEntregaRutaRequestDTO(null)).isEmpty());
  }

  @Test
  void agregarEntrega_valido_sinViolaciones() {
    assertTrue(validator.validate(new AgregarEntregaRutaRequestDTO(UUID.randomUUID())).isEmpty());
  }

  // ======================== IniciarRutaRequestDTO ========================

  @Test
  void iniciarRuta_choferIdNulo_violacion() {
    assertFalse(validator.validate(new IniciarRutaRequestDTO(null, "admin")).isEmpty());
  }

  @Test
  void iniciarRuta_actorVacio_violacion() {
    assertFalse(validator.validate(new IniciarRutaRequestDTO(UUID.randomUUID(), "  ")).isEmpty());
  }

  // ======================== CambioEstadoRutaRequestDTO ========================

  @Test
  void cambioEstadoRuta_estadoNulo_violacion() {
    assertFalse(validator.validate(new CambioEstadoRutaRequestDTO(null, null, "admin")).isEmpty());
  }

  @Test
  void cambioEstadoRuta_actorVacio_violacion() {
    assertFalse(
        validator
            .validate(new CambioEstadoRutaRequestDTO(EstadoRuta.EN_TRASLADO, null, "  "))
            .isEmpty());
  }

  @Test
  void cambioEstadoRuta_choferIdOpcional_sinViolacion() {
    assertTrue(
        validator
            .validate(new CambioEstadoRutaRequestDTO(EstadoRuta.COMPLETADA, null, "admin"))
            .isEmpty());
  }

  // ======================== CallbackPlanificacionRequestDTO ========================

  @Test
  void callbackPlanificacion_solicitudIdNula_violacion() {
    assertFalse(
        validator.validate(new CallbackPlanificacionRequestDTO(null, null, "OK", null)).isEmpty());
  }

  @Test
  void callbackPlanificacion_estadoVacio_violacion() {
    assertFalse(
        validator
            .validate(new CallbackPlanificacionRequestDTO(UUID.randomUUID(), null, "  ", null))
            .isEmpty());
  }

  @Test
  void callbackPlanificacion_valido_sinViolaciones() {
    assertTrue(
        validator
            .validate(new CallbackPlanificacionRequestDTO(UUID.randomUUID(), null, "OK", null))
            .isEmpty());
  }

  // ======================== RutaPlanificadaDTO ========================

  @Test
  void rutaPlanificada_camionIdNulo_violacion() {
    Set<ConstraintViolation<RutaPlanificadaDTO>> violations =
        validator.validate(
            new RutaPlanificadaDTO(null, UUID.randomUUID(), LocalDate.now(), List.of()));
    assertFalse(violations.isEmpty());
  }

  @Test
  void rutaPlanificada_entregaIdsVacia_violacion() {
    assertFalse(
        validator
            .validate(
                new RutaPlanificadaDTO(
                    UUID.randomUUID(), UUID.randomUUID(), LocalDate.now(), List.of()))
            .isEmpty());
  }

  @Test
  void rutaPlanificada_valido_sinViolaciones() {
    assertTrue(
        validator
            .validate(
                new RutaPlanificadaDTO(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    LocalDate.now(),
                    List.of(UUID.randomUUID())))
            .isEmpty());
  }
}
