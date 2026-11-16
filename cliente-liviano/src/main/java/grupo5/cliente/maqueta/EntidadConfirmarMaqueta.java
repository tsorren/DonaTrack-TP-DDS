package grupo5.cliente.maqueta;

import grupo5.cliente.puertos.EntidadConfirmarPuerto;
import grupo5.cliente.vistas.EntidadEntregaVista;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Las dos entregas que esperan en la puerta del Comedor Barrio Ejemplo: las lentejas (hoy) y la
 * ropa (ayer, de quien ocultó su nombre: figura como Donante Sostenedor). Confirmar no guarda nada:
 * en la maqueta el formulario lo atiende SitioController.
 */
@Component
@ConditionalOnProperty(
    name = "cliente.datos.entidad",
    havingValue = "maqueta",
    matchIfMissing = true)
class EntidadConfirmarMaqueta implements EntidadConfirmarPuerto {

  private static final List<EntidadEntregaVista> ENTREGAS =
      List.of(
          new EntidadEntregaVista(
              "lentejas",
              "lentejas secas",
              "Lentejas secas, 15 kg",
              "15 kg",
              "Hoy, 14:30",
              "Hoy, 18:20",
              "Llegó hoy a las 18:20.",
              "Llegaron hoy a las 18:20.",
              "0001-00004521",
              "cat-alimentos",
              "Alimentos",
              "Lentejas secas, 40 kg, hasta el 31 de octubre.",
              "Van 10 kg recibidos.",
              "Tu necesidad de lentejas secas: 25 de 40 kg recibidos, hasta el 31 de octubre.",
              "Distribuidora Ejemplo SA",
              null,
              null,
              "lentejas-1",
              "18:45",
              15,
              "kg",
              true,
              "12 de 15 kg",
              "Dos bolsas rotas."),
          new EntidadEntregaVista(
              "ropa",
              "ropa de abrigo",
              "Ropa de abrigo, 10 unidades",
              "10 unidades",
              "Ayer, 15:50",
              "Ayer, 17:10",
              "Llegó ayer a las 17:10.",
              "Llegó ayer a las 17:10.",
              "0001-00004498",
              "cat-ropa",
              "Ropa",
              "Ropa de abrigo, 30 unidades, hasta el 15 de noviembre.",
              "Van 5 unidades recibidas.",
              "Tu necesidad de ropa de abrigo: 15 de 30 unidades recibidas, hasta el 15 de"
                  + " noviembre.",
              null,
              "Sostenedor",
              null,
              "ropa-1",
              "18:50",
              10,
              "unidades",
              false,
              "8 de 10 unidades",
              "Faltaba una caja."));

  @Override
  public List<EntidadEntregaVista> entregas(UUID entidad, String pedida) {
    return ENTREGAS;
  }

  @Override
  public String hoy() {
    return "15 de octubre de 2026";
  }

  @Override
  public void confirmar(UUID entidad, String clave, Acta acta) {
    // En la maqueta no se guarda nada.
  }
}
