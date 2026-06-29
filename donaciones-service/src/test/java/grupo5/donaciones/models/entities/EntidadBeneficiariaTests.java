package grupo5.donaciones.models.entities;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.models.entities.beneficiarios.EntidadBeneficiaria;
import grupo5.donaciones.models.entities.personas.Humana;
import grupo5.donaciones.models.entities.personas.Juridica;
import java.time.LocalDate;
import java.time.Month;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EntidadBeneficiariaTests {
  private Juridica juridica;
  private Humana representante;
  private static final LocalDate TEST_DATE = LocalDate.of(2026, Month.JUNE, 9);

  @BeforeEach
  void setUp() {
    representante = new Humana("Juan", "Perez", TEST_DATE.minusYears(25));
    juridica = new Juridica(representante);
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
