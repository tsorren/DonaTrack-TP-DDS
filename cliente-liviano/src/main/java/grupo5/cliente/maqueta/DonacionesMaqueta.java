package grupo5.cliente.maqueta;

import static grupo5.cliente.vistas.EstadoEntrega.CON_PROBLEMA;
import static grupo5.cliente.vistas.EstadoEntrega.ENTREGADA;
import static grupo5.cliente.vistas.EstadoEntrega.EN_CAMINO;
import static grupo5.cliente.vistas.EstadoEntrega.PENDIENTE;

import grupo5.cliente.puertos.DonacionesPuerto;
import grupo5.cliente.vistas.MisDonacionesVista;
import grupo5.cliente.vistas.MisDonacionesVista.Categoria;
import grupo5.cliente.vistas.MisDonacionesVista.Donacion;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Las siete donaciones de ejemplo de la empresa, las mismas de {@code viaje-datos.html} (que siguen
 * usando el detalle, el inicio y Seguimiento), ya con los textos que muestra la pantalla. "Hoy" es
 * el 15 de octubre de 2026.
 */
@Component
@ConditionalOnProperty(
    name = "cliente.datos.donaciones",
    havingValue = "maqueta",
    matchIfMissing = true)
class DonacionesMaqueta implements DonacionesPuerto {

  private static final String ALIMENTOS = "alimentos";
  private static final String ICONO_ALIMENTOS = "cat-alimentos";
  private static final String TEXTO_ALIMENTOS = "Alimentos";
  private static final String ROPA = "ropa";
  private static final String ICONO_ROPA = "cat-ropa";
  private static final String TEXTO_ROPA = "Ropa";
  private static final String MERENDERO = "Merendero Ejemplo Sur";
  private static final String COMEDOR = "Comedor Barrio Ejemplo";

  private static final MisDonacionesVista DONACIONES =
      new MisDonacionesVista(
          List.of(
              new Donacion(
                  "arroz",
                  7,
                  "Arroz largo fino",
                  "10 kg",
                  ALIMENTOS,
                  ICONO_ALIMENTOS,
                  TEXTO_ALIMENTOS,
                  null,
                  "14 de octubre de 2026",
                  PENDIENTE,
                  "En el depósito, espera destino"),
              new Donacion(
                  "leche",
                  6,
                  "Leche en polvo",
                  "6 kg",
                  ALIMENTOS,
                  ICONO_ALIMENTOS,
                  TEXTO_ALIMENTOS,
                  MERENDERO,
                  "13 de octubre de 2026",
                  PENDIENTE,
                  "Asignada, espera el camión"),
              new Donacion(
                  "lentejas",
                  5,
                  "Lentejas secas",
                  "15 kg",
                  ALIMENTOS,
                  ICONO_ALIMENTOS,
                  TEXTO_ALIMENTOS,
                  COMEDOR,
                  "12 de octubre de 2026",
                  EN_CAMINO,
                  "Salió hoy a las 14:30"),
              new Donacion(
                  "utiles",
                  4,
                  "Útiles escolares",
                  "2 cajas",
                  "escolares",
                  "cat-escolares",
                  "Útiles escolares",
                  "Escuela Ejemplo N.º 12",
                  "8 de octubre de 2026",
                  CON_PROBLEMA,
                  "No pudo llegar; sale de nuevo el 17 de octubre"),
              new Donacion(
                  "zapatillas",
                  3,
                  "Zapatillas",
                  "4 pares",
                  ROPA,
                  ICONO_ROPA,
                  TEXTO_ROPA,
                  null,
                  "6 de octubre de 2026",
                  PENDIENTE,
                  "Espera una necesidad que le corresponda"),
              new Donacion(
                  ROPA,
                  2,
                  "Ropa de abrigo",
                  "12 prendas",
                  ROPA,
                  ICONO_ROPA,
                  TEXTO_ROPA,
                  MERENDERO,
                  "2 de octubre de 2026",
                  ENTREGADA,
                  "Entrega confirmada el 9 de octubre, con foto de la entidad"),
              new Donacion(
                  "fideos",
                  1,
                  "Fideos secos",
                  "8 kg",
                  ALIMENTOS,
                  ICONO_ALIMENTOS,
                  TEXTO_ALIMENTOS,
                  COMEDOR,
                  "1 de octubre de 2026",
                  ENTREGADA,
                  "Entrega confirmada el 5 de octubre, con foto de la entidad")),
          List.of(
              new Categoria(ALIMENTOS, TEXTO_ALIMENTOS),
              new Categoria(ROPA, TEXTO_ROPA),
              new Categoria("escolares", "Útiles escolares")),
          "hoy, 15 de octubre, 16:25");

  @Override
  public MisDonacionesVista misDonaciones(UUID donante) {
    return DONACIONES;
  }
}
