package grupo5.notificaciones.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import grupo5.notificaciones.models.entities.notificaciones.EstadoNotificacion;
import grupo5.notificaciones.models.entities.notificaciones.Notificacion;
import grupo5.notificaciones.models.entities.notificaciones.events.NotificacionCreada;
import grupo5.notificaciones.models.entities.notificaciones.events.NotificacionEnviada;
import grupo5.notificaciones.models.entities.notificaciones.events.NotificacionFallida;
import grupo5.notificaciones.models.entities.personas.Correo;
import grupo5.notificaciones.models.entities.personas.Persona;
import grupo5.notificaciones.models.entities.personas.TipoPersona;
import grupo5.notificaciones.models.ports.NotificacionSender;
import grupo5.notificaciones.models.repositories.INotificacionRepository;
import grupo5.notificaciones.models.repositories.IPersonaRepository;
import grupo5.notificaciones.services.gestores.NotificacionGestor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class NotificacionGestorTest {

  @Mock private INotificacionRepository repository;
  @Mock private IPersonaRepository personaRepository;
  @Mock private NotificacionSender sender;
  @Mock private ApplicationEventPublisher eventPublisher;

  private NotificacionGestor gestor;

  @BeforeEach
  void setUp() {
    gestor = new NotificacionGestor(repository, personaRepository, sender, eventPublisher);
  }

  @Test
  void notificarPendientes_conMedioQueEnvia_deberiaQuedarEnviada() {
    Persona persona = new Persona(UUID.randomUUID(), new ArrayList<>(), "Juan", TipoPersona.HUMANA);
    Correo correo = new Correo();
    correo.setDireccionCorreo("juan@test.com");
    correo.marcarComoPredeterminado();
    persona.agregarMedioDeContacto(correo);

    Notificacion notificacion = new Notificacion(persona.getId(), "Hola");
    when(personaRepository.findById(persona.getId())).thenReturn(Optional.of(persona));
    when(repository.findByEstado(EstadoNotificacion.PENDIENTE)).thenReturn(List.of(notificacion));
    when(sender.enviarA(any(Correo.class), anyString())).thenReturn(true);

    gestor.notificarPendientes();

    assertEquals(EstadoNotificacion.ENVIADA, notificacion.getEstadoNotificacion());
    verify(repository, times(1)).save(notificacion);
    // Oleada 2 (RF-02): notificar() generó NotificacionEnviada; el gestor debe publicarlo y
    // dejar la lista de domain events de la propia notificación limpia después.
    verify(eventPublisher, times(1)).publishEvent(any(NotificacionEnviada.class));
    assertEquals(List.of(), notificacion.getDomainEvents());
  }

  @Test
  void notificarPendientes_conPersonaSinMedios_deberiaQuedarFallida() {
    Persona persona = new Persona(UUID.randomUUID(), new ArrayList<>(), "Juan", TipoPersona.HUMANA);
    Notificacion notificacion = new Notificacion(persona.getId(), "Hola");
    when(repository.findByEstado(EstadoNotificacion.PENDIENTE)).thenReturn(List.of(notificacion));

    gestor.notificarPendientes();

    assertEquals(EstadoNotificacion.FALLIDA, notificacion.getEstadoNotificacion());
    verify(sender, never()).enviarA(any(Correo.class), anyString());
    verify(eventPublisher, times(1)).publishEvent(any(NotificacionFallida.class));
    assertEquals(List.of(), notificacion.getDomainEvents());
  }

  @Test
  void onNotificacionCreada_deberiaDelegarANotificarPendientes() {
    Persona persona = new Persona(UUID.randomUUID(), new ArrayList<>(), "Juan", TipoPersona.HUMANA);
    Correo correo = new Correo();
    correo.setDireccionCorreo("juan@test.com");
    correo.marcarComoPredeterminado();
    persona.agregarMedioDeContacto(correo);

    Notificacion notificacion = new Notificacion(persona.getId(), "Hola");
    when(personaRepository.findById(persona.getId())).thenReturn(Optional.of(persona));
    when(repository.findByEstado(EstadoNotificacion.PENDIENTE)).thenReturn(List.of(notificacion));
    when(sender.enviarA(any(Correo.class), anyString())).thenReturn(true);

    gestor.onNotificacionCreada(
        new NotificacionCreada(notificacion.getId(), persona.getId(), LocalDateTime.now()));

    assertEquals(EstadoNotificacion.ENVIADA, notificacion.getEstadoNotificacion());
    verify(repository, times(1)).save(notificacion);
  }
}
