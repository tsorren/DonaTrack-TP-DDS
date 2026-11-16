package grupo5.auth.correo;

/** Publica un evento de cuenta. Es la costura que permite probar el proveedor sin un broker. */
public interface PublicadorDeEventos {

  /**
   * Publica el evento y vuelve recién cuando el broker lo confirmó y lo encaminó a alguna cola.
   *
   * @param traceId el identificador de trazabilidad que viaja en el header {@code X-Trace-Id}
   */
  void publicar(EventoEnlaceDeCuenta evento, String traceId) throws PublicacionFallida;
}
