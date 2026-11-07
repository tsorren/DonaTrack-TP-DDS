package grupo5.donaciones.services;

/** Situación de una persona frente al rol de donante al intentar registrarla sin reactivarla. */
public enum EstadoRegistroDonante {
  /** No era donante y se la registró ahora. */
  CREADO,
  /** Ya era donante activo: no se hizo nada. */
  YA_REGISTRADO,
  /** Era donante pero está dada de baja: no se la reactivó. */
  DADO_DE_BAJA
}
