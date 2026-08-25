package grupo5.donaciones.models.entities.itemsNormalizados;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.donaciones.models.entities.donaciones.Bien;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EvaluadorNormalizacionTest {

  private UUID donacionId;
  private UUID subcategoriaId;
  private Bien bien;

  @BeforeEach
  void setUp() {
    donacionId = UUID.randomUUID();
    subcategoriaId = UUID.randomUUID();
    bien = new Bien("Mesa", "foto.png", null, null, 5.0, 0.2);
  }

  @Test
  void estanTodosNormalizados_conListaNulaOVacia_debeRetornarFalse() {
    assertFalse(EvaluadorNormalizacion.estanTodosNormalizados(null));
    assertFalse(EvaluadorNormalizacion.estanTodosNormalizados(Collections.emptyList()));
  }

  @Test
  void estanTodosNormalizados_cuandoAlMenosUnItemEstaPendiente_debeRetornarFalse() {
    BienNormalizado bnAceptado =
        new BienNormalizado(bien, subcategoriaId, 1.0, EstadoNormalizacion.ACEPTADO, false, false);
    BienNormalizado bnPendiente =
        new BienNormalizado(
            bien, subcategoriaId, 0.4, EstadoNormalizacion.PENDIENTE_REVISION, false, false);

    ItemDonacionNormalizado item1 = new ItemDonacionNormalizado(donacionId, bnAceptado, 2);
    ItemDonacionNormalizado item2 = new ItemDonacionNormalizado(donacionId, bnPendiente, 1);

    assertFalse(EvaluadorNormalizacion.estanTodosNormalizados(List.of(item1, item2)));
  }

  @Test
  void estanTodosNormalizados_cuandoTodosLosItemsEstanAceptadosORechazados_debeRetornarTrue() {
    BienNormalizado bnAceptado =
        new BienNormalizado(bien, subcategoriaId, 1.0, EstadoNormalizacion.ACEPTADO, false, false);
    BienNormalizado bnRechazado =
        new BienNormalizado(bien, subcategoriaId, 0.0, EstadoNormalizacion.RECHAZADO, false, false);

    ItemDonacionNormalizado item1 = new ItemDonacionNormalizado(donacionId, bnAceptado, 2);
    ItemDonacionNormalizado item2 = new ItemDonacionNormalizado(donacionId, bnRechazado, 1);

    assertTrue(EvaluadorNormalizacion.estanTodosNormalizados(List.of(item1, item2)));
  }

  @Test
  void itemDonacionNormalizado_metodosSemanticos_respondenCorrectamente() {
    BienNormalizado bnPendiente =
        new BienNormalizado(
            bien, subcategoriaId, 0.4, EstadoNormalizacion.PENDIENTE_REVISION, false, false);
    ItemDonacionNormalizado item = new ItemDonacionNormalizado(donacionId, bnPendiente, 1);

    assertTrue(item.estaPendienteDeRevision());
    assertFalse(item.estaResuelto());
    assertFalse(item.estaAceptado());

    BienNormalizado bnAceptado =
        new BienNormalizado(bien, subcategoriaId, 1.0, EstadoNormalizacion.ACEPTADO, false, false);
    item.actualizarBien(bnAceptado);

    assertFalse(item.estaPendienteDeRevision());
    assertTrue(item.estaResuelto());
    assertTrue(item.estaAceptado());
  }
}
