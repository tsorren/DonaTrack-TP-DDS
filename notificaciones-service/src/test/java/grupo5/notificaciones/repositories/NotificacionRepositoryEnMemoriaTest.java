package grupo5.notificaciones.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.notificaciones.models.entities.notificaciones.EstadoNotificacion;
import grupo5.notificaciones.models.entities.notificaciones.Notificacion;
import grupo5.notificaciones.models.entities.personas.Persona;
import grupo5.notificaciones.models.entities.personas.TipoPersona;
import grupo5.notificaciones.models.repositories.NotificacionRepositoryEnMemoria;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NotificacionRepositoryEnMemoriaTest {

  private NotificacionRepositoryEnMemoria repository;

  @BeforeEach
  void setUp() {
    repository = new NotificacionRepositoryEnMemoria();
  }

  private Persona crearPersona() {
    return new Persona(UUID.randomUUID(), new ArrayList<>(), "Test Persona", TipoPersona.HUMANA);
  }

  @Test
  void deberiaFiltrarNotificacionesPorEstado() {
    Persona persona = crearPersona();

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
    Persona persona1 = crearPersona();

    Persona persona2 = crearPersona();

    Notificacion notificacionPersona1 = new Notificacion(persona1, "Mensaje para persona 1");
    Notificacion notificacionPersona2 = new Notificacion(persona2, "Mensaje para persona 2");

    repository.save(notificacionPersona1);
    repository.save(notificacionPersona2);

    List<Notificacion> resultado = repository.findByPersonaId(persona1.getId());

    assertEquals(1, resultado.size(), "Debería encontrar 1 notificación para la persona 10");
    assertEquals(
        notificacionPersona1.getId(),
        resultado.get(0).getId(),
        "Debería devolver la notificación de la persona correcta");
  }

  @Test
  void deberiaDevolverListaVaciaSiLaPersonaNoTieneNotificaciones() {
    Persona persona1 = crearPersona();
    Notificacion notificacion = new Notificacion(persona1, "Mensaje");
    repository.save(notificacion);

    List<Notificacion> resultado =
        repository.findByPersonaId(java.util.UUID.randomUUID()); // ID que no existe

    assertTrue(resultado.isEmpty(), "La lista debería estar vacía para un ID inexistente");
  }
}
