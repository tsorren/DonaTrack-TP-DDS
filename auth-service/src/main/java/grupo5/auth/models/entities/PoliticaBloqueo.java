package grupo5.auth.models.entities;

import java.time.Duration;

/** RNF-AUTH-07: N fallos dentro de una ventana bloquean la cuenta por una duración. */
public record PoliticaBloqueo(int maxIntentos, Duration ventana, Duration duracionBloqueo) {

  public static final PoliticaBloqueo POR_DEFECTO =
      new PoliticaBloqueo(5, Duration.ofMinutes(15), Duration.ofMinutes(15));

  public PoliticaBloqueo {
    if (maxIntentos < 1 || ventana.isNegative() || duracionBloqueo.isNegative()) {
      throw new DominioAuthException("Política de bloqueo inválida");
    }
  }
}
