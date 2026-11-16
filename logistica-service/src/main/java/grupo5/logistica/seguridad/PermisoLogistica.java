package grupo5.logistica.seguridad;

import grupo5.common.seguridad.Permiso;

/** Permisos de este servicio: cada nombre es un rol del cliente donatrack-api en el realm. */
public enum PermisoLogistica implements Permiso {
  VER_ENTREGAS,
  VER_RUTAS,
  CONFIRMAR_RECEPCION
}
