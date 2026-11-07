package grupo5.donaciones.models.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.fixtures.PersonaMother;
import grupo5.donaciones.models.entities.donantes.Donante;
import grupo5.donaciones.models.entities.personas.Humana;
import org.junit.jupiter.api.Test;

class DonanteTest {

  @Test
  void crearDonante_usaElIdDeLaPersona() {
    Humana persona = PersonaMother.juanPerez();

    Donante donante = new Donante(persona.getId());

    assertEquals(persona.getId(), donante.getId());
    assertEquals(persona.getId(), donante.personaId());
  }

  @Test
  void crearDonante_desdeLaPersona_usaElIdDeLaPersona() {
    Humana persona = PersonaMother.juanPerez();

    assertEquals(persona.getId(), new Donante(persona).getId());
  }

  @Test
  void crearDonante_conPersonaNula_lanzaValidationException() {
    ValidationException ex =
        assertThrows(ValidationException.class, () -> new Donante((java.util.UUID) null));

    assertEquals(ErrorCatalog.DONANTE_SIN_PERSONA, ex.getError());
  }

  @Test
  void donanteNuevo_estaActivo() {
    assertTrue(new Donante(PersonaMother.juanPerez().getId()).estaActivo());
  }

  @Test
  void darDeBaja_desactivaYAvisaQueHuboTransicion() {
    Donante donante = new Donante(PersonaMother.juanPerez().getId());

    assertTrue(donante.darDeBaja());
    assertFalse(donante.estaActivo());
  }

  @Test
  void darDeBaja_sobreUnDonanteInactivo_noHaceNadaYNoAvisaTransicion() {
    Donante donante = new Donante(PersonaMother.juanPerez().getId());
    donante.darDeBaja();

    assertFalse(donante.darDeBaja());
    assertFalse(donante.estaActivo());
  }

  @Test
  void reactivar_sobreUnDonanteInactivo_loActivaYAvisaTransicion() {
    Donante donante = new Donante(PersonaMother.juanPerez().getId());
    donante.darDeBaja();

    assertTrue(donante.reactivar());
    assertTrue(donante.estaActivo());
  }

  @Test
  void reactivar_sobreUnDonanteActivo_noHaceNadaYNoAvisaTransicion() {
    Donante donante = new Donante(PersonaMother.juanPerez().getId());

    assertFalse(donante.reactivar());
    assertTrue(donante.estaActivo());
  }
}
