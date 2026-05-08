package ar.edu.utn.frba.ddsi.donaciones.models.entities;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.personas.Humana;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.personas.Juridica;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JuridicaTest {

  private Juridica juridica;
  private Humana representante1;
  private Humana representante2;

  @BeforeEach
  void setup() {
    juridica = new Juridica();

    representante1 = new Humana();
    representante2 = new Humana();
  }

  @Test
  void agregarRepresentante() {
    juridica.agregarRepresentante(representante1);

    assertTrue(juridica.getRepresentantes().contains(representante1));
  }

  @Test
  void quitarRepresentante() {
    juridica.agregarRepresentante(representante1);
    juridica.agregarRepresentante(representante2);

    juridica.quitarRepresentante(representante1);

    assertFalse(juridica.getRepresentantes().contains(representante1));
    assertTrue(juridica.getRepresentantes().contains(representante2));
  }
}
