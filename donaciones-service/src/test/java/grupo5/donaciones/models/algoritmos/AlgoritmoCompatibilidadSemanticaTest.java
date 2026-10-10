package grupo5.donaciones.models.algoritmos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.fixtures.BienMother;
import grupo5.donaciones.fixtures.CategoriaMother;
import grupo5.donaciones.fixtures.NecesidadMother;
import grupo5.donaciones.models.entities.categorias.Categoria;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.donaciones.Bien;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.ItemDonacionIndependiente;
import grupo5.donaciones.models.entities.itemsNormalizados.BienNormalizado;
import grupo5.donaciones.models.entities.necesidades.NecesidadExtraordinaria;
import grupo5.donaciones.models.normalizacion.ComparadorTexto;
import grupo5.donaciones.models.normalizacion.NormalizadorBasicoTexto;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AlgoritmoCompatibilidadSemanticaTest {

  private Subcategoria subcategoria;
  private Subcategoria subcategoriaOtra;
  private BienNormalizado bienNormalizadoOtro;
  private AlgoritmoCompatibilidadSemantica algoritmo;

  @BeforeEach
  void setUp() {
    Categoria categoria = CategoriaMother.muebles();
    subcategoria = CategoriaMother.sillas(categoria);
    subcategoriaOtra = new Subcategoria(categoria.getId(), "Muebles de Oficina");

    Bien bien = BienMother.mueble("Mesa de oficina");
    bienNormalizadoOtro = BienMother.aceptado(bien, subcategoriaOtra.getId());

    algoritmo =
        new AlgoritmoCompatibilidadSemantica(new ComparadorTexto(new NormalizadorBasicoTexto()));
  }

  private DonacionIndependiente crearDonacion(int cantidad, String descripcion) {
    Bien bien = BienMother.mueble(descripcion);
    BienNormalizado bienConDescripcion = BienMother.aceptado(bien, subcategoria.getId());
    List<ItemDonacionIndependiente> items =
        List.of(new ItemDonacionIndependiente(bienConDescripcion, cantidad));
    return new DonacionIndependiente(UUID.randomUUID(), items);
  }

  @Test
  void filtrarDonaciones_cuandoMismaSubcategoria_debeIncluirla() {
    NecesidadExtraordinaria necesidad = NecesidadMother.extraordinaria(subcategoria.getId(), 3);
    DonacionIndependiente donacion = crearDonacion(5, "silla madera");
    List<DonacionIndependiente> donaciones = new ArrayList<>(List.of(donacion));

    List<DonacionIndependiente> resultado = algoritmo.filtrarDonaciones(necesidad, donaciones);

    assertEquals(1, resultado.size());
  }

  @Test
  void filtrarDonaciones_cuandoDistintaSubcategoria_debeExcluirla() {
    NecesidadExtraordinaria necesidad =
        new NecesidadExtraordinaria(subcategoria.getId(), 3, "silla madera");
    List<ItemDonacionIndependiente> items =
        List.of(new ItemDonacionIndependiente(bienNormalizadoOtro, 5));
    DonacionIndependiente donacionOtraCategoria =
        new DonacionIndependiente(UUID.randomUUID(), items);

    List<DonacionIndependiente> donaciones = new ArrayList<>(List.of(donacionOtraCategoria));

    List<DonacionIndependiente> resultado = algoritmo.filtrarDonaciones(necesidad, donaciones);

    assertTrue(resultado.isEmpty());
  }

  @Test
  void filtrarDonaciones_debeOrdenarPorScoreSemanticoDescendente() {
    NecesidadExtraordinaria necesidad =
        new NecesidadExtraordinaria(subcategoria.getId(), 3, "banco escolar madera");
    DonacionIndependiente donacionConMasScore = crearDonacion(5, "banco madera");
    DonacionIndependiente donacionConMenosScore = crearDonacion(5, "banco plastico");

    List<DonacionIndependiente> donaciones =
        new ArrayList<>(List.of(donacionConMenosScore, donacionConMasScore));

    List<DonacionIndependiente> resultado = algoritmo.filtrarDonaciones(necesidad, donaciones);

    assertEquals(2, resultado.size());
    assertSame(donacionConMasScore, resultado.get(0));
    assertSame(donacionConMenosScore, resultado.get(1));
  }

  @Test
  void filtrarDonaciones_cuandoTodasSonDiferenteSubcategoria_debeRetornarVacia() {
    NecesidadExtraordinaria necesidad =
        new NecesidadExtraordinaria(subcategoria.getId(), 3, "silla madera");
    List<ItemDonacionIndependiente> items =
        List.of(new ItemDonacionIndependiente(bienNormalizadoOtro, 5));
    DonacionIndependiente donacion1 = new DonacionIndependiente(UUID.randomUUID(), items);
    DonacionIndependiente donacion2 = new DonacionIndependiente(UUID.randomUUID(), items);

    List<DonacionIndependiente> donaciones = new ArrayList<>(List.of(donacion1, donacion2));

    List<DonacionIndependiente> resultado = algoritmo.filtrarDonaciones(necesidad, donaciones);

    assertTrue(resultado.isEmpty());
  }

  @Test
  void filtrarDonaciones_conNecesidadNula_debeLanzarExcepcion() {
    List<DonacionIndependiente> donaciones =
        new ArrayList<>(List.of(crearDonacion(5, "banco madera")));

    ValidationException exception =
        assertThrows(
            ValidationException.class, () -> algoritmo.filtrarDonaciones(null, donaciones));
    assertEquals(ErrorCatalog.ALGORITMO_NECESIDAD_NULA, exception.getError());
  }

  @Test
  void filtrarDonaciones_conListaNula_debeLanzarExcepcion() {
    NecesidadExtraordinaria necesidad =
        new NecesidadExtraordinaria(subcategoria.getId(), 3, "silla madera");

    ValidationException exception =
        assertThrows(ValidationException.class, () -> algoritmo.filtrarDonaciones(necesidad, null));
    assertEquals(ErrorCatalog.ALGORITMO_DONACIONES_NULAS, exception.getError());
  }
}
