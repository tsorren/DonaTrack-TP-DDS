package grupo5.donaciones.models.entities;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.donaciones.models.entities.bienes.Bien;
import grupo5.donaciones.models.entities.bienes.Estado;
import grupo5.donaciones.models.entities.categorias.Categoria;
import grupo5.donaciones.models.entities.categorias.SubCategoria;
import grupo5.donaciones.models.entities.categorias.Unidad;
import grupo5.donaciones.models.entities.donaciones.*;
import grupo5.donaciones.models.entities.donantes.Donante;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.personas.Humana;
import java.time.LocalDate;
import java.util.List;

import grupo5.donaciones.infraestructure.SegmentadorSubcategorias;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SegmentadorSubcategoriasTest {

  private SegmentadorSubcategorias segmentador;
  private Donante donante;
  private SubCategoria subcategoriaInvierno;
  private Bien abrigoInvierno;
  private Bien polleraInvierno;
  private Bien manzanas;

  @BeforeEach
  void setUp() {
    segmentador = new SegmentadorSubcategorias();
    Humana humana = new Humana("Juan", "Pérez", LocalDate.of(1990, 1, 1));
    donante = new Donante(humana);

    Categoria categoriaRopa = new Categoria("Ropa", false, true, Unidad.UNIDADES);
    Categoria categoriaAlimentos = new Categoria("Alimentos", false, true, Unidad.KILOGRAMO);

    subcategoriaInvierno = new SubCategoria(categoriaRopa, "Ropa de Invierno");
    SubCategoria subcategoriaFrutas = new SubCategoria(categoriaAlimentos, "Frutas");

    abrigoInvierno =
        new Bien(
            "Abrigo de lana",
            "abrigo.png",
            LocalDate.now().plusMonths(6),
            Estado.NUEVO,
            subcategoriaInvierno);
    polleraInvierno =
        new Bien(
            "Pollera de invierno",
            "pollera.png",
            LocalDate.now().plusMonths(6),
            Estado.NUEVO,
            subcategoriaInvierno);
    manzanas =
        new Bien(
            "Manzanas rojas",
            "manzanas.png",
            LocalDate.now().plusMonths(2),
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

    List<DonacionIndependiente> resultado = segmentador.segmentar(donacion);

    assertNotNull(resultado);
    assertEquals(
        1, resultado.size(), "Debe haber 1 donación independiente");
    assertEquals(
        8,
        resultado.getFirst().getCantidad(),
        "Debe sumar la cantidad de ambos items");
  }

  @Test
  void segmentar_conItemsDeDiferentesSubcategorias_creaVariasDonacionesIndependientes() {
    ItemDonacion item1 = new ItemDonacion(abrigoInvierno, 5);
    ItemDonacion item2 = new ItemDonacion(manzanas, 10);

    Donacion donacion = new Donacion(donante);
    donacion.agregarItem(item1);
    donacion.agregarItem(item2);

    List<DonacionIndependiente> resultado = segmentador.segmentar(donacion);

    assertNotNull(resultado);
    assertEquals(
        2,
        resultado.size(),
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

    List<DonacionIndependiente> resultado = segmentador.segmentar(donacion);

    assertNotNull(resultado);
    assertEquals(
        2,
        resultado.size(),
        "Debe haber 2 donaciones independientes (2 subcategorías)");
  }

  @Test
  void segmentar_donacionOriginalContieneDonacionOriginal() {
    ItemDonacion item1 = new ItemDonacion(abrigoInvierno, 5);

    Donacion donacion = new Donacion(donante);
    donacion.agregarItem(item1);

    List<DonacionIndependiente> resultado = segmentador.segmentar(donacion);

    assertNotNull(resultado.getFirst().getDonacionOriginal());
    assertEquals(donacion, resultado.getFirst().getDonacionOriginal(), "Debe contener la donación original");
  }

  @Test
  void segmentar_creaItemsDonacionIndependientesConSubcategoryCorrecta() {
    ItemDonacion item1 = new ItemDonacion(abrigoInvierno, 5);
    ItemDonacion item2 = new ItemDonacion(polleraInvierno, 3);

    Donacion donacion = new Donacion(donante);
    donacion.agregarItem(item1);
    donacion.agregarItem(item2);

    List<DonacionIndependiente> resultado = segmentador.segmentar(donacion);

    DonacionIndependiente donacionIndependiente =
        resultado.getFirst();
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

    int cantidadOriginal = donacion.getItems().stream().mapToInt(ItemDonacion::getCantidad).sum();

    List<DonacionIndependiente> resultado = segmentador.segmentar(donacion);

    int cantidadSegmentada =
        resultado.stream()
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

    List<DonacionIndependiente> resultado = segmentador.segmentar(donacion);

    resultado
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
  void segmentar_donacionesIndependientesEstanVinculadasAlDonacionOriginal() {
    ItemDonacion item1 = new ItemDonacion(abrigoInvierno, 5);

    Donacion donacion = new Donacion(donante);
    donacion.agregarItem(item1);

    List<DonacionIndependiente> resultado = segmentador.segmentar(donacion);

    resultado
        .forEach(
            donacionInd -> {
              assertNotNull(
                  donacionInd.getDonacionOriginal(),
                  "La donación independiente debe estar vinculada");
              assertEquals(
                  donacion,
                  donacionInd.getDonacionOriginal(),
                  "Debe estar vinculada a la donación original");
            });
  }

  @Test
  void segmentar_conUnSoloItem_segmentaCorrectamente() {
    ItemDonacion item1 = new ItemDonacion(manzanas, 20);

    Donacion donacion = new Donacion(donante);
    donacion.agregarItem(item1);

    List<DonacionIndependiente> resultado = segmentador.segmentar(donacion);

    assertEquals(1, resultado.size());
    assertEquals(20, resultado.getFirst().getCantidad());
  }
}
