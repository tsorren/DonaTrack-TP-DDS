package grupo5.cliente.maqueta;

import grupo5.cliente.puertos.DonanteEntidadesPuerto;
import grupo5.cliente.vistas.DonanteEntidadesVista;
import grupo5.cliente.vistas.DonanteEntidadesVista.Categoria;
import grupo5.cliente.vistas.DonanteEntidadesVista.Entidad;
import grupo5.cliente.vistas.DonanteEntidadesVista.Pedido;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Las entidades de ejemplo de 46, en 8 páginas, con lo que pide cada una y si la empresa ya le
 * donó. Están dibujadas las dos primeras: la primera, la que escribía la plantilla (seis); la
 * segunda, tres para comprobar la pantalla con nombres largos (R11-18: uno de 60 letras sin
 * espacios y uno de 120 con espacios) y con una entidad a la que ya le donaste que no pide nada
 * pendiente. La búsqueda por nombre (sin tildes, como Donaciones), la categoría y "Ya doné" los
 * resuelve este puerto sobre las dibujadas, como si fueran todas (R11-12): con un filtro, el
 * resultado entra en una página.
 */
@Component
@ConditionalOnProperty(
    name = "cliente.datos.donante",
    havingValue = "maqueta",
    matchIfMissing = true)
class DonanteEntidadesMaqueta implements DonanteEntidadesPuerto {

  private static final String ALIMENTOS = "cat-alimentos";
  private static final String ROPA = "cat-ropa";
  private static final String MUEBLES = "cat-muebles";
  private static final String CADA_MES = "Se repite cada mes";
  private static final String NOVIEMBRE_15 = "Hasta el 15 de noviembre";
  private static final String NOVIEMBRE_30 = "Hasta el 30 de noviembre";
  private static final String SILLAS = "Sillas";

  /** La segunda página dibujada: los nombres largos y una entidad sin nada pendiente. */
  private static final List<Entidad> SEGUNDA =
      List.of(
          new Entidad(
              "CooperativaDeTrabajoYAyudaMutuaDelBarrioEjemploNorteYAledaño",
              false,
              List.of(
                  new Pedido(
                      "Frazadas", "ropa", ROPA, "Faltan 8 de 12 unidades", NOVIEMBRE_15, 4, 12))),
          new Entidad(
              "Asociación Civil de Vecinos y Amigos del Barrio Ejemplo para la Ayuda Comunitaria,"
                  + " la Educación Popular y el Deporte Sur",
              true,
              List.of(
                  new Pedido(
                      "Arroz",
                      "alimentos",
                      ALIMENTOS,
                      "Faltan 18 de 30 kg este mes",
                      CADA_MES,
                      12,
                      30))),
          new Entidad("Comedor Ejemplo Este", true, List.of()));

