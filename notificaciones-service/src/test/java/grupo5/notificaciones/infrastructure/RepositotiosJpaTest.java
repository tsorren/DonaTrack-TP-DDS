package grupo5.notificaciones.infrastructure;

import static org.junit.jupiter.api.Assertions.*;

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
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class RepositoriosJpaTest {
  @Autowired private IPersonaRepository personaRepository;
  @Autowired private INotificacionRepository notificacionRepository;

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
    // 1. Guardar Persona en Base de Datos
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
    // 1. Guardar Notificación
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
