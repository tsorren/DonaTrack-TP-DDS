package grupo5.incentivos.seguridad;

import grupo5.common.seguridad.Permiso;

/** Permisos de este servicio: cada nombre es un rol del cliente donatrack-api en el realm. */
public enum PermisoIncentivos implements Permiso {
  VER_INCENTIVOS,
  CONFIGURAR_INSIGNIAS
}
