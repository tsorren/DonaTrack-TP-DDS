package ar.edu.utn.frba.ddsi.donaciones.models.entities.persona;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JuridicaTest {

  Juridica juridica;
  Humana representante1;
  Humana representante2;

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
