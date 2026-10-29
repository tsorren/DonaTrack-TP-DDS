package grupo5.auth.models.entities;

public enum Rol {
  DONANTE,
  ENTIDAD,
  ADMIN,
  /** Rol técnico para clients (client credentials). Nunca asignable a una persona. */
  SERVICIO
}
