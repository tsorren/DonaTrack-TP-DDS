package grupo5.cliente.maqueta;

import grupo5.cliente.puertos.PublicoPuerto;
import grupo5.cliente.vistas.PublicoCifra;
import grupo5.cliente.vistas.PublicoEntregas;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Los datos de ejemplo de las páginas públicas (ficha raices-publico-navegable.md, 6.3), los que
 * antes estaban escritos en las plantillas. Los estados ({@code ?escenario=}) los sigue mostrando
 * cada plantilla: acá no se leen, porque Contacto y Entregas usan nombres que {@link Escenario} no
 * conoce.
 */
@Component
@ConditionalOnProperty(
    name = "cliente.datos.publico",
    havingValue = "maqueta",
    matchIfMissing = true)
class PublicoMaqueta implements PublicoPuerto {

  private static final String ENTREGAS = "1.284";
  private static final String ENTIDADES = "46";

  private static final String COMEDOR = "Comedor Barrio Ejemplo";
  private static final String EN_EL_COMEDOR = COMEDOR + " · Barrio Ejemplo";
  private static final String FOTO_COMEDOR = "/img/ejemplo-entrega.svg";
  private static final String FOTO_ROPA = "/img/ejemplo-entrega-ropa.svg";
  private static final String FOTO_SILLAS = "/img/ejemplo-entrega-sillas.svg";
  private static final String ALT_COMEDOR = "Cajas con la donación junto a la puerta del comedor";
  private static final String ROPA_DE_ABRIGO = "Ropa de abrigo, ";
  private static final String ANIO = "2026";

  /** Las 10 confirmadas del último mes ("hoy" es el 15 de octubre de 2026). */
  private static final PublicoEntregas CONFIRMADAS =
      new PublicoEntregas(
          List.of(
              entrega(
                  "Sillas, 6 unidades",
                  "muebles",
                  "Escuela Ejemplo N.º 12",
                  "Centro Ejemplo",
                  "14",
                  FOTO_SILLAS,
                  "Sillas apiladas en un aula"),
              alimentos("Lentejas secas, 10 kg", "10", true),
              alimentos("Arroz, 5 kg", "09", true),
              entrega(
                  ROPA_DE_ABRIGO + "12 prendas",
                  "ropa",
                  "Merendero Ejemplo Sur",
                  "Ejemplo Sur",
                  "09",
                  FOTO_ROPA,
                  "Bolsas con ropa de abrigo junto a la puerta del merendero"),
              entrega(
                  ROPA_DE_ABRIGO + "5 unidades",
                  "ropa",
                  COMEDOR,
                  "Barrio Ejemplo",
                  "08",
                  FOTO_ROPA,
                  "Bolsas con ropa de abrigo junto a la puerta del comedor"),
              entrega(
                  "Frazadas, 10 unidades",
                  "ropa",
                  "Hogar Casa Ejemplo",
                  "Parque Ejemplo",
                  "07",
                  FOTO_ROPA,
                  "Bolsas con frazadas junto a la puerta del hogar"),
              alimentos("Leche en polvo, 12 unidades", "06", false),
              alimentos("Fideos secos, 8 kg", "05", false),
              alimentos("Arroz, 10 kg", "02", false),
              new PublicoEntregas.Entrega(
                  "Sillas, 4 unidades",
                  "muebles",
                  "Muebles",
                  "Biblioteca Popular Ejemplo",
                  "Biblioteca Popular Ejemplo · Villa Ejemplo",
                  "22 de septiembre",
                  ANIO,
                  "2026-09-22",
                  false,
                  FOTO_SILLAS,
                  "Sillas apiladas en la sala de lectura")),
          "del 16 de septiembre al 15 de octubre de 2026",
          "del 8 al 15 de octubre de 2026",
          "15 de octubre de 2026, 14:30");

  /**
   * Una entrega de octubre; {@code dia}, con dos cifras. Es de la semana si llegó del 8 al 15 (hoy
   * es el 15).
   */
  private static PublicoEntregas.Entrega entrega(
      String que, String cat, String entidad, String barrio, String dia, String foto, String alt) {
    return new PublicoEntregas.Entrega(
        que,
        cat,
        Character.toUpperCase(cat.charAt(0)) + cat.substring(1),
        entidad,
        entidad + " · " + barrio,
        Integer.parseInt(dia) + " de octubre",
        ANIO,
        "2026-10-" + dia,
        Integer.parseInt(dia) >= 8,
        foto,
        alt);
  }

  private static PublicoEntregas.Entrega alimentos(String que, String dia, boolean semana) {
    return new PublicoEntregas.Entrega(
        que,
        "alimentos",
        "Alimentos",
        COMEDOR,
        EN_EL_COMEDOR,
        Integer.parseInt(dia) + " de octubre",
        ANIO,
        "2026-10-" + dia,
        semana,
        FOTO_COMEDOR,
        ALT_COMEDOR);
  }

  @Override
  public PublicoEntregas entregasConfirmadas() {
    return CONFIRMADAS;
  }

  @Override
  public List<PublicoCifra> cifrasDePortada() {
    return List.of(
        new PublicoCifra(ENTREGAS, "Entregas confirmadas"),
        new PublicoCifra(ENTIDADES, "Organizaciones en el mapa"),
        new PublicoCifra("32", "Donaciones en camino ahora"));
  }

  @Override
  public List<PublicoCifra> cifrasDeNosotros() {
    return List.of(
        new PublicoCifra(ENTREGAS, "Entregas confirmadas desde marzo de 2026"),
        new PublicoCifra(ENTIDADES, "Organizaciones en el mapa"),
        new PublicoCifra("318", "Donantes con al menos una donación desde marzo de 2026"));
  }
}
