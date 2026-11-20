package grupo5.notificaciones.infrastructure.adapters;

import grupo5.notificaciones.infrastructure.NotificacionRouter;
import grupo5.notificaciones.models.entities.personas.Correo;
import grupo5.notificaciones.models.entities.personas.Telefono;
import grupo5.notificaciones.models.ports.NotificacionSender;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/** Cuenta cada intento de envío por canal y resultado, y delega en el router. */
@Component
@Primary
public class NotificacionSenderConMetricas implements NotificacionSender {

  private static final String ENVIOS = "donatrack.notificaciones.envios";

  private final NotificacionRouter router;
  private final MeterRegistry registro;

  public NotificacionSenderConMetricas(NotificacionRouter router, MeterRegistry registro) {
    this.router = router;
    this.registro = registro;
  }

  @Override
  public boolean enviarA(Correo correo, String mensaje) {
    return enviarContando("correo", () -> router.enviarA(correo, mensaje));
  }

  @Override
  public boolean enviarA(Telefono telefono, String mensaje) {
    String canal = telefono.getTipo().name().toLowerCase(Locale.ROOT);
    return enviarContando(canal, () -> router.enviarA(telefono, mensaje));
  }

  private boolean enviarContando(String canal, BooleanSupplier envio) {
    try {
      boolean enviado = envio.getAsBoolean();
      contar(canal, enviado ? "enviado" : "fallido");
      return enviado;
    } catch (RuntimeException e) {
      contar(canal, "fallido");
      throw e;
    }
  }

  private void contar(String canal, String resultado) {
    registro.counter(ENVIOS, "canal", canal, "resultado", resultado).increment();
  }
}
