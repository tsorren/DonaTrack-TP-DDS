package grupo5.donaciones.fixtures;

import grupo5.donaciones.models.entities.categorias.Categoria;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.donaciones.Bien;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.ItemDonacionIndependiente;
import grupo5.donaciones.models.entities.itemsNormalizados.BienNormalizado;
import grupo5.donaciones.models.entities.necesidades.Asignable;
import java.util.List;
import java.util.UUID;

public final class DonacionIndependienteMother {

  public static final String ACTOR_DEFECTO = "SISTEMA";

  private DonacionIndependienteMother() {}

  public static DonacionIndependiente crearConCantidad(int cantidad) {
    Categoria categoria = CategoriaMother.ropa();
    Subcategoria subcategoria = CategoriaMother.camperas(categoria);
    Bien bien = BienMother.prendaRopa("Campera de abrigo");
    BienNormalizado bienNormalizado = BienMother.aceptado(bien, subcategoria.getId());
    ItemDonacionIndependiente item = new ItemDonacionIndependiente(bienNormalizado, cantidad);

    return new DonacionIndependiente(UUID.randomUUID(), List.of(item));
  }

  public static DonacionIndependiente crearParaSubcategoria(UUID subcategoriaId, int cantidad) {
    Bien bien = BienMother.simple("Item de prueba");
    BienNormalizado bienNormalizado = BienMother.aceptado(bien, subcategoriaId);
    ItemDonacionIndependiente item = new ItemDonacionIndependiente(bienNormalizado, cantidad);

    return new DonacionIndependiente(UUID.randomUUID(), List.of(item));
  }

  public static DonacionIndependiente enDeposito(int cantidad) {
    return crearConCantidad(cantidad);
  }

  public static DonacionIndependiente asignada(int cantidad, Asignable receptor) {
    DonacionIndependiente d = enDeposito(cantidad);
    d.asignar(ACTOR_DEFECTO, receptor);
    return d;
  }

  public static DonacionIndependiente listaParaEntregar(int cantidad, Asignable receptor) {
    DonacionIndependiente d = asignada(cantidad, receptor);
    d.planificarRuta(ACTOR_DEFECTO);
    return d;
  }

  public static DonacionIndependiente enTraslado(int cantidad, Asignable receptor) {
    DonacionIndependiente d = listaParaEntregar(cantidad, receptor);
    d.iniciarRecorrido(ACTOR_DEFECTO);
    return d;
  }

  public static DonacionIndependiente entregada(int cantidad, Asignable receptor) {
    DonacionIndependiente d = enTraslado(cantidad, receptor);
    d.confirmarEntrega(ACTOR_DEFECTO);
    return d;
  }

  public static DonacionIndependiente entregaFallida(
      int cantidad, Asignable receptor, String motivo) {
    DonacionIndependiente d = enTraslado(cantidad, receptor);
    d.registrarFalla(motivo, ACTOR_DEFECTO);
    return d;
  }

  public static DonacionIndependiente vencida(int cantidad) {
    DonacionIndependiente d = enDeposito(cantidad);
    d.vencer(ACTOR_DEFECTO);
    return d;
  }
}
