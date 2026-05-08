package ar.edu.utn.frba.ddsi.donaciones;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.beneficiarios.NecesidadExtraordinaria;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EntidadBeneficiariaTests {
  private EntidadBeneficiaria entidad;
  private NecesidadExtraordinaria necesidad;

  @BeforeEach
  void setUp() {
    entidad = new EntidadBeneficiaria();
    entidad.setNecesidades(new ArrayList<>());

    necesidad = new NecesidadExtraordinaria();
    necesidad.setDescripcion("30 bancos y sillas para el aula");
  }

  @Test
  void agregarNecesidad_deberiaIncrementarElTamanioDeLaLista() {
    entidad.agregarNecesidad(necesidad);

    assertEquals(1, entidad.getNecesidades().size());
    assertTrue(entidad.getNecesidades().contains(necesidad));
  }

  @Test
  void quitarNecesidad_deberiaReducirElTamanioDeLaLista() {
    entidad.agregarNecesidad(necesidad);
    entidad.quitarNecesidad(necesidad);

    assertTrue(entidad.getNecesidades().isEmpty());
  }
}
