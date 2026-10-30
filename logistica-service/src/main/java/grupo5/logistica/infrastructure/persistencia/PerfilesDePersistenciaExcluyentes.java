package grupo5.logistica.infrastructure.persistencia; // NOSONAR

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** Cada perfil elige un motor de persistencia: con los dos activos la aplicación no arranca. */
@Configuration
@Profile("postgres & mongo")
class PerfilesDePersistenciaExcluyentes {

  PerfilesDePersistenciaExcluyentes() {
    throw new IllegalStateException(
        "Los perfiles postgres y mongo no pueden estar activos a la vez: elegir uno");
  }
}
