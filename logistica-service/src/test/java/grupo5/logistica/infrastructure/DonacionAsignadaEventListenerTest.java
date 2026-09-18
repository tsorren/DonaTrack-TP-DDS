package grupo5.logistica.infrastructure;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.dto.entregas.CrearEntregaRequestDTO;
import grupo5.logistica.dto.eventos.DestinoEventoDTO;
import grupo5.logistica.dto.eventos.EventoDonacionAsignadaV1;
import grupo5.logistica.models.repositories.IEntregasRepository;
import grupo5.logistica.services.IEntregasService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DonacionAsignadaEventListenerTest {

  @Mock private IEntregasService entregasService;
  @Mock private IEntregasRepository entregasRepository;

  private DonacionAsignadaEventListener listener;

  @BeforeEach
  void setUp() {
    listener = new DonacionAsignadaEventListener(entregasService, entregasRepository);
  }

  private static EventoDonacionAsignadaV1 eventoDeEjemplo(UUID donacionId) {
    DestinoEventoDTO destino =
        new DestinoEventoDTO(
            "Av. Medrano", 951, null, null, "C1179AAQ", "CABA", "Buenos Aires", "Argentina");
    return new EventoDonacionAsignadaV1(
        donacionId,
        UUID.randomUUID(),
        LocalDateTime.now(),
        UUID.randomUUID(),
        "Caja de alimentos no perecederos",
        destino,
        10.5,
        0.25,
        List.of("ALIMENTOS"),
        10);
  }

  /**
   * Arma un evento de ejemplo y deja stubeada la consulta de idempotencia para ese mismo
   * donacionIndependienteId, que es el preámbulo común a todos los casos salvo el del evento nulo.
   */
  private EventoDonacionAsignadaV1 eventoConIdempotencia(boolean yaProcesada) {
    EventoDonacionAsignadaV1 evento = eventoDeEjemplo(UUID.randomUUID());
    when(entregasRepository.existsByIdDonacion(evento.donacionIndependienteId()))
        .thenReturn(yaProcesada);
    return evento;
  }

  @Test
  void onDonacionAsignada_cuandoEventoNuevo_creaLaEntregaConLosDatosMapeados() {
    EventoDonacionAsignadaV1 evento = eventoConIdempotencia(false);

    listener.onDonacionAsignada(evento);

    ArgumentCaptor<CrearEntregaRequestDTO> captor =
        ArgumentCaptor.forClass(CrearEntregaRequestDTO.class);
    verify(entregasService).crear(captor.capture());

    CrearEntregaRequestDTO dto = captor.getValue();
    assertEquals(evento.donacionIndependienteId(), dto.idDonacion());
    assertEquals(evento.personaBeneficiariaId(), dto.idBeneficiaria());
    assertEquals(10.5f, dto.pesoTotalKG());
    assertEquals(0.25f, dto.volumenTotalM3());
    assertEquals("Av. Medrano", dto.destino().calle());
    assertEquals("CABA", dto.destino().localidad());
  }

  @Test
  void onDonacionAsignada_cuandoEventoDuplicado_noCreaNadaYNoRompe() {
    EventoDonacionAsignadaV1 evento = eventoConIdempotencia(true);

    assertDoesNotThrow(() -> listener.onDonacionAsignada(evento));

    verify(entregasService, never()).crear(any());
  }

  @Test
  void onDonacionAsignada_cuandoServicioLanzaValidationException_noRelanzaLaExcepcion() {
    EventoDonacionAsignadaV1 evento = eventoConIdempotencia(false);
    when(entregasService.crear(any()))
        .thenThrow(new ValidationException(ErrorCatalog.ARGUMENTO_NULO));

    assertDoesNotThrow(() -> listener.onDonacionAsignada(evento));
  }

  @Test
  void onDonacionAsignada_cuandoServicioLanzaExcepcionTransitoria_sePropagaParaRetryYDlq() {
    EventoDonacionAsignadaV1 evento = eventoConIdempotencia(false);
    when(entregasService.crear(any()))
        .thenThrow(new RuntimeException("timeout de infraestructura"));

    assertThrows(RuntimeException.class, () -> listener.onDonacionAsignada(evento));
  }

  @Test
  void onDonacionAsignada_cuandoEventoEsNulo_noRompeYNoLlamaAlServicio() {
    assertDoesNotThrow(() -> listener.onDonacionAsignada(null));

    verifyNoInteractions(entregasService);
  }
}
