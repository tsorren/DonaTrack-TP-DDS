package grupo5.donaciones.models.entities.donantes;

/**
 * Un donante recién procesado del padrón, para mostrar el avance (R11-01): el nombre visible y si
 * se creó o se actualizó.
 */
public record UltimoImportado(String nombre, Resultado resultado) {

  public enum Resultado {
    NUEVO,
    ACTUALIZADO
  }
}
