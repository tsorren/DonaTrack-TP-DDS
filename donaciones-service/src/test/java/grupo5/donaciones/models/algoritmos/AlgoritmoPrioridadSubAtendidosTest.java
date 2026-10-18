package grupo5.donaciones.models.algoritmos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.fixtures.CategoriaMother;
import grupo5.donaciones.fixtures.DonacionIndependienteMother;
import grupo5.donaciones.fixtures.NecesidadMother;
import grupo5.donaciones.fixtures.PersonaMother;
import grupo5.donaciones.models.entities.beneficiarios.EntidadBeneficiaria;
import grupo5.donaciones.models.entities.categorias.Categoria;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import grupo5.donaciones.models.entities.necesidades.NecesidadExtraordinaria;
import grupo5.donaciones.models.entities.personas.Juridica;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AlgoritmoPrioridadSubAtendidosTest {

  private Subcategoria subcategoria;
  private Subcategoria subcategoriaOtra;
  private DonacionIndependiente donacionEnSubcategoria;
  private DonacionIndependiente donacionEnOtraSubcategoria;
  private AlgoritmoPrioridadSubAtendidos algoritmo;

  @BeforeEach
  void setUp() {
    Categoria categoria = CategoriaMother.ropa();
    subcategoria = CategoriaMother.camperas(categoria);
    subcategoriaOtra = new Subcategoria(categoria.getId(), "Ropa de Verano");

    donacionEnSubcategoria =
        DonacionIndependienteMother.crearParaSubcategoria(subcategoria.getId(), 5);
    donacionEnOtraSubcategoria =
        DonacionIndependienteMother.crearParaSubcategoria(subcategoriaOtra.getId(), 5);

    algoritmo = new AlgoritmoPrioridadSubAtendidos();
  }

  @Test
  void filtrarDonaciones_cuandoMismaSubcategoria_debeIncluirla() {
    NecesidadExtraordinaria necesidad = NecesidadMother.extraordinaria(subcategoria.getId(), 3);
    List<DonacionIndependiente> donaciones = new ArrayList<>(List.of(donacionEnSubcategoria));

    List<DonacionIndependiente> resultado = algoritmo.filtrarDonaciones(necesidad, donaciones);

    assertEquals(1, resultado.size());
    assertTrue(resultado.contains(donacionEnSubcategoria));
  }

  @Test
  void filtrarDonaciones_cuandoDistintaSubcategoria_debeExcluirla() {
    NecesidadExtraordinaria necesidad = NecesidadMother.extraordinaria(subcategoria.getId(), 3);
    List<DonacionIndependiente> donaciones = new ArrayList<>(List.of(donacionEnOtraSubcategoria));

    List<DonacionIndependiente> resultado = algoritmo.filtrarDonaciones(necesidad, donaciones);

    assertTrue(resultado.isEmpty());
  }

  @Test
  void filtrarDonaciones_conListaMixta_soloDebeRetornarLasDeMismaSubcategoria() {
    NecesidadExtraordinaria necesidad = NecesidadMother.extraordinaria(subcategoria.getId(), 3);
    List<DonacionIndependiente> donaciones =
        new ArrayList<>(List.of(donacionEnSubcategoria, donacionEnOtraSubcategoria));

    List<DonacionIndependiente> resultado = algoritmo.filtrarDonaciones(necesidad, donaciones);

    assertEquals(1, resultado.size());
    assertTrue(resultado.contains(donacionEnSubcategoria));
  }

  @Test
  void ordenarNecesidades_cuandoUnaEntidadTieneMayorPorcentajeSatisfecho_debeQuedarAlFinal() {
    Juridica juridicaMuyAtendida = PersonaMother.empresaSA();
    Juridica juridicaPocoAtendida = PersonaMother.fundacionEsperanza();
    EntidadBeneficiaria entidadMuyAtendida = new EntidadBeneficiaria(juridicaMuyAtendida.getId());
    EntidadBeneficiaria entidadPocoAtendida = new EntidadBeneficiaria(juridicaPocoAtendida.getId());

    NecesidadExtraordinaria necesidadActualMuyAtendida =
        NecesidadMother.extraordinaria(entidadMuyAtendida.getId(), subcategoria.getId(), 10);
    necesidadActualMuyAtendida.asignarDonacion(donacionEnSubcategoria);

    NecesidadExtraordinaria necesidadActualPocoAtendida =
        NecesidadMother.extraordinaria(entidadPocoAtendida.getId(), subcategoria.getId(), 5);

    List<Necesidad> necesidades =
        new ArrayList<>(List.of(necesidadActualMuyAtendida, necesidadActualPocoAtendida));

    List<Necesidad> resultado = algoritmo.ordenarNecesidades(necesidades);

    assertEquals(necesidadActualPocoAtendida, resultado.get(0));
    assertEquals(necesidadActualMuyAtendida, resultado.get(1));
  }

  @Test
  void ordenarNecesidades_conNecesidadRecurrenteConDonaciones_debeConsiderarDonacionesRecientes() {
    UUID entidadConRecurrente = UUID.randomUUID();
    UUID entidadSinDonaciones = UUID.randomUUID();

    grupo5.donaciones.models.entities.necesidades.NecesidadRecurrente necesidadRecurrente =
        NecesidadMother.recurrenteSemanal(entidadConRecurrente, subcategoria.getId(), 10);
    necesidadRecurrente.asignarDonacion(donacionEnSubcategoria);

    NecesidadExtraordinaria necesidadSinDonaciones =
        NecesidadMother.extraordinaria(entidadSinDonaciones, subcategoria.getId(), 5);

    List<Necesidad> necesidades =
        new ArrayList<>(List.of(necesidadRecurrente, necesidadSinDonaciones));

    List<Necesidad> resultado = algoritmo.ordenarNecesidades(necesidades);

    assertEquals(necesidadSinDonaciones, resultado.get(0));
    assertEquals(necesidadRecurrente, resultado.get(1));
  }

  @Test
  void ordenarNecesidades_cuandoNingunaTieneEntidad_debeRetornarLasMismas() {
    NecesidadExtraordinaria necesidad1 =
        NecesidadMother.extraordinaria(UUID.randomUUID(), subcategoria.getId(), 5);
    NecesidadExtraordinaria necesidad2 =
        NecesidadMother.extraordinaria(UUID.randomUUID(), subcategoria.getId(), 5);

    List<Necesidad> necesidades = new ArrayList<>(List.of(necesidad1, necesidad2));

    List<Necesidad> resultado = algoritmo.ordenarNecesidades(necesidades);

    assertEquals(2, resultado.size());
  }

  @Test
  void filtrarDonaciones_conNecesidadNula_debeLanzarExcepcion() {
    List<DonacionIndependiente> donaciones = new ArrayList<>(List.of(donacionEnSubcategoria));

    ValidationException exception =
        assertThrows(
            ValidationException.class, () -> algoritmo.filtrarDonaciones(null, donaciones));
    assertEquals(ErrorCatalog.ALGORITMO_NECESIDAD_NULA, exception.getError());
  }

  @Test
  void filtrarDonaciones_conListaNula_debeLanzarExcepcion() {
    NecesidadExtraordinaria necesidad = NecesidadMother.extraordinaria(subcategoria.getId(), 3);

    ValidationException exception =
        assertThrows(ValidationException.class, () -> algoritmo.filtrarDonaciones(necesidad, null));
    assertEquals(ErrorCatalog.ALGORITMO_DONACIONES_NULAS, exception.getError());
  }

  @Test
  void ordenarNecesidades_conListaNula_debeLanzarExcepcion() {
    ValidationException exception =
        assertThrows(ValidationException.class, () -> algoritmo.ordenarNecesidades(null));
    assertEquals(ErrorCatalog.ALGORITMO_NECESIDADES_NULAS, exception.getError());
  }
}
