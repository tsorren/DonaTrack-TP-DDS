package grupo5.notificaciones.infrastructure.persistencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.common.testing.DisabledIfDockerUnavailable;
import grupo5.notificaciones.infrastructure.persistencia.adapters.NotificacionRepositoryJpaAdapter;
import grupo5.notificaciones.infrastructure.persistencia.adapters.PersonaRepositoryJpaAdapter;
import grupo5.notificaciones.infrastructure.persistencia.mappers.NotificacionPersistenciaMapper;
import grupo5.notificaciones.infrastructure.persistencia.mappers.PersonaPersistenciaMapper;
import grupo5.notificaciones.models.entities.notificaciones.EstadoNotificacion;
import grupo5.notificaciones.models.entities.notificaciones.Notificacion;
import grupo5.notificaciones.models.entities.personas.Correo;
import grupo5.notificaciones.models.entities.personas.Persona;
import grupo5.notificaciones.models.entities.personas.Telefono;
import grupo5.notificaciones.models.entities.personas.TipoPersona;
import grupo5.notificaciones.models.entities.personas.TipoTelefono;
import grupo5.notificaciones.models.repositories.INotificacionRepository;
import grupo5.notificaciones.models.repositories.IPersonaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

@DataJpaTest
@ActiveProfiles("postgres")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
  PersonaPersistenciaMapper.class,
  NotificacionPersistenciaMapper.class,
  PersonaRepositoryJpaAdapter.class,
  NotificacionRepositoryJpaAdapter.class
})
@Testcontainers
@DisabledIfDockerUnavailable
class RepositoriosJpaTest {

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("donatrack")
          .withUsername("notificaciones_user")
          .withPassword("notif_pass_2026")
          .withCopyFileToContainer(
              MountableFile.forClasspathResource("init-db/01-init-schemas-roles.sql"),
              "/docker-entrypoint-initdb.d/01-init-schemas-roles.sql");

  @Autowired private IPersonaRepository personaRepository;
  @Autowired private INotificacionRepository notificacionRepository;

  @Test
  void deberiaUsarAdaptadoresJpaEnLugarDeMemoria() {
    assertInstanceOf(PersonaRepositoryJpaAdapter.class, personaRepository);
    assertInstanceOf(NotificacionRepositoryJpaAdapter.class, notificacionRepository);
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
