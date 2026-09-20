package grupo5.donaciones.models.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.fixtures.PersonaMother;
import grupo5.donaciones.models.entities.beneficiarios.EntidadBeneficiaria;
import grupo5.donaciones.models.entities.personas.Juridica;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EntidadBeneficiariaTest {

  private Juridica juridica;

  @BeforeEach
  void setUp() {
    juridica = PersonaMother.empresaSA();
  }

  @Test
  void crearEntidadBeneficiaria_conJuridicaValida_deberiaInicializarCorrectamente() {
    EntidadBeneficiaria entidad = new EntidadBeneficiaria(juridica.getId());

    assertNotNull(entidad.getId());
    assertEquals(juridica.getId(), entidad.juridicaId());
  }

  @Test
  void crearEntidadBeneficiaria_conJuridicaNula_deberiaLanzarValidationException() {
    assertThrows(ValidationException.class, () -> new EntidadBeneficiaria(null));
  }
}
