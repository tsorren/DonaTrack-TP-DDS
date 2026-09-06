package grupo5.notificaciones.infrastructure.persistencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.notificaciones.models.entities.notificaciones.EstadoNotificacion;
import grupo5.notificaciones.models.entities.notificaciones.Notificacion;
import grupo5.notificaciones.models.entities.personas.Correo;
import grupo5.notificaciones.models.entities.personas.Persona;
import grupo5.notificaciones.models.entities.personas.Telefono;
import grupo5.notificaciones.models.entities.personas.TipoPersona;
import grupo5.notificaciones.models.entities.personas.TipoTelefono;
import grupo5.notificaciones.models.repositories.INotificacionRepository;
import grupo5.notificaciones.models.repositories.IPersonaRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

@SpringBootTest
@ActiveProfiles("postgres")
@Testcontainers
class RepositoriosJpaTest {

  private static String resolveInitScriptPath() {
    Path pathInSubmodule = Path.of("../persistencia/init-db/01-init-schemas-roles.sql");
    if (Files.exists(pathInSubmodule)) {
      return pathInSubmodule.toAbsolutePath().toString();
    }
    return Path.of("persistencia/init-db/01-init-schemas-roles.sql").toAbsolutePath().toString();
  }

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("donatrack")
          .withUsername("admin")
          .withPassword("admin_secure_password")
          .withCopyFileToContainer(
              MountableFile.forHostPath(resolveInitScriptPath()),
              "/docker-entrypoint-initdb.d/01-init-schemas-roles.sql");

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add(
        "spring.datasource.url",
        () -> {
          String jdbcUrl = postgres.getJdbcUrl();
          String separator = jdbcUrl.contains("?") ? "&" : "?";
          return jdbcUrl + separator + "currentSchema=notificaciones";
        });
    registry.add("spring.datasource.username", () -> "notificaciones_user");
    registry.add("spring.datasource.password", () -> "notif_pass_2026");
  }

  @Autowired private IPersonaRepository personaRepository;
  @Autowired private INotificacionRepository notificacionRepository;

  @Test
  void deberiaUsarAdaptadoresJpaEnLugarDeMemoria() {
    assertInstanceOf(
        grupo5.notificaciones.infrastructure.persistencia.adapters.PersonaRepositoryJpaAdapter
            .class,
        personaRepository);
    assertInstanceOf(
        grupo5.notificaciones.infrastructure.persistencia.adapters.NotificacionRepositoryJpaAdapter
            .class,
        notificacionRepository);
  }

  @Test
  void deberiaPersistirYRecuperarPersonaConMediosDeContacto() {
    UUID personaId = UUID.randomUUID();
    Correo correo = new Correo();
    correo.setDireccionCorreo("test@donatrack.org");
    correo.marcarComoPredeterminado();
    Telefono telefono = new Telefono();
    telefono.setCaracteristica("54");
    telefono.setCodigoArea("11");
    telefono.setNumero("12345678");
    telefono.setTipo(TipoTelefono.WHATSAPP);
    Persona persona =
        new Persona(personaId, List.of(correo, telefono), "Carlos Donante", TipoPersona.HUMANA);

    // 1. Guardar Persona en Base de Datos PostgreSQL real
    personaRepository.save(persona);

    // 2. Recuperar Persona
    Optional<Persona> recuperadaOpt = personaRepository.findById(personaId);
    assertTrue(recuperadaOpt.isPresent());
    Persona recuperada = recuperadaOpt.get();
    assertEquals("Carlos Donante", recuperada.getDenominacion());
    assertEquals(2, recuperada.getMediosDeContacto().size());
  }

  @Test
  void deberiaPersistirNotificacionYFiltrarPorEstado() {
    UUID personaId = UUID.randomUUID();
    Notificacion notificacion = new Notificacion(personaId, "Mensaje de prueba de persistencia");
    notificacion.actualizarEstado(EstadoNotificacion.ENVIADA);

    // 1. Guardar Notificación en PostgreSQL real
    notificacionRepository.save(notificacion);

    // 2. Buscar por Estado
    List<Notificacion> enviadas = notificacionRepository.findByEstado(EstadoNotificacion.ENVIADA);
    assertFalse(enviadas.isEmpty());
    assertTrue(enviadas.stream().anyMatch(n -> n.getId().equals(notificacion.getId())));

    // 3. Buscar por PersonaId
    List<Notificacion> porPersona = notificacionRepository.findByPersonaId(personaId);
    assertEquals(1, porPersona.size());
    assertEquals("Mensaje de prueba de persistencia", porPersona.get(0).getMensaje());
    assertEquals(2, porPersona.get(0).getHistorialEstado().size()); // PENDIENTE + ENVIADA
  }
}
