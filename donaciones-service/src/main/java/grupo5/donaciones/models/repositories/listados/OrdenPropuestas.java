package grupo5.donaciones.models.repositories.listados;

/**
 * El orden del listado de propuestas (R11-24). {@code PUESTO}: el mejor puesto que le dio un
 * algoritmo (con un algoritmo elegido, el de ese), después la más reciente; {@code RECIENTE}: la
 * más reciente primero. Desde R11-35, por la fecha de la decisión: {@code DECISION_RECIENTE} (las
 * que no la tienen, al final) y {@code DECISION_ANTIGUA} (las que no la tienen, primero); las dos
 * desempatan por la fecha de creación.
 */
public enum OrdenPropuestas {
  PUESTO,
  RECIENTE,
  DECISION_RECIENTE,
  DECISION_ANTIGUA
}
