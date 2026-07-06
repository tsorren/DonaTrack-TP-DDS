package grupo5.donaciones.config;

import grupo5.donaciones.models.entities.personas.Correo;
import grupo5.donaciones.models.entities.personas.Genero;
import grupo5.donaciones.models.entities.personas.Humana;
import grupo5.donaciones.models.entities.personas.factories.PersonaFactory;
import grupo5.donaciones.models.repositories.IPersonasRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Siembra la persona administradora del sistema al levantar donaciones-service, con un id fijo (ver
 * {@link AdminConstantes#ID_ADMIN}) compartido con notificaciones-service. Al ser en memoria, se
 * recrea limpia en cada arranque; el chequeo previo es una salvaguarda defensiva.
 */
@Component
public class AdminSeeder implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

  private final IPersonasRepository personasRepository;

  public AdminSeeder(IPersonasRepository personasRepository) {
    this.personasRepository = personasRepository;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (personasRepository.findByDocumento(AdminConstantes.DOCUMENTO_ADMIN).isPresent()) {
      return;
    }

    Humana admin =
        PersonaFactory.crearHumanaConId(
            AdminConstantes.ID_ADMIN,
            AdminConstantes.NOMBRE_ADMIN,
            AdminConstantes.APELLIDO_ADMIN,
            null,
            Genero.PREFIERO_NO_DECIR);
    admin.actualizarDocumento(
        AdminConstantes.TIPO_DOCUMENTO_ADMIN, AdminConstantes.DOCUMENTO_ADMIN);

    // El constructor de Persona agrega un Telefono() vacío por defecto; lo reemplazamos por
    // un medio de contacto real y lo marcamos predeterminado.
    admin.limpiarMediosDeContacto();
    Correo correo = new Correo();
    correo.setDireccionCorreo(AdminConstantes.EMAIL_ADMIN);
    admin.agregarMedioDeContacto(correo);
    admin.definirMedioDeContactoPredeterminado(correo);

    personasRepository.save(admin);
    log.info(
        "[ADMIN_SEEDER] Persona administradora sembrada con id fijo {}", AdminConstantes.ID_ADMIN);
  }
}
