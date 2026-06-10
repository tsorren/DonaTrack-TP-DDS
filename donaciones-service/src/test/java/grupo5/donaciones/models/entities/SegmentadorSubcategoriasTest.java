package grupo5.donaciones.models.entities;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.donaciones.models.entities.bienes.*;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donaciones.Donante;
import grupo5.donaciones.models.entities.donaciones.ItemDonacion;
import grupo5.donaciones.models.entities.donaciones.segmentaciones.DonacionIndependiente;
import grupo5.donaciones.models.entities.donaciones.segmentaciones.DonacionSegmentada;
import grupo5.donaciones.models.entities.donaciones.segmentaciones.segmentadores.SegmentadorSubcategorias;
import grupo5.donaciones.models.entities.personas.Humana;
import java.time.LocalDate;
import java.time.Month;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SegmentadorSubcategoriasTest {
  private static final LocalDate TEST_DATE = LocalDate.of(2026, Month.JUNE, 9);

  private SegmentadorSubcategorias segmentador;
  private Donante donante;
  private SubCategoria subcategoriaInvierno;
  private Bien abrigoInvierno;
  private Bien polleraInvierno;
  private Bien manzanas;

  @BeforeEach
  void setUp() {
    segmentador = new SegmentadorSubcategorias();
    Humana humana = new Humana("Juan", "Pérez", LocalDate.of(1990, Month.JANUARY, 1));
    donante = new Donante(humana);

    Categoria categoriaRopa = new Categoria("Ropa", false, true, Unidad.UNIDADES);
    Categoria categoriaAlimentos = new Categoria("Alimentos", false, true, Unidad.KILOGRAMO);

    subcategoriaInvierno = new SubCategoria(categoriaRopa, "Ropa de Invierno");
    SubCategoria subcategoriaFrutas = new SubCategoria(categoriaAlimentos, "Frutas");

    abrigoInvierno =
        new Bien(
            "Abrigo de lana",
            "abrigo.png",
            TEST_DATE.plusMonths(6),
            Estado.NUEVO,
            subcategoriaInvierno);
    polleraInvierno =
        new Bien(
            "Pollera de invierno",
            "pollera.png",
            TEST_DATE.plusMonths(6),
            Estado.NUEVO,
            subcategoriaInvierno);
    manzanas =
        new Bien(
            "Manzanas rojas",
            "manzanas.png",
            TEST_DATE.plusMonths(2),
            Estado.NUEVO,
            subcategoriaFrutas);
  }

  @Test
  void segmentar_conItemsDeMismaSubcategoria_creaUnaDonacionIndependiente() {
    ItemDonacion item1 = new ItemDonacion(abrigoInvierno, 5);
    ItemDonacion item2 = new ItemDonacion(polleraInvierno, 3);

    Donacion donacion = new Donacion(donante);
    donacion.agregarItem(item1);
    donacion.agregarItem(item2);

    DonacionSegmentada resultado = segmentador.segmentar(donacion);

    assertNotNull(resultado);
    assertEquals(
        1, resultado.getDonacionesIndependientes().size(), "Debe haber 1 donación independiente");
    assertEquals(
        8,
        resultado.getDonacionesIndependientes().getFirst().getCantidad(),
        "Debe sumar la cantidad de ambos items");
  }

  @Test
  void segmentar_conItemsDeDiferentesSubcategorias_creaVariasDonacionesIndependientes() {
    ItemDonacion item1 = new ItemDonacion(abrigoInvierno, 5);
    ItemDonacion item2 = new ItemDonacion(manzanas, 10);

    Donacion donacion = new Donacion(donante);
    donacion.agregarItem(item1);
    donacion.agregarItem(item2);

    DonacionSegmentada resultado = segmentador.segmentar(donacion);

    assertNotNull(resultado);
    assertEquals(
        2,
        resultado.getDonacionesIndependientes().size(),
        "Debe haber 2 donaciones independientes");
  }

  @Test
  void segmentar_conTresSubcategorias_creaCorrespondientesDonacionesIndependientes() {
    ItemDonacion itemRopa1 = new ItemDonacion(abrigoInvierno, 2);
    ItemDonacion itemRopa2 = new ItemDonacion(polleraInvierno, 3);
    ItemDonacion itemAlimento = new ItemDonacion(manzanas, 15);

    Donacion donacion = new Donacion(donante);
    donacion.agregarItem(itemRopa1);
    donacion.agregarItem(itemRopa2);
    donacion.agregarItem(itemAlimento);

    DonacionSegmentada resultado = segmentador.segmentar(donacion);

    assertNotNull(resultado);
    assertEquals(
        2,
        resultado.getDonacionesIndependientes().size(),
        "Debe haber 2 donaciones independientes (2 subcategorías)");
  }

  @Test
  void segmentar_donacionSegmentadaContieneDonacionOriginal() {
    ItemDonacion item1 = new ItemDonacion(abrigoInvierno, 5);

    Donacion donacion = new Donacion(donante);
    donacion.agregarItem(item1);

    DonacionSegmentada resultado = segmentador.segmentar(donacion);

    assertNotNull(resultado.getDonacion());
    assertEquals(donacion, resultado.getDonacion(), "Debe contener la donación original");
  }

  @Test
  void segmentar_creaItemsDonacionIndependientesConSubcategoryCorrecta() {
    ItemDonacion item1 = new ItemDonacion(abrigoInvierno, 5);
    ItemDonacion item2 = new ItemDonacion(polleraInvierno, 3);

    Donacion donacion = new Donacion(donante);
    donacion.agregarItem(item1);
    donacion.agregarItem(item2);

    DonacionSegmentada resultado = segmentador.segmentar(donacion);

    DonacionIndependiente donacionIndependiente =
        resultado.getDonacionesIndependientes().getFirst();
    assertEquals(
        subcategoriaInvierno,
        donacionIndependiente.getSubCategoria(),
        "Debe tener la categoria correcta");
  }

  @Test
  void segmentar_preservaCantidadesTotal() {
    ItemDonacion item1 = new ItemDonacion(abrigoInvierno, 5);
    ItemDonacion item2 = new ItemDonacion(polleraInvierno, 3);
    ItemDonacion item3 = new ItemDonacion(manzanas, 10);

    Donacion donacion = new Donacion(donante);
    donacion.agregarItem(item1);
    donacion.agregarItem(item2);
    donacion.agregarItem(item3);

    int cantidadOriginal = 5 + 3 + 10;

    DonacionSegmentada resultado = segmentador.segmentar(donacion);

    int cantidadSegmentada =
        resultado.getDonacionesIndependientes().stream()
            .mapToInt(DonacionIndependiente::getCantidad)
            .sum();

    assertEquals(cantidadOriginal, cantidadSegmentada, "Debe preservar la cantidad total");
  }

  @Test
  void segmentar_todosLosItemsEscanQueBienAsociado() {
    ItemDonacion item1 = new ItemDonacion(abrigoInvierno, 5);
    ItemDonacion item2 = new ItemDonacion(polleraInvierno, 3);

    Donacion donacion = new Donacion(donante);
    donacion.agregarItem(item1);
    donacion.agregarItem(item2);

    DonacionSegmentada resultado = segmentador.segmentar(donacion);

    resultado
        .getDonacionesIndependientes()
        .forEach(
            donacionInd ->
                donacionInd
                    .getItems()
                    .forEach(
                        item -> {
                          assertNotNull(item.getBien(), "El item debe tener un bien asociado");
                          assertEquals(
                              donacionInd.getSubCategoria(),
                              item.getBien().getSubcategoria(),
                              "El bien debe pertenecer a la subcategoría de la donación");
                        }));
  }

  @Test
  void segmentar_donacionesIndependientesEstanVinculadasAlDonacionSegmentada() {
    ItemDonacion item1 = new ItemDonacion(abrigoInvierno, 5);

    Donacion donacion = new Donacion(donante);
    donacion.agregarItem(item1);

    DonacionSegmentada resultado = segmentador.segmentar(donacion);

    resultado
        .getDonacionesIndependientes()
        .forEach(
            donacionInd -> {
              assertNotNull(
                  donacionInd.getDonacionSegmentada(),
                  "La donación independiente debe estar vinculada");
              assertEquals(
                  resultado,
                  donacionInd.getDonacionSegmentada(),
                  "Debe estar vinculada a la donación segmentada");
            });
  }

  @Test
  void segmentar_conUnSoloItem_segmentaCorrectamente() {
    ItemDonacion item1 = new ItemDonacion(manzanas, 20);

    Donacion donacion = new Donacion(donante);
    donacion.agregarItem(item1);

    DonacionSegmentada resultado = segmentador.segmentar(donacion);

    assertEquals(1, resultado.getDonacionesIndependientes().size());
    assertEquals(20, resultado.getDonacionesIndependientes().getFirst().getCantidad());
  }
}
