package grupo5.donaciones.models.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.donaciones.models.entities.beneficiarios.EntidadBeneficiaria;
import grupo5.donaciones.models.entities.categorias.Categoria;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.categorias.Unidad;
import grupo5.donaciones.models.entities.necesidades.NecesidadExtraordinaria;
import grupo5.donaciones.models.entities.personas.Humana;
import grupo5.donaciones.models.entities.personas.Juridica;
import java.time.LocalDate;
import java.time.Month;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EntidadBeneficiariaTests {
  private EntidadBeneficiaria entidad;
  private NecesidadExtraordinaria necesidad;
  private static final LocalDate TEST_DATE = LocalDate.of(2026, Month.JUNE, 9);

  @BeforeEach
  void setUp() {
    Humana representante = new Humana("Juan", "Perez", TEST_DATE.minusYears(25));
    Juridica juridica = new Juridica(representante);
    entidad = new EntidadBeneficiaria(juridica);

    Categoria categoria = new Categoria("Mueble", false, true, Unidad.UNIDADES);
    Subcategoria subcategoria = new Subcategoria(categoria, "Muebles Escolares");
    necesidad = new NecesidadExtraordinaria(subcategoria, 30, "30 bancos y sillas para el aula");
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
