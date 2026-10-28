package grupo5.incentivos.models.repositories;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import grupo5.incentivos.fixtures.DonanteIncentivosMother;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;

class EventosProcesadosEnMemoriaTest {

  @Test
  void guardarSiNoProcesado_conEventoNuevo_guardaYDevuelveTrue() {
    IDonanteIncentivosRepository repo = mock(IDonanteIncentivosRepository.class);
    EventosProcesadosEnMemoria inbox = new EventosProcesadosEnMemoria(repo);
    DonanteIncentivos donante = DonanteIncentivosMother.colaboradorSinMisiones(UUID.randomUUID());

    assertTrue(inbox.guardarSiNoProcesado(UUID.randomUUID(), donante));

    verify(repo).save(donante);
  }

  @Test
  void guardarSiNoProcesado_conEventoRepetido_noGuardaYDevuelveFalse() {
    IDonanteIncentivosRepository repo = mock(IDonanteIncentivosRepository.class);
    EventosProcesadosEnMemoria inbox = new EventosProcesadosEnMemoria(repo);
    DonanteIncentivos donante = DonanteIncentivosMother.colaboradorSinMisiones(UUID.randomUUID());
    UUID eventoId = UUID.randomUUID();

    assertTrue(inbox.guardarSiNoProcesado(eventoId, donante));
    assertFalse(inbox.guardarSiNoProcesado(eventoId, donante));

    verify(repo, times(1)).save(donante);
  }

  @Test
  void yaProcesado_soloEsTrueDespuesDeGuardarConExito() {
    IDonanteIncentivosRepository repo = mock(IDonanteIncentivosRepository.class);
    EventosProcesadosEnMemoria inbox = new EventosProcesadosEnMemoria(repo);
    DonanteIncentivos donante = DonanteIncentivosMother.colaboradorSinMisiones(UUID.randomUUID());
    UUID eventoId = UUID.randomUUID();

    assertFalse(inbox.yaProcesado(eventoId));
    inbox.guardarSiNoProcesado(eventoId, donante);

    assertTrue(inbox.yaProcesado(eventoId));
  }

  @Test
  void guardarSiNoProcesado_cuandoElGuardadoFalla_liberaLaMarcaParaPoderReintentar() {
    IDonanteIncentivosRepository repo = mock(IDonanteIncentivosRepository.class);
    EventosProcesadosEnMemoria inbox = new EventosProcesadosEnMemoria(repo);
    DonanteIncentivos donante = DonanteIncentivosMother.colaboradorSinMisiones(UUID.randomUUID());
    UUID eventoId = UUID.randomUUID();
    when(repo.save(any()))
        .thenThrow(new OptimisticLockingFailureException("choque"))
        .thenReturn(donante);

    assertThrows(
        OptimisticLockingFailureException.class,
        () -> inbox.guardarSiNoProcesado(eventoId, donante));

    assertTrue(inbox.guardarSiNoProcesado(eventoId, donante));
  }

  @Test
  void guardarSiNoProcesado_sinEventoId_guardaSiempreSinDeduplicar() {
    IDonanteIncentivosRepository repo = mock(IDonanteIncentivosRepository.class);
    EventosProcesadosEnMemoria inbox = new EventosProcesadosEnMemoria(repo);
    DonanteIncentivos donante = DonanteIncentivosMother.colaboradorSinMisiones(UUID.randomUUID());

    assertTrue(inbox.guardarSiNoProcesado(null, donante));
    assertTrue(inbox.guardarSiNoProcesado(null, donante));

    verify(repo, times(2)).save(donante);
  }
}
