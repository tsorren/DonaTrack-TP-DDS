package grupo5.notificaciones.models.entities.personas;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.notificaciones.models.ports.Anonimizable;
import java.util.ArrayList;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PersonaTest {

  private Persona persona;
  private Correo correo;
  private Telefono telefono;

  @BeforeEach
  void setUp() {
    persona = new Persona(UUID.randomUUID(), new ArrayList<>(), "Juan Perez", TipoPersona.HUMANA);

    correo = new Correo();
    correo.setDireccionCorreo("juan.perez@example.com");

    telefono = new Telefono();
    telefono.setCaracteristica("+54");
    telefono.setCodigoArea("11");
    telefono.setNumero("12345678");

    persona.agregarMedioDeContacto(correo);
    persona.agregarMedioDeContacto(telefono);
  }

  @Test
  void agregarYQuitarMedioDeContacto() {
    assertEquals(2, persona.getMediosDeContacto().size());
    persona.quitarMedioDeContacto(correo);
    assertEquals(1, persona.getMediosDeContacto().size());
    assertEquals(telefono, persona.getMediosDeContacto().getFirst());
  }

  @Test
  void definirMedioDeContactoPredeterminado() {
    persona.definirMedioDeContactoPredeterminado(correo);
    assertTrue(correo.getEsPredeterminado());
    assertFalse(telefono.getEsPredeterminado());

    persona.definirMedioDeContactoPredeterminado(telefono);
    assertFalse(correo.getEsPredeterminado());
    assertTrue(telefono.getEsPredeterminado());
  }

  @Test
  void anonimizar_deberiaAnonimizarDenominacionYContactos() {
    persona.anonimizar();

    assertEquals(Anonimizable.VALOR_STRING, persona.getDenominacion());
    assertEquals(
        Anonimizable.VALOR_STRING,
        ((Correo) persona.getMediosDeContacto().get(0)).getDireccionCorreo());
    assertEquals(
        Anonimizable.VALOR_STRING,
        ((Telefono) persona.getMediosDeContacto().get(1)).getCaracteristica());
  }
}
