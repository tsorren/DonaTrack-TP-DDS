package grupo5.donaciones.fixtures;

import grupo5.donaciones.models.entities.categorias.Categoria;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.categorias.Unidad;

public final class CategoriaMother {

  private CategoriaMother() {}

  public static Categoria alimentos() {
    return new Categoria("Alimentos", true, false, Unidad.KILOGRAMO);
  }

  public static Categoria ropa() {
    return new Categoria("Ropa", false, true, Unidad.UNIDADES);
  }

  public static Categoria muebles() {
    return new Categoria("Muebles", false, true, Unidad.UNIDADES);
  }

  public static Categoria tecnologia() {
    return new Categoria("Tecnología", false, true, Unidad.UNIDADES);
  }

  public static Subcategoria arroz(Categoria categoria) {
    Subcategoria sub = new Subcategoria(categoria.getId(), "Arroz");
    sub.agregarAlias("arroz blanco");
    sub.agregarAlias("arroz integral");
    return sub;
  }

  public static Subcategoria camperas(Categoria categoria) {
    Subcategoria sub = new Subcategoria(categoria.getId(), "Camperas");
    sub.agregarAlias("campera de abrigo");
    sub.agregarAlias("abrigo");
    sub.agregarAlias("camperas");
    return sub;
  }

  public static Subcategoria sillas(Categoria categoria) {
    Subcategoria sub = new Subcategoria(categoria.getId(), "Sillas");
    sub.agregarAlias("silla de madera");
    sub.agregarAlias("silla");
    sub.agregarAlias("bancos");
    return sub;
  }

  public static Subcategoria celulares(Categoria categoria) {
    Subcategoria sub = new Subcategoria(categoria.getId(), "Celulares");
    sub.agregarAlias("celu");
    sub.agregarAlias("movil");
    sub.agregarAlias("smartphone");
    return sub;
  }
}
