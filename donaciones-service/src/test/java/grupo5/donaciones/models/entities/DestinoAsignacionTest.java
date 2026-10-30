package grupo5.donaciones.models.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import grupo5.donaciones.fixtures.DonacionIndependienteMother;
import grupo5.donaciones.fixtures.NecesidadMother;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.necesidades.NecesidadExtraordinaria;
import grupo5.donaciones.models.entities.necesidades.NecesidadRecurrente;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Reemplaza a AsignableTest (DTI-06): la donación ya no guarda el objeto al que se asignó, sino los
 * ids de la necesidad, el período y la entidad.
 */
class DestinoAsignacionTest {

  private UUID entidadId;
  private DonacionIndependiente donacion;

  @BeforeEach
  void setUp() {
    entidadId = UUID.randomUUID();
    donacion = DonacionIndependienteMother.crearParaSubcategoria(UUID.randomUUID(), 10);
  }

  @Test
  void asignarAExtraordinaria_guardaLaNecesidadYLaEntidadSinPeriodo() {
    NecesidadExtraordinaria necesidad =
        NecesidadMother.extraordinaria(entidadId, UUID.randomUUID(), 10);

    donacion.asignar("SISTEMA", necesidad.destinoDeAsignacion());

    assertEquals(necesidad.getId(), donacion.getNecesidadId());
    assertNull(donacion.getPeriodoId());
    assertEquals(entidadId, donacion.getEntidadBeneficiariaId());
  }

  @Test
  void donacionNoAsignada_noTieneDestino() {
    assertNull(donacion.getNecesidadId());
    assertNull(donacion.getPeriodoId());
    assertNull(donacion.getEntidadBeneficiariaId());
  }

  @Test
  void asignarARecurrente_guardaElPeriodoActualYLaNecesidadPadre() {
    NecesidadRecurrente necesidad =
        NecesidadMother.recurrenteSemanal(entidadId, UUID.randomUUID(), 10);

    donacion.asignar("SISTEMA", necesidad.destinoDeAsignacion());

    assertEquals(necesidad.getId(), donacion.getNecesidadId());
    assertEquals(necesidad.obtenerPeriodoActual().id(), donacion.getPeriodoId());
    assertEquals(entidadId, donacion.getEntidadBeneficiariaId());
  }
}
