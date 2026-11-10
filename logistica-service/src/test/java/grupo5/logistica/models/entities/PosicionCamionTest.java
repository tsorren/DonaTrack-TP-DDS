package grupo5.logistica.models.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.camiones.PosicionCamion;
import grupo5.logistica.testutils.CamionMother;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PosicionCamionTest {

  private static final Instant T0 = Instant.parse("2026-11-08T12:00:00Z");

  @Test
  void rechazaCoordenadasFueraDeRangoYHoraNula() {
    assertThrows(ValidationException.class, () -> new PosicionCamion(91, 0, T0));
    assertThrows(ValidationException.class, () -> new PosicionCamion(0, -181, T0));
    assertThrows(ValidationException.class, () -> new PosicionCamion(Double.NaN, 0, T0));
    assertThrows(ValidationException.class, () -> new PosicionCamion(0, 0, null));
  }

  @Test
  void unaPosicionMasNuevaReemplazaALaGuardada() {
    Camion camion = CamionMother.disponible();
    PosicionCamion primera = new PosicionCamion(-34.60, -58.38, T0);
    PosicionCamion segunda = new PosicionCamion(-34.61, -58.39, T0.plusSeconds(30));

    assertTrue(camion.registrarPosicion(primera));
    assertTrue(camion.registrarPosicion(segunda));
    assertEquals(segunda, camion.getPosicion());
  }

  @Test
  void unaPosicionIgualOMasViejaQueLaGuardadaSeDescarta() {
    Camion camion = CamionMother.disponible();
    PosicionCamion nueva = new PosicionCamion(-34.61, -58.39, T0.plusSeconds(30));
    camion.registrarPosicion(nueva);

    assertFalse(camion.registrarPosicion(new PosicionCamion(-34.60, -58.38, T0)));
    assertFalse(camion.registrarPosicion(new PosicionCamion(-34.70, -58.40, T0.plusSeconds(30))));
    assertEquals(nueva, camion.getPosicion());
  }

  @Test
  void registrarUnaPosicionNulaFalla() {
    Camion camion = CamionMother.disponible();
    assertThrows(ValidationException.class, () -> camion.registrarPosicion(null));
  }
}
