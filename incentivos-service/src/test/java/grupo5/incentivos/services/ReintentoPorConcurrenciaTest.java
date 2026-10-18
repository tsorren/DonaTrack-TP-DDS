package grupo5.incentivos.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;

class ReintentoPorConcurrenciaTest {

  @Test
  void ejecutar_cuandoOtraEscrituraGanaDosVeces_deberiaReintentarHastaGuardar() {
    AtomicInteger intentos = new AtomicInteger();

    ReintentoPorConcurrencia.ejecutar(
        "prueba",
        () -> {
          if (intentos.incrementAndGet() < 3) {
            throw new OptimisticLockingFailureException("versión vieja");
          }
        });

    assertEquals(3, intentos.get());
  }

  @Test
  void ejecutar_cuandoSiempreChoca_deberiaPropagarLaExcepcionTrasAgotarLosIntentos() {
    AtomicInteger intentos = new AtomicInteger();
    Runnable siempreChoca =
        () -> {
          intentos.incrementAndGet();
          throw new OptimisticLockingFailureException("versión vieja");
        };

    assertThrows(
        OptimisticLockingFailureException.class,
        () -> ReintentoPorConcurrencia.ejecutar("prueba", siempreChoca));
    assertEquals(ReintentoPorConcurrencia.INTENTOS, intentos.get());
  }

  @Test
  void ejecutar_conUnErrorQueNoEsDeConcurrencia_noDeberiaReintentar() {
    AtomicInteger intentos = new AtomicInteger();
    Runnable falla =
        () -> {
          intentos.incrementAndGet();
          throw new BusinessStateException(ErrorCatalog.DONANTE_INCENTIVOS_NO_ENCONTRADO);
        };

    assertThrows(
        BusinessStateException.class, () -> ReintentoPorConcurrencia.ejecutar("prueba", falla));
    assertEquals(1, intentos.get());
  }
}
