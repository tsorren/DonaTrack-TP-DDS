package grupo5.donaciones.infrastructure.algoritmos;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.models.entities.beneficiarios.EntidadBeneficiaria;
import grupo5.donaciones.models.entities.categorias.Categoria;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.categorias.Unidad;
import grupo5.donaciones.models.entities.donaciones.Bien;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donaciones.Estado;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.ItemDonacionIndependiente;
import grupo5.donaciones.models.entities.donantes.Donante;
import grupo5.donaciones.models.entities.itemsNormalizados.BienNormalizado;
import grupo5.donaciones.models.entities.itemsNormalizados.EstadoNormalizacion;
import grupo5.donaciones.models.entities.necesidades.NecesidadExtraordinaria;
import grupo5.donaciones.models.entities.personas.Humana;
import grupo5.donaciones.models.entities.personas.Juridica;
import grupo5.donaciones.models.entities.personas.TipoJuridico;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AlgoritmoPrioridadSubAtendidosTest {
  private static final LocalDate TEST_DATE = LocalDate.of(2026, Month.JUNE, 9);

  private Subcategoria subcategoria;
  private Subcategoria subcategoriaOtra;
  private DonacionIndependiente donacionEnSubcategoria;
  private DonacionIndependiente donacionEnOtraSubcategoria;
  private AlgoritmoPrioridadSubAtendidos algoritmo;

  @BeforeEach
  void setUp() {
    Humana humana = new Humana("nombre", "apellido", TEST_DATE);
    Donante donante = new Donante(humana.getId());
    Donacion donacionOriginal = new Donacion(donante.getId());
    Categoria categoria = new Categoria("Ropa", false, true, Unidad.UNIDADES);
    subcategoria = new Subcategoria(categoria.getId(), "Ropa de Invierno");
    subcategoriaOtra = new Subcategoria(categoria.getId(), "Ropa de Verano");
    Bien bien =
        new Bien("descripcion", "imagen.png", TEST_DATE.plusMonths(2), Estado.NUEVO, 1.0, 1.0);
    BienNormalizado bienNormalizado =
        new BienNormalizado(
            bien, subcategoria.getId(), 1.0, EstadoNormalizacion.ACEPTADO, true, false);
    BienNormalizado bienNormalizadoOtro =
        new BienNormalizado(
            bien, subcategoriaOtra.getId(), 1.0, EstadoNormalizacion.ACEPTADO, true, false);

    List<ItemDonacionIndependiente> items = new ArrayList<>();
    items.add(new ItemDonacionIndependiente(bienNormalizado, 5));
    donacionEnSubcategoria = new DonacionIndependiente(donacionOriginal.getId(), items);

    List<ItemDonacionIndependiente> itemsOtros = new ArrayList<>();
    itemsOtros.add(new ItemDonacionIndependiente(bienNormalizadoOtro, 5));
    donacionEnOtraSubcategoria = new DonacionIndependiente(donacionOriginal.getId(), itemsOtros);

    algoritmo = new AlgoritmoPrioridadSubAtendidos();
  }

  @Test
  void filtrarDonaciones_cuandoMismaSubcategoria_debeIncluirla() {
    NecesidadExtraordinaria necesidad =
        new NecesidadExtraordinaria(subcategoria.getId(), 3, "necesito ropa de invierno");
    List<DonacionIndependiente> donaciones = new ArrayList<>();
    donaciones.add(donacionEnSubcategoria);

    List<DonacionIndependiente> resultado = algoritmo.filtrarDonaciones(necesidad, donaciones);

    assertEquals(1, resultado.size());
    assertTrue(resultado.contains(donacionEnSubcategoria));
  }

  @Test
  void filtrarDonaciones_cuandoDistintaSubcategoria_debeExcluirla() {
    NecesidadExtraordinaria necesidad =
        new NecesidadExtraordinaria(subcategoria.getId(), 3, "necesito ropa de invierno");
    List<DonacionIndependiente> donaciones = new ArrayList<>();
    donaciones.add(donacionEnOtraSubcategoria);

    List<DonacionIndependiente> resultado = algoritmo.filtrarDonaciones(necesidad, donaciones);

    assertTrue(resultado.isEmpty());
  }

  @Test
  void filtrarDonaciones_conListaMixta_soloDebeRetornarLasDeMismaSubcategoria() {
    NecesidadExtraordinaria necesidad =
        new NecesidadExtraordinaria(subcategoria.getId(), 3, "necesito ropa de invierno");
    List<DonacionIndependiente> donaciones = new ArrayList<>();
    donaciones.add(donacionEnSubcategoria);
    donaciones.add(donacionEnOtraSubcategoria);

    List<DonacionIndependiente> resultado = algoritmo.filtrarDonaciones(necesidad, donaciones);

    assertEquals(1, resultado.size());
    assertTrue(resultado.contains(donacionEnSubcategoria));
  }

  @Test
  void ordenarNecesidades_cuandoUnaEntidadTieneMayorPorcentajeSatisfecho_debeQuedarAlFinal() {
    Humana representante = new Humana("rep", "apellido", TEST_DATE);
    Juridica juridicaMuyAtendida =
        new Juridica(representante, "Empresa Muy Atendida", TipoJuridico.EMPRESA, "Rubro");
    Juridica juridicaPocoAtendida =
        new Juridica(representante, "Empresa Poco Atendida", TipoJuridico.EMPRESA, "Rubro");
    EntidadBeneficiaria entidadMuyAtendida = new EntidadBeneficiaria(juridicaMuyAtendida.getId());
    EntidadBeneficiaria entidadPocoAtendida = new EntidadBeneficiaria(juridicaPocoAtendida.getId());

    // Entidad muy atendida: tiene donaciones recientes asignadas a su necesidad
    NecesidadExtraordinaria necesidadActualMuyAtendida =
        new NecesidadExtraordinaria(
            subcategoria.getId(), 10, "nueva necesidad de entidad muy atendida");
    necesidadActualMuyAtendida.asociarAEntidad(entidadMuyAtendida.getId());
    necesidadActualMuyAtendida.asignarDonacion(donacionEnSubcategoria);

    // Entidad poco atendida: sin donaciones asignadas
    NecesidadExtraordinaria necesidadActualPocoAtendida =
        new NecesidadExtraordinaria(
            subcategoria.getId(), 5, "nueva necesidad de entidad poco atendida");
    necesidadActualPocoAtendida.asociarAEntidad(entidadPocoAtendida.getId());

    List<grupo5.donaciones.models.entities.necesidades.Necesidad> necesidades = new ArrayList<>();
    necesidades.add(necesidadActualMuyAtendida);
    necesidades.add(necesidadActualPocoAtendida);

    List<grupo5.donaciones.models.entities.necesidades.Necesidad> resultado =
        algoritmo.ordenarNecesidades(necesidades);

    assertEquals(necesidadActualPocoAtendida, resultado.get(0));
    assertEquals(necesidadActualMuyAtendida, resultado.get(1));
  }

  @Test
  void ordenarNecesidades_cuandoNingunaTieneEntidad_debeRetornarLasMismas() {
    NecesidadExtraordinaria necesidad1 =
        new NecesidadExtraordinaria(subcategoria.getId(), 5, "primera necesidad sin entidad");
    NecesidadExtraordinaria necesidad2 =
        new NecesidadExtraordinaria(subcategoria.getId(), 5, "segunda necesidad sin entidad");

    List<grupo5.donaciones.models.entities.necesidades.Necesidad> necesidades = new ArrayList<>();
    necesidades.add(necesidad1);
    necesidades.add(necesidad2);

    List<grupo5.donaciones.models.entities.necesidades.Necesidad> resultado =
        algoritmo.ordenarNecesidades(necesidades);

    assertEquals(2, resultado.size());
  }

  @Test
  void filtrarDonaciones_conNecesidadNula_debeLanzarExcepcion() {
    List<DonacionIndependiente> donaciones = new ArrayList<>();
    donaciones.add(donacionEnSubcategoria);

    ValidationException exception =
        assertThrows(
            ValidationException.class, () -> algoritmo.filtrarDonaciones(null, donaciones));
    assertEquals(ErrorCatalog.ALGORITMO_NECESIDAD_NULA, exception.getError());
  }

  @Test
  void filtrarDonaciones_conListaNula_debeLanzarExcepcion() {
    NecesidadExtraordinaria necesidad =
        new NecesidadExtraordinaria(subcategoria.getId(), 3, "necesito ropa de invierno");

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