  private static final DonanteEntidadesVista ENTIDADES =
      new DonanteEntidadesVista(
          List.of(
              new Entidad(
                  "Comedor Barrio Ejemplo",
                  true,
                  List.of(
                      new Pedido(
                          "Lentejas secas",
                          "alimentos",
                          ALIMENTOS,
                          "Faltan 30 de 40 kg este mes",
                          CADA_MES,
                          10,
                          40),
                      new Pedido(
                          "Arroz",
                          "alimentos",
                          ALIMENTOS,
                          "Faltan 25 de 40 kg este mes",
                          CADA_MES,
                          15,
                          40),
                      new Pedido(
                          "Aceite de girasol",
                          "alimentos",
                          ALIMENTOS,
                          "Faltan los 20 litros",
                          "Hasta el 20 de octubre",
                          0,
                          20),
                      new Pedido(
                          "Leche en polvo",
                          "alimentos",
                          ALIMENTOS,
                          "Faltan 12 de 24 unidades",
                          "Hasta el 25 de octubre",
                          12,
                          24),
                      new Pedido(
                          "Ropa de abrigo",
                          "ropa",
                          ROPA,
                          "Faltan 25 de 30 unidades",
                          NOVIEMBRE_15,
                          5,
                          30))),
              new Entidad(
                  "Escuela Ejemplo N.º 12",
                  true,
                  List.of(
                      new Pedido(
                          SILLAS,
                          "muebles",
                          MUEBLES,
                          "Faltan 14 de 20 unidades",
                          NOVIEMBRE_30,
                          6,
                          20),
                      new Pedido(
                          "Útiles escolares",
                          "escolares",
                          "cat-escolares",
                          "Faltan las 4 cajas",
                          NOVIEMBRE_30,
                          0,
                          4))),
              new Entidad(
                  "Merendero Ejemplo Sur",
                  true,
                  List.of(
                      new Pedido(
                          "Galletitas",
                          "alimentos",
                          ALIMENTOS,
                          "Faltan 10 de 20 kg este mes",
                          CADA_MES,
                          10,
                          20),
                      new Pedido(
                          "Leche en polvo",
                          "alimentos",
                          ALIMENTOS,
                          "Faltan las 20 unidades",
                          "Hasta el 31 de octubre",
                          0,
                          20))),
              new Entidad(
                  "Hogar Casa Ejemplo",
                  false,
                  List.of(
                      new Pedido(
                          "Frazadas",
                          "ropa",
                          ROPA,
                          "Faltan 10 de 20 unidades",
                          NOVIEMBRE_15,
                          10,
                          20))),
              new Entidad(
                  "Club Ejemplo Norte",
                  false,
                  List.of(
                      new Pedido(
                          "Camisetas deportivas",
                          "ropa",
                          ROPA,
                          "Faltan las 15 unidades",
                          NOVIEMBRE_30,
                          0,
                          15))),
              new Entidad(
                  "Biblioteca Popular Ejemplo",
                  false,
                  List.of(
                      new Pedido(
                          SILLAS,
                          "muebles",
                          MUEBLES,
                          "Faltan 4 de 8 unidades",
                          "Hasta el 31 de diciembre",
                          4,
                          8)))),
          46,
          1,
          8,
          2,
          List.of(
              new Categoria("alimentos", "Alimentos", ALIMENTOS),
              new Categoria("ropa", "Ropa", ROPA),
              new Categoria("muebles", "Muebles", MUEBLES),
              new Categoria("escolares", "Útiles escolares", "cat-escolares")),
          "hoy, 15 de octubre, 16:25");

  @Override
  public DonanteEntidadesVista entidades(UUID donante, Consulta consulta) {
    String categoria =
        ENTIDADES.categorias().stream().anyMatch(c -> c.valor().equals(consulta.categoria()))
            ? consulta.categoria()
            : null;
    if (consulta.nombre().isEmpty() && categoria == null && !consulta.soloDonadas()) {
      int pagina = Math.min(consulta.pagina(), ENTIDADES.paginas());
      return new DonanteEntidadesVista(
          pagina == 2 ? SEGUNDA : ENTIDADES.entidades(),
          ENTIDADES.total(),
          pagina,
          ENTIDADES.paginas(),
          ENTIDADES.dibujadas(),
          ENTIDADES.categorias(),
          ENTIDADES.actualizado());
    }
    String buscado = sinTildes(consulta.nombre());
    List<Entidad> coinciden =
        Stream.concat(ENTIDADES.entidades().stream(), SEGUNDA.stream())
            .filter(e -> sinTildes(e.nombre()).contains(buscado))
            .filter(e -> !consulta.soloDonadas() || e.ya())
            .filter(
                e ->
                    categoria == null
                        || e.pide().stream().anyMatch(p -> categoria.equals(p.categoria())))
            .toList();
    return new DonanteEntidadesVista(
        coinciden, coinciden.size(), 1, 1, 1, ENTIDADES.categorias(), ENTIDADES.actualizado());
  }

  /**
   * Como la búsqueda de Donaciones: sin tildes ni mayúsculas ("direccion" encuentra "Dirección").
   */
  private static String sinTildes(String texto) {
    return Normalizer.normalize(texto, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT);
  }
}
