package grupo5.incentivos.infrastructure;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.incentivos.dto.DonacionExitosaRequest;
import grupo5.incentivos.dto.ModificarDonanteRequest;
import grupo5.incentivos.dto.NuevaDonacionRequest;
import grupo5.incentivos.dto.RegistrarDonanteRequest;
import grupo5.incentivos.dto.events.EventoDonacionRecibidaV1;
import grupo5.incentivos.dto.events.EventoDonacionSegmentadaV1;
import grupo5.incentivos.dto.events.EventoDonanteDadoDeBajaV1;
import grupo5.incentivos.dto.events.EventoDonanteRegistradoV1;
import grupo5.incentivos.dto.events.EventoPersonaSincronizadaV1;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import grupo5.incentivos.services.IGestionDonanteService;
import grupo5.incentivos.services.IMisionesDonacionService;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IncentivosEventosListenerTest {

  @Mock private IGestionDonanteService gestionDonanteService;
  @Mock private IMisionesDonacionService misionesDonacionService;

  private Validator validator;
  private IncentivosEventosListener listener;

  @BeforeEach
  void setUp() {
    validator = Validation.buildDefaultValidatorFactory().getValidator();
    listener =
        new IncentivosEventosListener(gestionDonanteService, misionesDonacionService, validator);
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
  @DisplayName("onDonacionSegmentada extrae categorías únicas, suma cantidades y delega")
  void onDonacionSegmentada_exitoso() {
    UUID donanteId = UUID.randomUUID();
    LocalDateTime fecha = LocalDateTime.now().minusHours(1);
    List<EventoDonacionSegmentadaV1.Item> items =
        List.of(
            new EventoDonacionSegmentadaV1.Item("ALIMENTOS", 3),
            new EventoDonacionSegmentadaV1.Item("ROPA", 2),
            new EventoDonacionSegmentadaV1.Item("ALIMENTOS", 5));
    EventoDonacionSegmentadaV1 evento = new EventoDonacionSegmentadaV1(donanteId, items, fecha);

    listener.onDonacionSegmentada(evento);

    ArgumentCaptor<NuevaDonacionRequest> captor =
        ArgumentCaptor.forClass(NuevaDonacionRequest.class);
    verify(misionesDonacionService).procesarDonacion(captor.capture());

    NuevaDonacionRequest req = captor.getValue();
    assertEquals(donanteId, req.donanteId());
    assertEquals(2, req.categorias().size());
    assertTrue(req.categorias().containsAll(List.of("ALIMENTOS", "ROPA")));
    assertEquals(10, req.cantidadBienes());
  }

  @Test
  @DisplayName("onDonacionRecibida delega en procesarDonacionExitosa")
  void onDonacionRecibida_exitoso() {
    UUID donanteId = UUID.randomUUID();
    UUID beneficiarioId = UUID.randomUUID();
    EventoDonacionRecibidaV1 evento = new EventoDonacionRecibidaV1(donanteId, beneficiarioId);

    listener.onDonacionRecibida(evento);

    ArgumentCaptor<DonacionExitosaRequest> captor =
        ArgumentCaptor.forClass(DonacionExitosaRequest.class);
    verify(misionesDonacionService).procesarDonacionExitosa(captor.capture());

    DonacionExitosaRequest req = captor.getValue();
    assertEquals(donanteId, req.donanteId());
    assertEquals(beneficiarioId, req.organizacionId());
  }

  @Test
  @DisplayName("onPersonaSincronizada modifica el donante cuando existe y cambió su denominación")
  void onPersonaSincronizada_cuandoExisteYCambiaNombre_actualiza() {
    UUID personaId = UUID.randomUUID();
    UUID donanteId = UUID.randomUUID();
    EventoPersonaSincronizadaV1 evento =
        new EventoPersonaSincronizadaV1(personaId, "Nuevo Nombre SRL");

    DonanteIncentivos mockDonante = mock(DonanteIncentivos.class);
    when(mockDonante.getId()).thenReturn(donanteId);
    when(mockDonante.getNombre()).thenReturn("Viejo Nombre SA");
    when(gestionDonanteService.buscarDonantePorPersonaId(personaId))
        .thenReturn(Optional.of(mockDonante));

    listener.onPersonaSincronizada(evento);

    ArgumentCaptor<ModificarDonanteRequest> captor =
        ArgumentCaptor.forClass(ModificarDonanteRequest.class);
    verify(gestionDonanteService).modificarDonante(eq(donanteId), captor.capture());
    assertEquals("Nuevo Nombre SRL", captor.getValue().nombre());
  }

  @Test
  @DisplayName("onPersonaSincronizada no modifica el donante si el nombre ya es idéntico")
  void onPersonaSincronizada_cuandoExisteYNombreEsIdentico_noActualiza() {
    UUID personaId = UUID.randomUUID();
    UUID donanteId = UUID.randomUUID();
    EventoPersonaSincronizadaV1 evento = new EventoPersonaSincronizadaV1(personaId, "Mismo Nombre");

    DonanteIncentivos mockDonante = mock(DonanteIncentivos.class);
    when(mockDonante.getNombre()).thenReturn("Mismo Nombre");
    when(gestionDonanteService.buscarDonantePorPersonaId(personaId))
        .thenReturn(Optional.of(mockDonante));

    listener.onPersonaSincronizada(evento);

    verify(gestionDonanteService, never()).modificarDonante(any(), any());
  }

  @Test
  @DisplayName(
      "onPersonaSincronizada no falla ni actualiza si la persona no es donante de incentivos")
  void onPersonaSincronizada_cuandoNoExisteDonante_ignoraSilenciosamente() {
    UUID personaId = UUID.randomUUID();
    EventoPersonaSincronizadaV1 evento = new EventoPersonaSincronizadaV1(personaId, "Persona X");

    when(gestionDonanteService.buscarDonantePorPersonaId(personaId)).thenReturn(Optional.empty());

    assertDoesNotThrow(() -> listener.onPersonaSincronizada(evento));
    verify(gestionDonanteService, never()).modificarDonante(any(), any());
  }

  @Test
  @DisplayName(
      "validar lanza ConstraintViolationException cuando los campos obligatorios son nulos")
  void onCualquierEvento_conCamposInvalidos_lanzaConstraintViolationException() {
    EventoDonanteRegistradoV1 invalido = new EventoDonanteRegistradoV1(null, null, " ");

    assertThrows(ConstraintViolationException.class, () -> listener.onDonanteRegistrado(invalido));
  }
}
