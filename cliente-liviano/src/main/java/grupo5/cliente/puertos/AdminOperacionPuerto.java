package grupo5.cliente.puertos;

import grupo5.cliente.vistas.AdminCamionesVista;

/**
 * La operación del admin: camiones, asignaciones y donaciones. Lo implementan la maqueta y el
 * adaptador REST; {@code cliente.datos.admin-operacion} elige cuál. {@code accion} es la pantalla
 * de resultado de la maqueta ({@code agregado}, {@code deshabilitado}…; '' es la lista): el
 * adaptador REST la ignora.
 */
public interface AdminOperacionPuerto {

  AdminCamionesVista camiones(String accion);

  /** Agrega un camión y devuelve su clave. */
  String agregarCamion(String patente, float capacidadKg, float volumen, float altura);

  /**
   * Deshabilita el camión (con el motivo, opcional) o lo vuelve a habilitar; devuelve su patente.
   */
  String cambiarCamion(String camion, boolean habilitado, String motivo);
}
