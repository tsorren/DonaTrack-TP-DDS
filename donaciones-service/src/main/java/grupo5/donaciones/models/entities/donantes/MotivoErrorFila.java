package grupo5.donaciones.models.entities.donantes;

/** Por qué una fila del padrón no se importó (HB-14). */
public enum MotivoErrorFila {
  /** La línea no tiene la misma cantidad de columnas que el encabezado. */
  FORMATO_INVALIDO,
  /** Algún dato de la fila no pasa la validación (tipo de persona, documento, fecha, etc.). */
  DATOS_INVALIDOS,
  /** La persona existe pero fue dada de baja como donante: no se la reactiva. */
  DONANTE_DADO_DE_BAJA,
  /** El correo ya pertenece a otra persona con un documento distinto. */
  PERSONA_DUPLICADA_EN_CONFLICTO,
  /** Cualquier otra falla al aplicar la fila. */
  ERROR_INESPERADO
}
