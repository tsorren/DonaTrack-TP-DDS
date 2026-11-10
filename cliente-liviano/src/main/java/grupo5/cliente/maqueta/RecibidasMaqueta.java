package grupo5.cliente.maqueta;

import grupo5.cliente.puertos.RecibidasPuerto;
import grupo5.cliente.vistas.RecibidasVista;
import grupo5.cliente.vistas.RecibidasVista.Recibida;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Las 11 donaciones asignadas al Comedor Barrio Ejemplo, las mismas de {@code piezas-entidad.html}
 * (que siguen usando Inicio y Seguir entregas). "Hoy" es el 15 de octubre de 2026. Quien ocultó su
 * nombre figura con su categoría del ranking (Donante Sostenedor).
 */
@Component
@ConditionalOnProperty(
    name = "cliente.datos.recibidas",
    havingValue = "maqueta",
    matchIfMissing = true)
class RecibidasMaqueta implements RecibidasPuerto {

  private static final String ALIMENTOS = "cat-alimentos";
  private static final String TEXTO_ALIMENTOS = "Alimentos";
  private static final String LENTEJAS = "lentejas";
  private static final String LENTEJAS_SECAS = "Lentejas secas";
  private static final String ARROZ = "arroz";
  private static final String ROPA = "ropa";
  private static final String ROPA_DE_ABRIGO = "Ropa de abrigo";
  private static final String LECHE = "leche";
  private static final String LECHE_EN_POLVO = "Leche en polvo";
  private static final String DISTRIBUIDORA = "Distribuidora Ejemplo SA";
  private static final String ALMACEN = "Almacén Ejemplo SRL";
  private static final String LACTEOS = "Lácteos Ejemplo SA";
  private static final String SOSTENEDOR = "Donante Sostenedor";
  private static final String RECIBIDA = "recibida";

  private static final RecibidasVista RECIBIDAS =
      new RecibidasVista(
          List.of(
              espera(
                  LENTEJAS,
                  LENTEJAS_SECAS,
                  "15 kg",
                  DISTRIBUIDORA,
                  "puerta",
                  "Llegó a tu puerta hoy a las 18:20. Espera tu confirmación."),
              new Recibida(
                  ROPA,
                  ROPA_DE_ABRIGO,
                  "cat-ropa",
                  "Ropa",
                  "10 unidades",
                  SOSTENEDOR,
                  ROPA,
                  "ropa de abrigo",
                  "puerta",
                  "Llegó a tu puerta ayer a las 17:10. Espera tu confirmación.",
                  null,
                  null),
              espera(
                  "aceite",
                  "Aceite de girasol",
                  "10 litros",
                  ALMACEN,
                  "problema",
                  "El camión no pudo llegar el 14 de octubre. Te avisamos la nueva fecha."),
              espera(
                  ARROZ,
                  "Arroz",
                  "20 kg",
                  ALMACEN,
                  "camino",
                  "Salió del depósito hoy a las 9:15. Llega mañana."),
              espera(
                  LECHE,
                  LECHE_EN_POLVO,
                  "12 unidades",
                  LACTEOS,
                  "salir",
                  "Asignada el 13 de octubre. Sale del depósito el 17 de octubre."),
              recibida("r10", LENTEJAS_SECAS, ALIMENTOS, "10 kg", ALMACEN, LENTEJAS, 10),
              recibida("r9", "Arroz", ALIMENTOS, "5 kg", "Martina Ruiz", ARROZ, 9),
              recibida("r8", ROPA_DE_ABRIGO, "cat-ropa", "5 unidades", SOSTENEDOR, ROPA, 8),
              recibida("r6", LECHE_EN_POLVO, ALIMENTOS, "12 unidades", LACTEOS, LECHE, 6),
              recibida("r5", "Fideos secos", ALIMENTOS, "8 kg", DISTRIBUIDORA, null, 5),
              recibida("r2", "Arroz", ALIMENTOS, "10 kg", ALMACEN, ARROZ, 2)),
          "octubre",
          "15 de octubre de 2026, 18:30",
          3);

  @Override
  public RecibidasVista recibidas(UUID entidad) {
    return RECIBIDAS;
  }

  /** Una de alimentos que todavía no se recibió; su pedido se llama como ella. */
  private static Recibida espera(
      String clave, String nombre, String cuanto, String dono, String grupo, String paso) {
    return new Recibida(
        clave,
        nombre,
        ALIMENTOS,
        TEXTO_ALIMENTOS,
        cuanto,
        dono,
        clave,
        nombre.toLowerCase(Locale.ROOT),
        grupo,
        paso,
        null,
        null);
  }

  /** Una recibida en octubre: el pedido se nombra como el bien, en minúsculas. */
  private static Recibida recibida(
      String clave,
      String nombre,
      String icono,
      String cuanto,
      String dono,
      String pedido,
      int dia) {
    return new Recibida(
        clave,
        nombre,
        icono,
        ALIMENTOS.equals(icono) ? TEXTO_ALIMENTOS : "Ropa",
        cuanto,
        dono,
        pedido,
        pedido == null ? null : nombre.toLowerCase(Locale.ROOT),
        RECIBIDA,
        "Confirmaste la recepción el " + dia + " de octubre.",
        String.valueOf(dia),
        "2026-10-%02d".formatted(dia));
  }
}
