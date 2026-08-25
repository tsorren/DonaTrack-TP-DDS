package grupo5.notificaciones.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import grupo5.notificaciones.models.entities.notificaciones.EstadoNotificacion;
import grupo5.notificaciones.models.entities.notificaciones.Notificacion;
import grupo5.notificaciones.models.entities.personas.Correo;
import grupo5.notificaciones.models.entities.personas.Persona;
import grupo5.notificaciones.models.entities.personas.TipoPersona;
import grupo5.notificaciones.models.ports.NotificacionSender;
import grupo5.notificaciones.models.repositories.INotificacionRepository;
import grupo5.notificaciones.services.gestores.NotificacionGestor;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificacionGestorTest {

  @Mock private INotificacionRepository repository;
  @Mock private NotificacionSender sender;

  private NotificacionGestor gestor;

  @BeforeEach
  void setUp() {
    gestor = new NotificacionGestor(repository, sender);
  }

  @Test
  void notificarPendientes_conMedioQueEnvia_deberiaQuedarEnviada() {
    Persona persona = new Persona(UUID.randomUUID(), new ArrayList<>(), "Juan", TipoPersona.HUMANA);
    Correo correo = new Correo();
    correo.setDireccionCorreo("juan@test.com");
    correo.setEsPredeterminado(true);
    persona.agregarMedioDeContacto(correo);

    Notificacion notificacion = new Notificacion(persona, "Hola");
    when(repository.findByEstado(EstadoNotificacion.PENDIENTE)).thenReturn(List.of(notificacion));
    when(sender.enviarA(any(Correo.class), anyString())).thenReturn(true);

    gestor.notificarPendientes();

    assertEquals(EstadoNotificacion.ENVIADA, notificacion.getEstadoNotificacion());
    verify(repository, times(1)).save(notificacion);
  }

  @Test
  void notificarPendientes_conPersonaSinMedios_deberiaQuedarFallida() {
    Persona persona = new Persona(UUID.randomUUID(), new ArrayList<>(), "Juan", TipoPersona.HUMANA);
    Notificacion notificacion = new Notificacion(persona, "Hola");
    when(repository.findByEstado(EstadoNotificacion.PENDIENTE)).thenReturn(List.of(notificacion));

    gestor.notificarPendientes();

    assertEquals(EstadoNotificacion.FALLIDA, notificacion.getEstadoNotificacion());
    verify(sender, never()).enviarA(any(Correo.class), anyString());
  }
}
