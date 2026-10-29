package grupo5.incentivos.infrastructure;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.incentivos.config.RabbitMQConfig;
import grupo5.incentivos.dto.RegistrarDonanteRequest;
import grupo5.incentivos.dto.events.EventoDonanteDadoDeBajaV1;
import grupo5.incentivos.dto.events.EventoDonanteRegistradoV1;
import grupo5.incentivos.services.IGestionDonanteService;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

@ExtendWith(MockitoExtension.class)
class DonanteCicloDeVidaListenerTest {

  @Mock private IGestionDonanteService gestionDonanteService;

  private DonanteCicloDeVidaListener listener;

  @BeforeEach
  void setUp() {
    Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    listener = new DonanteCicloDeVidaListener(gestionDonanteService, validator);
  }

  @Test
  @DisplayName("onDonanteRegistrado mapea a RegistrarDonanteRequest y registra el donante")
  void onDonanteRegistrado_exitoso() {
    UUID donanteId = UUID.randomUUID();
    UUID personaId = UUID.randomUUID();
    String nombre = "Carlos Gardel";
    EventoDonanteRegistradoV1 evento = new EventoDonanteRegistradoV1(donanteId, personaId, nombre);

    listener.onDonanteRegistrado(evento);

    ArgumentCaptor<RegistrarDonanteRequest> captor =
        ArgumentCaptor.forClass(RegistrarDonanteRequest.class);
    verify(gestionDonanteService).registrarDonante(captor.capture());

    RegistrarDonanteRequest req = captor.getValue();
    assertEquals(donanteId, req.idDonante());
    assertEquals(personaId, req.idPersona());
    assertEquals(nombre, req.nombre());
  }

  @Test
  @DisplayName("onDonanteDadoDeBaja ejecuta darDeBaja con el donanteId correcto")
  void onDonanteDadoDeBaja_exitoso() {
    UUID donanteId = UUID.randomUUID();
    EventoDonanteDadoDeBajaV1 evento = new EventoDonanteDadoDeBajaV1(donanteId);

    listener.onDonanteDadoDeBaja(evento);

    verify(gestionDonanteService).darDeBaja(donanteId);
  }

  @Test
  @DisplayName("onDonanteDadoDeBaja absorbe idempotentemente DONANTE_INCENTIVOS_NO_ENCONTRADO")
  void onDonanteDadoDeBaja_cuandoDonanteNoExiste_noLanzaExcepcion() {
    UUID donanteId = UUID.randomUUID();
    EventoDonanteDadoDeBajaV1 evento = new EventoDonanteDadoDeBajaV1(donanteId);

    doThrow(new BusinessStateException(ErrorCatalog.DONANTE_INCENTIVOS_NO_ENCONTRADO))
        .when(gestionDonanteService)
        .darDeBaja(donanteId);

    assertDoesNotThrow(() -> listener.onDonanteDadoDeBaja(evento));
  }

  @Test
  @DisplayName("onDonanteDadoDeBaja propaga excepciones de negocio no esperadas para rechazo/DLQ")
  void onDonanteDadoDeBaja_cuandoOcurreOtraExcepcionDeNegocio_propagaExcepcion() {
    UUID donanteId = UUID.randomUUID();
    EventoDonanteDadoDeBajaV1 evento = new EventoDonanteDadoDeBajaV1(donanteId);

    doThrow(new BusinessStateException(ErrorCatalog.INSIGNIA_NO_ENCONTRADA))
        .when(gestionDonanteService)
        .darDeBaja(donanteId);

    assertThrows(BusinessStateException.class, () -> listener.onDonanteDadoDeBaja(evento));
  }

  @Test
  @DisplayName("onDonanteRegistrado lanza ConstraintViolationException con campos inválidos")
  void onDonanteRegistrado_conCamposInvalidos_lanzaConstraintViolationException() {
    EventoDonanteRegistradoV1 invalido = new EventoDonanteRegistradoV1(null, null, " ");

    assertThrows(ConstraintViolationException.class, () -> listener.onDonanteRegistrado(invalido));
    verifyNoInteractions(gestionDonanteService);
  }

  @Test
  @DisplayName("onDonanteDadoDeBaja lanza ConstraintViolationException si falta el donanteId")
  void onDonanteDadoDeBaja_conDonanteIdNulo_lanzaConstraintViolationException() {
    EventoDonanteDadoDeBajaV1 invalido = new EventoDonanteDadoDeBajaV1(null);

    assertThrows(ConstraintViolationException.class, () -> listener.onDonanteDadoDeBaja(invalido));
    verifyNoInteractions(gestionDonanteService);
  }

  @Test
  @DisplayName("El listener consume la cola de ciclo de vida con un único consumidor")
  void listener_escuchaColaUnicaConConcurrenciaUno() {
    RabbitListener anotacion = DonanteCicloDeVidaListener.class.getAnnotation(RabbitListener.class);

    assertNotNull(anotacion);
    assertArrayEquals(
        new String[] {RabbitMQConfig.QUEUE_INCENTIVOS_DONANTE_CICLO_DE_VIDA}, anotacion.queues());
    assertEquals("1", anotacion.concurrency());
  }
}
