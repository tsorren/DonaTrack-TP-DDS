package grupo5.notificaciones.mothers;

import grupo5.notificaciones.models.entities.notificaciones.Notificacion;
import grupo5.notificaciones.models.entities.personas.Persona;

/** Object Mother para {@code Notificacion} (Oleada 8). Nunca sufijo {@code *Test}. */
public final class NotificacionMother {

  private NotificacionMother() {}

  /**
   * Notificación recién creada (PENDIENTE) para la persona dada, con un mensaje genérico.
   *
   * <p>Recibe {@code Persona} completa (no solo el id) por comodidad de los tests — igual que
   * {@code Notificacion} desde la reconciliación con "vuelvo al PersonaId" (Anushig04), solo se usa
   * {@code persona.getId()} acá; quien necesite pasarle la {@code Persona} a {@code
   * notificar()}/{@code ordenarMedios()} más adelante tiene que guardar su propia referencia.
   */
  public static Notificacion pendiente(Persona persona) {
    return pendiente(persona, "Mensaje de prueba");
  }

  public static Notificacion pendiente(Persona persona, String mensaje) {
    return new Notificacion(persona.getId(), mensaje);
  }
}
