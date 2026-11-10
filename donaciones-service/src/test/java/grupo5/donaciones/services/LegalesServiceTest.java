package grupo5.donaciones.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.dto.personas.LegalesDTO;
import grupo5.donaciones.fixtures.PersonaMother;
import grupo5.donaciones.models.entities.personas.Humana;
import grupo5.donaciones.models.repositories.impl.AceptacionesLegalesRepositoryEnMemoria;
import grupo5.donaciones.models.repositories.impl.PersonasRepositoryEnMemoria;
import grupo5.donaciones.services.impl.LegalesService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LegalesServiceTest {

  private final AceptacionesLegalesRepositoryEnMemoria aceptaciones =
      new AceptacionesLegalesRepositoryEnMemoria();
  private final PersonasRepositoryEnMemoria personas = new PersonasRepositoryEnMemoria();
  private Humana juan;

  @BeforeEach
  void setUp() {
    juan = PersonaMother.juanPerez();
    personas.save(juan);
  }

  private LegalesService conVigente(int version) {
    return new LegalesService(aceptaciones, personas, version);
  }

  @Test
  void sinAceptarNingunaVersion_laVigenteFigura_comoPendiente() {
    LegalesDTO legales = conVigente(1).consultar(juan.getId());

    assertNull(legales.versionAceptada());
    assertNull(legales.aceptadaEn());
    assertEquals(1, legales.versionVigente());
    assertFalse(legales.vigenteAceptada());
  }

  @Test
  void aceptarLaVigente_dejaConstanciaConVersionYFecha() {
    LegalesDTO legales = conVigente(1).aceptar(juan.getId(), 1);

    assertEquals(1, legales.versionAceptada());
    assertTrue(legales.vigenteAceptada());
    assertEquals(legales, conVigente(1).consultar(juan.getId()));
  }

  @Test
  void aceptarDosVecesLaMismaVersion_conservaLaPrimeraConstancia() {
    LegalesDTO primera = conVigente(1).aceptar(juan.getId(), 1);
    LegalesDTO segunda = conVigente(1).aceptar(juan.getId(), 1);

    assertEquals(primera, segunda);
    assertEquals(1, aceptaciones.findAll().size());
  }

  @Test
  void conUnaVersionNueva_laAnteriorNoAlcanza_yLaNuevaSeAgrega() {
    conVigente(1).aceptar(juan.getId(), 1);
    LegalesService conVersion2 = conVigente(2);

    assertFalse(conVersion2.consultar(juan.getId()).vigenteAceptada());
    assertTrue(conVersion2.aceptar(juan.getId(), 2).vigenteAceptada());
    assertEquals(2, aceptaciones.findAll().size());
  }

  @Test
  void unaVersionQueNoEsLaVigente_responde400() {
    ValidationException error =
        assertThrows(ValidationException.class, () -> conVigente(2).aceptar(juan.getId(), 1));

    assertEquals(ErrorCatalog.LEGALES_VERSION_NO_VIGENTE.getCode(), error.getErrorCode());
  }

  @Test
  void unaPersonaQueNoExiste_responde404() {
    UUID nadie = UUID.randomUUID();
    LegalesService legales = conVigente(1);

    assertThrows(RecursoNoEncontradoException.class, () -> legales.consultar(nadie));
    assertThrows(RecursoNoEncontradoException.class, () -> legales.aceptar(nadie, 1));
  }
}
