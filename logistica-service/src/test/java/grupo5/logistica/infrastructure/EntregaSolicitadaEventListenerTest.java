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
import grupo5.logistica.dto.eventos.EventoEntregaSolicitadaV1;
import grupo5.logistica.models.repositories.IEntregasRepository;
import grupo5.logistica.services.IEntregasService;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EntregaSolicitadaEventListenerTest {

  @Mock private IEntregasService entregasService;
  @Mock private IEntregasRepository entregasRepository;

  private EntregaSolicitadaEventListener listener;

  @BeforeEach
  void setUp() {
    listener = new EntregaSolicitadaEventListener(entregasService, entregasRepository);
  }

  private static EventoEntregaSolicitadaV1 comandoDeEjemplo(UUID donacionId) {
    DestinoEventoDTO destino =
        new DestinoEventoDTO(
            "Av. Medrano", 951, null, null, "C1179AAQ", "CABA", "Buenos Aires", "Argentina");
    return new EventoEntregaSolicitadaV1(
        donacionId, UUID.randomUUID(), destino, 10.5, 0.25, LocalDateTime.now());
  }

  /**
   * Arma un comando de ejemplo y deja stubeada la consulta de idempotencia para ese mismo
   * donacionIndependienteId, que es el preámbulo común a todos los casos salvo el del comando nulo.
   */
  private EventoEntregaSolicitadaV1 comandoConIdempotencia(boolean yaProcesada) {
    EventoEntregaSolicitadaV1 comando = comandoDeEjemplo(UUID.randomUUID());
    when(entregasRepository.existsByIdDonacion(comando.donacionIndependienteId()))
        .thenReturn(yaProcesada);
    return comando;
  }

  @Test
  void onEntregaSolicitada_cuandoComandoNuevo_creaLaEntregaConLosDatosMapeados() {
    EventoEntregaSolicitadaV1 comando = comandoConIdempotencia(false);

    listener.onEntregaSolicitada(comando);

    ArgumentCaptor<CrearEntregaRequestDTO> captor =
        ArgumentCaptor.forClass(CrearEntregaRequestDTO.class);
    verify(entregasService).crear(captor.capture());

    CrearEntregaRequestDTO dto = captor.getValue();
    assertEquals(comando.donacionIndependienteId(), dto.idDonacion());
    assertEquals(comando.personaBeneficiariaId(), dto.idBeneficiaria());
    assertEquals(10.5f, dto.pesoTotalKG());
    assertEquals(0.25f, dto.volumenTotalM3());
    assertEquals("Av. Medrano", dto.destino().calle());
    assertEquals("CABA", dto.destino().localidad());
  }

  @Test
  void onEntregaSolicitada_cuandoComandoDuplicado_noCreaNadaYNoRompe() {
    EventoEntregaSolicitadaV1 comando = comandoConIdempotencia(true);

    assertDoesNotThrow(() -> listener.onEntregaSolicitada(comando));

    verify(entregasService, never()).crear(any());
  }

  @Test
  void onEntregaSolicitada_cuandoServicioLanzaValidationException_noRelanzaLaExcepcion() {
    EventoEntregaSolicitadaV1 comando = comandoConIdempotencia(false);
    when(entregasService.crear(any()))
        .thenThrow(new ValidationException(ErrorCatalog.ARGUMENTO_NULO));

    assertDoesNotThrow(() -> listener.onEntregaSolicitada(comando));
  }

  @Test
  void onEntregaSolicitada_cuandoServicioLanzaExcepcionTransitoria_sePropagaParaRetryYDlq() {
    EventoEntregaSolicitadaV1 comando = comandoConIdempotencia(false);
    when(entregasService.crear(any()))
        .thenThrow(new RuntimeException("timeout de infraestructura"));

    assertThrows(RuntimeException.class, () -> listener.onEntregaSolicitada(comando));
  }

  @Test
  void onEntregaSolicitada_cuandoComandoEsNulo_noRompeYNoLlamaAlServicio() {
    assertDoesNotThrow(() -> listener.onEntregaSolicitada(null));

    verifyNoInteractions(entregasService);
  }
}
