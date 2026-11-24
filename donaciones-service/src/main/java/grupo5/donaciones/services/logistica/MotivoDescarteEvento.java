package grupo5.donaciones.services.logistica;

/** Por qué se descartó un evento de vuelta de un proveedor de logística. */
public enum MotivoDescarteEvento {
  SIN_PROVEEDOR,
  SIN_TOKEN_CONFIGURADO,
  TOKEN_INCORRECTO,
  DONACION_AJENA
}
