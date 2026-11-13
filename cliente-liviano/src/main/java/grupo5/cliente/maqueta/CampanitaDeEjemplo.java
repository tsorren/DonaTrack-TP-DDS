package grupo5.cliente.maqueta;

import grupo5.cliente.puertos.CampanitaPuerto;
import grupo5.cliente.vistas.CampanitaVista;
import grupo5.cliente.vistas.CampanitaVista.Fila;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * La campanita de la maqueta (R11-29, R11-30): las sin leer de ejemplo de cada área, las mismas
 * cantidades que la cabecera (4 del donante, 3 de la organización y 3 de la administración).
 * "Ahora" es el 15 de octubre de 2026. {@code ?escenario=vacio} y {@code ?escenario=error} en la
 * dirección del desplegable muestran esos estados. Abrir no guarda: lleva a la bandeja del área.
 */
@Component
@ConditionalOnProperty(
    name = "cliente.datos.avisos",
    havingValue = "maqueta",
    matchIfMissing = true)
class CampanitaDeEjemplo implements CampanitaPuerto {

  private static final String AHORA_DONANTE = "2026-10-15T16:25";
  private static final String AHORA_RESTO = "2026-10-15T18:45";

  private static final Map<String, CampanitaVista> DE_EJEMPLO =
      Map.of(
          "donante",
          new CampanitaVista(
              List.of(
                  new Fila("d1", "Donación en camino", "2026-10-15T14:20"),
                  new Fila("d2", "Donación entregada", "2026-10-14T11:05"),
                  new Fila("d3", "Subiste de categoría", "2026-10-12T09:30"),
                  new Fila("d4", "Misión cumplida", "2026-10-01T17:00")),
              4,
              AHORA_DONANTE),
          "entidad",
          new CampanitaVista(
              List.of(
                  new Fila("e1", "Entrega en camino", "2026-10-15T17:50"),
                  new Fila("e2", "Nueva donación asignada", "2026-10-15T10:15"),
                  new Fila("e3", "Entrega recibida", "2026-10-13T12:40")),
              3,
              AHORA_RESTO),
          "admin",
          new CampanitaVista(
              List.of(
                  new Fila("a1", "Terminó una importación", "2026-10-15T18:40"),
                  new Fila("a2", "Nuevo mensaje de contacto", "2026-10-14T16:10"),
                  new Fila("a3", "Una importación se cortó", "2026-10-12T09:00")),
              3,
              AHORA_RESTO));

  @Override
  public CampanitaVista recientes(String area, HttpServletRequest pedido) {
    String escenario = pedido.getParameter("escenario");
    if ("error".equals(escenario)) {
      throw new IllegalStateException("Escenario de error de la maqueta");
    }
    CampanitaVista ejemplo = DE_EJEMPLO.get(area);
    if (ejemplo == null || "vacio".equals(escenario)) {
      return new CampanitaVista(List.of(), 0, AHORA_RESTO);
    }
    return ejemplo;
  }

  @Override
  public String abrir(String area, String id, HttpServletRequest pedido) {
    return "/" + area + "-notificaciones";
  }
}
