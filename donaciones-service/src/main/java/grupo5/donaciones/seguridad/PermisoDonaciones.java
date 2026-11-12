package grupo5.donaciones.seguridad;

import grupo5.common.seguridad.Permiso;

/** Permisos de este servicio: cada nombre es un rol del cliente donatrack-api en el realm. */
public enum PermisoDonaciones implements Permiso {
  VER_DONACIONES,
  REGISTRAR_DONACIONES,
  VER_NECESIDADES,
  GESTIONAR_NECESIDADES,
  EDITAR_DATOS_ORGANIZACION
}
