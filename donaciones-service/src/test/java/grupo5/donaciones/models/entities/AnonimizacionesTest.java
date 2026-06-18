package grupo5.donaciones.models.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import grupo5.donaciones.models.entities.personas.Humana;
import grupo5.donaciones.models.entities.personas.Juridica;
import grupo5.donaciones.models.privacidad.Anonimizable;
import java.time.LocalDate;
import java.time.Month;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AnonimizacionesTest {
  private Humana persona;
  private Juridica juridica;

  @BeforeEach
  void setUp() {
    persona = new Humana("Juan", "Pérez", LocalDate.of(1990, Month.JANUARY, 1));
    persona.setDocumento("12345678"); // Suponiendo este setter

    juridica = new Juridica(persona);
    juridica.setRazonSocial("Empresa SA");
  }

  @Test
  void anonimizar_deberiaLimpiarCamposSensibles() {
    persona.anonimizar();

    assertEquals(Anonimizable.VALOR_STRING, persona.getNombre());
    assertEquals(Anonimizable.VALOR_STRING, persona.getApellido());
    assertNull(persona.getDocumento(), "El documento debe ser nulo tras anonimizar");
  }

  @Test
  void anonimizar_deberiaPropagarAnonimizacionAlHijos() {
    juridica.anonimizar();

    assertEquals(Anonimizable.VALOR_STRING, juridica.getRepresentantes().getFirst().getNombre());
  }

  @Test
  void anonimizar_deberiaMantenerIDYLimpiarDatosSensibles() {
    java.util.UUID originalId = persona.getId();
    persona.setDocumento("12345678");

    persona.anonimizar();

    assertEquals(originalId, persona.getId(), "El ID de base de datos NUNCA debe cambiar");
    assertNull(persona.getDocumento(), "El DNI es sensible y debe ser eliminado/nulo");
  }
}
