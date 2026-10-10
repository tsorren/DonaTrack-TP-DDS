package grupo5.notificaciones.infrastructure.adapters;

import grupo5.notificaciones.infrastructure.adapters.politicas.CriterioFalloSimulado;
import java.security.SecureRandom;

/** Clase base para consolidar la lógica común de simulación y evitar código duplicado. */
public abstract class BaseAdapterSimulado {

  protected final CriterioFalloSimulado criterioFallo;
  protected final SecureRandom random = new SecureRandom();

  protected BaseAdapterSimulado(CriterioFalloSimulado criterioFallo) {
    this.criterioFallo = criterioFallo;
  }

  protected void simularLatenciaDeRed() {
    try {
      // Simula un tiempo de respuesta de API entre 100 y 500 ms
      long latencia = 100L + this.random.nextInt(400);
      Thread.sleep(latencia);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  protected String enmascararNumero(String numero) {
    if (numero == null || numero.length() < 4) return "***";
    return "*".repeat(numero.length() - 4) + numero.substring(numero.length() - 4);
  }
}
