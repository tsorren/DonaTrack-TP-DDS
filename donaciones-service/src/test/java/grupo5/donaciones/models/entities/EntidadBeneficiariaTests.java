package grupo5.donaciones.models.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.donaciones.models.entities.beneficiarios.EntidadBeneficiaria;
import grupo5.donaciones.models.entities.beneficiarios.NecesidadExtraordinaria;
import grupo5.donaciones.models.entities.bienes.Categoria;
import grupo5.donaciones.models.entities.bienes.SubCategoria;
import grupo5.donaciones.models.entities.bienes.Unidad;
import grupo5.donaciones.models.entities.personas.Humana;
import grupo5.donaciones.models.entities.personas.Juridica;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EntidadBeneficiariaTests {
  private EntidadBeneficiaria entidad;
  private NecesidadExtraordinaria necesidad;

  @BeforeEach
  void setUp() {
    Humana representante = new Humana("Juan", "Perez", LocalDate.now().minusYears(25));
    Juridica juridica = new Juridica(representante);
    entidad = new EntidadBeneficiaria(juridica);

    Categoria categoria = new Categoria("Mueble", false, true, Unidad.UNIDADES);
    SubCategoria subcategoria = new SubCategoria(categoria, "Muebles Escolares");
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
