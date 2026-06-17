package grupo5.notificaciones.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.notificaciones.models.entities.notificaciones.EstadoNotificacion;
import grupo5.notificaciones.models.entities.notificaciones.Notificacion;
import grupo5.notificaciones.models.entities.personas.Persona;
import grupo5.notificaciones.models.repositories.NotificacionRepositoryEnMemoria;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NotificacionRepositoryEnMemoriaTest {

  private NotificacionRepositoryEnMemoria repository;

  @BeforeEach
  void setUp() {
    repository = new NotificacionRepositoryEnMemoria();
  }

  @Test
  void deberiaFiltrarNotificacionesPorEstado() {
    Persona persona = new Persona();
    persona.setId(1L);

    Notificacion notificacionPendiente = new Notificacion(persona, "Mensaje pendiente");

    Notificacion notificacionEnviada = new Notificacion(persona, "Mensaje enviado");
    notificacionEnviada.setEstadoNotificacion(EstadoNotificacion.ENVIADA);

    repository.save(notificacionPendiente);
    repository.save(notificacionEnviada);

    List<Notificacion> resultado = repository.findByEstado(EstadoNotificacion.PENDIENTE);

    assertEquals(1, resultado.size(), "Debería encontrar exactamente 1 notificación pendiente");
    assertEquals(
        notificacionPendiente.getId(),
        resultado.get(0).getId(),
        "El ID debe coincidir con la notificación pendiente");
  }

  @Test
  void deberiaFiltrarNotificacionesPorIdDePersona() {
    Persona persona1 = new Persona();
    persona1.setId(10L);

    Persona persona2 = new Persona();
    persona2.setId(20L);

    Notificacion notificacionPersona1 = new Notificacion(persona1, "Mensaje para persona 1");
    Notificacion notificacionPersona2 = new Notificacion(persona2, "Mensaje para persona 2");

    repository.save(notificacionPersona1);
    repository.save(notificacionPersona2);

    List<Notificacion> resultado = repository.findByPersonaId(10L);

    assertEquals(1, resultado.size(), "Debería encontrar 1 notificación para la persona 10");
    assertEquals(
        notificacionPersona1.getId(),
        resultado.get(0).getId(),
        "Debería devolver la notificación de la persona correcta");
  }

  @Test
  void deberiaDevolverListaVaciaSiLaPersonaNoTieneNotificaciones() {
    Persona persona1 = new Persona();
    persona1.setId(1L);
    Notificacion notificacion = new Notificacion(persona1, "Mensaje");
    repository.save(notificacion);

    List<Notificacion> resultado = repository.findByPersonaId(99L); // ID que no existe

    assertTrue(resultado.isEmpty(), "La lista debería estar vacía para un ID inexistente");
  }
}
