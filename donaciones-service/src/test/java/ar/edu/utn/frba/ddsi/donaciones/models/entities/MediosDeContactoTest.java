package ar.edu.utn.frba.ddsi.donaciones.models.entities;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.personas.Humana;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.personas.medioDeContacto.Correo;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.personas.medioDeContacto.Telefono;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.personas.medioDeContacto.WhatsApp;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MediosDeContactoTest {

  private Humana persona;
  private Correo correo;
  private Telefono telefono;
  private WhatsApp whatsApp;

  @BeforeEach
  void setup() {
    correo = new Correo();
    correo.setDireccionCorreo("test@mail.com");

    telefono = new Telefono();
    telefono.setNumero("12345678");

    whatsApp = new WhatsApp();
    whatsApp.setNumero("87654321");

    persona = new Humana("Carlos", "López", LocalDate.of(1992, 8, 20), correo);
  }

  @Test
  void agregarMedioDeContacto() {
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
    assertTrue(persona.getContactos().contains(correo));
  }

  @Test
  void alDefinirPredeterminadoElAnteriorDejaDeSerlo() {
    persona.agregarMedioDeContacto(whatsApp);

    whatsApp.setEsPredeterminado(true);

    persona.definirMedioDeContactoPredeterminado(whatsApp);

    assertTrue(whatsApp.getEsPredeterminado());
    assertFalse(correo.getEsPredeterminado());
  }
}
