package grupo5.cliente.puertos;

import grupo5.cliente.vistas.AdminActividadVista;
import grupo5.cliente.vistas.ConsultaActividad;

/**
 * El registro de actividad de administración (R11-26, ficha
 * docs/interfaz/flujos/r11-p2-actividad.md): quién hizo qué y cuándo, de a una página, y la
 * anotación de lo que el sitio hace fuera de Donaciones (en Keycloak o en Logística). Lo
 * implementan la maqueta y el adaptador REST; lo elige {@code cliente.datos.admin-actividad}.
 */
public interface AdminActividadPuerto {

  /** Lo que el sitio anota: lo que hace sin pasar por Donaciones. */
  enum AccionDelSitio {
    ADMINISTRADOR_DADO_DE_ALTA,
    CONTRASENA_PROVISORIA_NUEVA,
    CAMION_AGREGADO,
    CAMION_DESHABILITADO,
    CAMION_HABILITADO
  }

  /**
   * La página pedida, con los filtros ya entendidos. {@code propia} es la cuenta de la sesión (el
   * {@code sub}), para "Solo lo mío" y la marca "(vos)"; {@code escenario}, el de la maqueta.
   */
  AdminActividadVista actividad(ConsultaActividad consulta, String propia, String escenario);

  /**
   * Anota una acción que ya salió bien, con el token de la sesión (el autor lo pone Donaciones).
   * Nunca falla: si no se puede anotar, deja un WARN con el traceId y la acción sigue hecha. {@code
   * detalle}: la patente de un camión; null para una cuenta.
   */
  void anotar(AccionDelSitio accion, String objetoId, String detalle);
}
