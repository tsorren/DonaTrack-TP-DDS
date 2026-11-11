package grupo5.cliente.maqueta;

import grupo5.cliente.puertos.PublicoMapaPuerto;
import grupo5.cliente.vistas.PublicoMapaVista;
import grupo5.cliente.vistas.PublicoMapaVista.Entrega;
import grupo5.cliente.vistas.PublicoMapaVista.Estacion;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Las diez entregas confirmadas de ejemplo (ficha raices-publico-navegable.md, 6.3), en los cinco
 * puntos del mapa que escribía {@code datos-publicos}: un punto sintético por barrio de la ciudad,
 * nunca una dirección. "Hoy" es el 15 de octubre de 2026. El filtro y los estados ({@code
 * ?escenario=}) los muestra la plantilla.
 */
@Component
@ConditionalOnProperty(
    name = "cliente.datos.publico",
    havingValue = "maqueta",
    matchIfMissing = true)
class PublicoMapaMaqueta implements PublicoMapaPuerto {

  private static final String COMEDOR = "Comedor Barrio Ejemplo";
  private static final String ESCUELA = "Escuela Ejemplo N.º 12";
  private static final String MERENDERO = "Merendero Ejemplo Sur";
  private static final String HOGAR = "Hogar Casa Ejemplo";
  private static final String BIBLIOTECA = "Biblioteca Popular Ejemplo";
  private static final String ALIMENTOS = "alimentos";
  private static final String ROPA = "ropa";
  private static final String MUEBLES = "muebles";
  private static final String FOTO_COMEDOR = "/img/ejemplo-entrega.svg";
  private static final String FOTO_ROPA = "/img/ejemplo-entrega-ropa.svg";
  private static final String FOTO_SILLAS = "/img/ejemplo-entrega-sillas.svg";
  private static final String ALT_COMEDOR = "Cajas con la donación junto a la puerta del comedor";
  private static final String ALT_ROPA_COMEDOR =
      "Bolsas con ropa de abrigo junto a la puerta del comedor";
  private static final String ROPA_DE_ABRIGO = "Ropa de abrigo, ";
  private static final String LECHE_EN_POLVO = "Leche en polvo";
  private static final String SILLAS = "Sillas";
  private static final String ANIO = "2026";

  private static final PublicoMapaVista MAPA =
      new PublicoMapaVista(
          List.of(
              new Estacion(
                  "comedor",
                  1,
                  -34.6418,
                  -58.4291,
                  COMEDOR,
                  "Barrio Ejemplo",
                  List.of(
                      "Lentejas secas",
                      "Arroz",
                      "Aceite de girasol",
                      LECHE_EN_POLVO,
                      "Ropa de abrigo"),
                  List.of(
                      alimentos("Lentejas secas, 10 kg", "10"),
                      alimentos("Arroz, 5 kg", "09"),
                      entrega(
                          ROPA_DE_ABRIGO + "5 unidades",
                          ROPA,
                          "08",
                          COMEDOR,
                          FOTO_ROPA,
                          ALT_ROPA_COMEDOR),
                      alimentos(LECHE_EN_POLVO + ", 12 unidades", "06"),
                      alimentos("Fideos secos, 8 kg", "05"),
                      alimentos("Arroz, 10 kg", "02"))),
              new Estacion(
                  "escuela",
                  2,
                  -34.6052,
                  -58.3846,
                  ESCUELA,
                  "Centro Ejemplo",
                  List.of(SILLAS, "Útiles escolares"),
                  List.of(
                      entrega(
                          "Sillas, 6 unidades",
                          MUEBLES,
                          "14",
                          ESCUELA,
                          FOTO_SILLAS,
                          "Sillas apiladas en un aula"))),
              new Estacion(
                  "merendero",
                  3,
                  -34.6683,
                  -58.4067,
                  MERENDERO,
                  "Ejemplo Sur",
                  List.of("Galletitas", LECHE_EN_POLVO),
                  List.of(
                      entrega(
                          ROPA_DE_ABRIGO + "12 prendas",
                          ROPA,
                          "09",
                          MERENDERO,
                          FOTO_ROPA,
                          "Bolsas con ropa de abrigo junto a la puerta del merendero"))),
              new Estacion(
                  "hogar",
                  4,
                  -34.6287,
                  -58.4622,
                  HOGAR,
                  "Parque Ejemplo",
                  List.of("Frazadas"),
                  List.of(
                      entrega(
                          "Frazadas, 10 unidades",
                          ROPA,
                          "07",
                          HOGAR,
                          FOTO_ROPA,
                          "Bolsas con frazadas junto a la puerta del hogar"))),
              new Estacion(
                  "biblioteca",
                  5,
                  -34.6561,
                  -58.4935,
                  BIBLIOTECA,
                  "Villa Ejemplo",
                  List.of(SILLAS),
                  List.of(
                      new Entrega(
                          "Sillas, 4 unidades",
                          MUEBLES,
                          "Muebles",
                          "22 de septiembre",
                          ANIO,
                          "2026-09-22",
                          BIBLIOTECA,
                          FOTO_SILLAS,
                          "Sillas apiladas en la sala de lectura")))),
          "del 16 de septiembre al 15 de octubre de 2026",
          "15 de octubre de 2026, 14:30",
          true,
          true,
          null);

  /** Una entrega de octubre; {@code dia}, con dos cifras. */
  private static Entrega entrega(
      String que, String cat, String dia, String entidad, String foto, String alt) {
    return new Entrega(
        que,
        cat,
        Character.toUpperCase(cat.charAt(0)) + cat.substring(1),
        Integer.parseInt(dia) + " de octubre",
        ANIO,
        "2026-10-" + dia,
        entidad,
        foto,
        alt);
  }

  private static Entrega alimentos(String que, String dia) {
    return entrega(que, ALIMENTOS, dia, COMEDOR, FOTO_COMEDOR, ALT_COMEDOR);
  }

  @Override
  public PublicoMapaVista mapa() {
    return MAPA;
  }
}
