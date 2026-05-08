package ar.edu.utn.frba.ddsi.donaciones.models.entities.persona;

import static org.junit.jupiter.api.Assertions.*;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.persona.medioDeContacto.Correo;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.persona.medioDeContacto.Telefono;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.persona.medioDeContacto.WhatsApp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MediosDeContactoTest {

  Humana persona;
  Correo correo;
  Telefono telefono;
  WhatsApp whatsApp;

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
