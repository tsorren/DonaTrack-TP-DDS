package ar.edu.utn.frba.ddsi.donaciones.models.entities;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.personas.Humana;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.personas.medioDeContacto.Correo;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.personas.medioDeContacto.Telefono;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.personas.medioDeContacto.WhatsApp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MediosDeContactoTest {

  private Humana persona;
  private Correo correo;
  private Telefono telefono;
  private WhatsApp whatsApp;

  @BeforeEach
  void setup() {
    persona = new Humana();

    correo = new Correo();
    correo.setEsPredeterminado(false);

    telefono = new Telefono();
    telefono.setEsPredeterminado(false);

    whatsApp = new WhatsApp();
    whatsApp.setEsPredeterminado(false);
  }

  @Test
  void agregarMedioDeContacto() {
    persona.agregarMedioDeContacto(correo);
    persona.agregarMedioDeContacto(telefono);

    assertTrue(persona.getContactos().contains(correo));
    assertTrue(persona.getContactos().contains(telefono));
  }

  @Test
  void quitarMedioDeContacto() {
    persona.agregarMedioDeContacto(telefono);
    persona.agregarMedioDeContacto(whatsApp);

    persona.quitarMedioDeContacto(telefono);

    assertFalse(persona.getContactos().contains(telefono));
    assertTrue(persona.getContactos().contains(whatsApp));
  }

  @Test
  void alDefinirPredeterminadoElAnteriorDejaDeSerlo() {
    persona.agregarMedioDeContacto(correo);
    persona.agregarMedioDeContacto(whatsApp);

    whatsApp.setEsPredeterminado(true);

    persona.definirMedioDeContactoPredeterminado(correo);

    assertFalse(whatsApp.getEsPredeterminado());
    assertTrue(correo.getEsPredeterminado());
  }
}
