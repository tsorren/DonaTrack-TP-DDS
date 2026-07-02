package grupo5.donaciones.infrastructure.algoritmos;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.infrastructure.analizadores.ComparadorTexto;
import grupo5.donaciones.infrastructure.analizadores.NormalizadorBasicoTexto;
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
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AlgoritmoCompatibilidadSemanticaTest {
  private static final LocalDate TEST_DATE = LocalDate.of(2026, Month.JUNE, 9);

  private Subcategoria subcategoria;
  private Subcategoria subcategoriaOtra;
  private Donacion donacionOriginal;

  private BienNormalizado bienNormalizadoOtro;
  private AlgoritmoCompatibilidadSemantica algoritmo;

  @BeforeEach
  void setUp() {
    Humana humana = new Humana("nombre", "apellido", TEST_DATE);
    Donante donante = new Donante(humana.getId());
    donacionOriginal = new Donacion(donante.getId());
    Categoria categoria = new Categoria("Mueble", false, true, Unidad.UNIDADES);
    subcategoria = new Subcategoria(categoria.getId(), "Muebles Escolares");
    subcategoriaOtra = new Subcategoria(categoria.getId(), "Muebles de Oficina");
    Bien bien = new Bien("descripcion", "imagen.png", TEST_DATE.plusMonths(2), Estado.NUEVO);
    bienNormalizadoOtro =
        new BienNormalizado(
            bien, subcategoriaOtra.getId(), 1.0, EstadoNormalizacion.ACEPTADO, true, false);

    algoritmo =
        new AlgoritmoCompatibilidadSemantica(new ComparadorTexto(new NormalizadorBasicoTexto()));
  }

  private DonacionIndependiente crearDonacion(int cantidad, String descripcion) {
    Bien bien = new Bien(descripcion, "imagen.png", TEST_DATE.plusMonths(2), Estado.NUEVO);
    BienNormalizado bienConDescripcion =
        new BienNormalizado(
            bien, subcategoria.getId(), 1.0, EstadoNormalizacion.ACEPTADO, true, false);
    List<ItemDonacionIndependiente> items = new ArrayList<>();
    items.add(new ItemDonacionIndependiente(bienConDescripcion, cantidad));
    return new DonacionIndependiente(donacionOriginal.getId(), items);
  }

  @Test
  void filtrarDonaciones_cuandoMismaSubcategoria_debeIncluirla() {
    NecesidadExtraordinaria necesidad =
        new NecesidadExtraordinaria(subcategoria.getId(), 3, "banco silla madera");
    DonacionIndependiente donacion = crearDonacion(5, "banco madera");
    List<DonacionIndependiente> donaciones = new ArrayList<>();
    donaciones.add(donacion);

    List<DonacionIndependiente> resultado = algoritmo.filtrarDonaciones(necesidad, donaciones);

    assertEquals(1, resultado.size());
  }

  @Test
  void filtrarDonaciones_cuandoDistintaSubcategoria_debeExcluirla() {
    NecesidadExtraordinaria necesidad =
        new NecesidadExtraordinaria(subcategoria.getId(), 3, "banco silla madera");
    List<ItemDonacionIndependiente> items = new ArrayList<>();
    items.add(new ItemDonacionIndependiente(bienNormalizadoOtro, 5));
    DonacionIndependiente donacionOtraCategoria =
        new DonacionIndependiente(donacionOriginal.getId(), items);

    List<DonacionIndependiente> donaciones = new ArrayList<>();
    donaciones.add(donacionOtraCategoria);

    List<DonacionIndependiente> resultado = algoritmo.filtrarDonaciones(necesidad, donaciones);

    assertTrue(resultado.isEmpty());
  }

  @Test
  void filtrarDonaciones_debeOrdenarPorScoreSemanticoDescendente() {
    NecesidadExtraordinaria necesidad =
        new NecesidadExtraordinaria(subcategoria.getId(), 3, "banco escolar madera");
    DonacionIndependiente donacionConMasScore = crearDonacion(5, "banco madera");
    DonacionIndependiente donacionConMenosScore = crearDonacion(5, "banco plastico");

    List<DonacionIndependiente> donaciones = new ArrayList<>();
    donaciones.add(donacionConMenosScore);
    donaciones.add(donacionConMasScore);

    List<DonacionIndependiente> resultado = algoritmo.filtrarDonaciones(necesidad, donaciones);

    assertEquals(2, resultado.size());
    assertSame(donacionConMasScore, resultado.get(0));
    assertSame(donacionConMenosScore, resultado.get(1));
  }

  @Test
  void filtrarDonaciones_cuandoTodasSonDiferenteSubcategoria_debeRetornarVacia() {
    NecesidadExtraordinaria necesidad =
        new NecesidadExtraordinaria(subcategoria.getId(), 3, "banco silla madera");
    List<ItemDonacionIndependiente> items = new ArrayList<>();
    items.add(new ItemDonacionIndependiente(bienNormalizadoOtro, 5));
    DonacionIndependiente donacion1 = new DonacionIndependiente(donacionOriginal.getId(), items);
    DonacionIndependiente donacion2 = new DonacionIndependiente(donacionOriginal.getId(), items);

    List<DonacionIndependiente> donaciones = new ArrayList<>();
    donaciones.add(donacion1);
    donaciones.add(donacion2);

    List<DonacionIndependiente> resultado = algoritmo.filtrarDonaciones(necesidad, donaciones);

    assertTrue(resultado.isEmpty());
  }

  @Test
  void filtrarDonaciones_conNecesidadNula_debeLanzarExcepcion() {
    List<DonacionIndependiente> donaciones = new ArrayList<>();
    donaciones.add(crearDonacion(5, "banco madera"));

    ValidationException exception =
        assertThrows(
            ValidationException.class, () -> algoritmo.filtrarDonaciones(null, donaciones));
    assertEquals(ErrorCatalog.ALGORITMO_NECESIDAD_NULA, exception.getError());
  }

  @Test
  void filtrarDonaciones_conListaNula_debeLanzarExcepcion() {
    NecesidadExtraordinaria necesidad =
        new NecesidadExtraordinaria(subcategoria.getId(), 3, "banco silla madera");

    ValidationException exception =
        assertThrows(ValidationException.class, () -> algoritmo.filtrarDonaciones(necesidad, null));
    assertEquals(ErrorCatalog.ALGORITMO_DONACIONES_NULAS, exception.getError());
  }
}
