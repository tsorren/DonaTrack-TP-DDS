package grupo5.cliente.maqueta;

import grupo5.cliente.puertos.DonanteEntidadesPuerto;
import grupo5.cliente.vistas.DonanteEntidadesVista;
import grupo5.cliente.vistas.DonanteEntidadesVista.Entidad;
import grupo5.cliente.vistas.DonanteEntidadesVista.Pedido;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * La primera página de entidades de ejemplo, la que escribía la plantilla: seis de 46, en 8
 * páginas, con lo que pide cada una y si la empresa ya le donó. La búsqueda por nombre la hace este
 * puerto (sin tildes, como Donaciones) sobre esa página; los filtros de categoría y "Ya doné" los
 * aplica la pantalla.
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

  private static final DonanteEntidadesVista ENTIDADES =
      new DonanteEntidadesVista(
          List.of(
              new Entidad(
                  "Comedor Barrio Ejemplo",
                  List.of("fideos", "lentejas"),
                  List.of(
                      new Pedido(
                          "Lentejas secas",
                          ALIMENTOS,
                          "Faltan 30 de 40 kg este mes",
                          CADA_MES,
                          10,
                          40),
                      new Pedido(
                          "Arroz", ALIMENTOS, "Faltan 25 de 40 kg este mes", CADA_MES, 15, 40),
                      new Pedido(
                          "Aceite de girasol",
                          ALIMENTOS,
                          "Faltan los 20 litros",
                          "Hasta el 20 de octubre",
                          0,
                          20),
                      new Pedido(
                          "Leche en polvo",
                          ALIMENTOS,
                          "Faltan 12 de 24 unidades",
                          "Hasta el 25 de octubre",
                          12,
                          24),
                      new Pedido(
                          "Ropa de abrigo",
                          ROPA,
                          "Faltan 25 de 30 unidades",
                          NOVIEMBRE_15,
                          5,
                          30))),
              new Entidad(
                  "Escuela Ejemplo N.º 12",
                  List.of("utiles"),
                  List.of(
                      new Pedido(SILLAS, MUEBLES, "Faltan 14 de 20 unidades", NOVIEMBRE_30, 6, 20),
                      new Pedido(
                          "Útiles escolares",
                          "cat-escolares",
                          "Faltan las 4 cajas",
                          NOVIEMBRE_30,
                          0,
                          4))),
              new Entidad(
                  "Merendero Ejemplo Sur",
                  List.of("ropa", "leche"),
                  List.of(
                      new Pedido(
                          "Galletitas", ALIMENTOS, "Faltan 10 de 20 kg este mes", CADA_MES, 10, 20),
                      new Pedido(
                          "Leche en polvo",
                          ALIMENTOS,
                          "Faltan las 20 unidades",
                          "Hasta el 31 de octubre",
                          0,
                          20))),
              new Entidad(
                  "Hogar Casa Ejemplo",
                  List.of(),
                  List.of(
                      new Pedido(
                          "Frazadas", ROPA, "Faltan 10 de 20 unidades", NOVIEMBRE_15, 10, 20))),
              new Entidad(
                  "Club Ejemplo Norte",
                  List.of(),
                  List.of(
                      new Pedido(
                          "Camisetas deportivas",
                          ROPA,
                          "Faltan las 15 unidades",
                          NOVIEMBRE_30,
                          0,
                          15))),
              new Entidad(
                  "Biblioteca Popular Ejemplo",
                  List.of(),
                  List.of(
                      new Pedido(
                          SILLAS,
                          MUEBLES,
                          "Faltan 4 de 8 unidades",
                          "Hasta el 31 de diciembre",
                          4,
                          8)))),
          46,
          8,
          1,
          "hoy, 15 de octubre, 16:25");

  @Override
  public DonanteEntidadesVista entidades(UUID donante, String nombre, int pagina) {
    if (nombre == null || nombre.isBlank()) {
      return ENTIDADES;
    }
    String buscado = sinTildes(nombre.strip());
    List<Entidad> coinciden =
        ENTIDADES.entidades().stream()
            .filter(e -> sinTildes(e.nombre()).contains(buscado))
            .toList();
    return new DonanteEntidadesVista(coinciden, coinciden.size(), 1, 1, ENTIDADES.actualizado());
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
