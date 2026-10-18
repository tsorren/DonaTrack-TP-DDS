package grupo5.notificaciones.config;

import grupo5.notificaciones.models.entities.personas.Correo;
import grupo5.notificaciones.models.entities.personas.MedioDeContacto;
import grupo5.notificaciones.models.entities.personas.Persona;
import grupo5.notificaciones.models.entities.personas.TipoPersona;
import grupo5.notificaciones.models.repositories.IPersonaRepository;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Siembra la persona administradora del sistema al levantar notificaciones-service, con el mismo id
 * fijo que donaciones-service (ver {@link AdminConstantes#ID_ADMIN}). Esto garantiza que exista
 * apenas arranca, sin depender de que donaciones-service ya esté levantado ni de que la
 * sincronización asincrónica haya llegado a tiempo.
 */
@Component
public class AdminSeeder implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

  private final IPersonaRepository personaRepository;

  public AdminSeeder(IPersonaRepository personaRepository) {
    this.personaRepository = personaRepository;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (personaRepository.existsById(AdminConstantes.ID_ADMIN)) {
      return;
    }

    Correo correo = new Correo();
    correo.setDireccionCorreo(AdminConstantes.EMAIL_ADMIN);
    correo.marcarComoPredeterminado();

    List<MedioDeContacto> medios = new ArrayList<>();
    medios.add(correo);

    Persona admin =
        new Persona(
            AdminConstantes.ID_ADMIN,
            medios,
            AdminConstantes.DENOMINACION_ADMIN,
            TipoPersona.HUMANA);

    personaRepository.save(admin);
    log.info(
        "[ADMIN_SEEDER] Persona administradora sembrada con id fijo {}", AdminConstantes.ID_ADMIN);
  }
}
