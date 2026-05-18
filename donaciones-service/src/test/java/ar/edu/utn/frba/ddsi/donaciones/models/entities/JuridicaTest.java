package ar.edu.utn.frba.ddsi.donaciones.models.entities;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.personas.Humana;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.personas.Juridica;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.personas.medioDeContacto.Correo;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JuridicaTest {

  private Juridica juridica;
  private Humana representante1;
  private Humana representante2;

  @BeforeEach
  void setup() {
    Correo correoRep1 = new Correo();
    correoRep1.setDireccionCorreo("rep1@mail.com");

    Correo correoRep2 = new Correo();
    correoRep2.setDireccionCorreo("rep2@mail.com");

    Correo correoJuridica = new Correo();
    correoJuridica.setDireccionCorreo("contacto@empresa.com");

    representante1 = new Humana("Juan", "Pérez", LocalDate.of(1990, 1, 1), correoRep1);
    representante2 = new Humana("Ana", "Gómez", LocalDate.of(1985, 5, 15), correoRep2);

    juridica = new Juridica(representante1, correoJuridica);
  }

  @Test
  void agregarRepresentante() {
    juridica.agregarRepresentante(representante2);

    assertTrue(juridica.getRepresentantes().contains(representante1));
    assertTrue(juridica.getRepresentantes().contains(representante2));
  }

  @Test
  void quitarRepresentante() {
    juridica.agregarRepresentante(representante2);

    juridica.quitarRepresentante(representante1);

    assertFalse(juridica.getRepresentantes().contains(representante1));
    assertTrue(juridica.getRepresentantes().contains(representante2));
  }
}
