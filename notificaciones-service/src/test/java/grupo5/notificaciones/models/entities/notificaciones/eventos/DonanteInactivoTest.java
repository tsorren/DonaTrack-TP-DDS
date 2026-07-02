package grupo5.notificaciones.models.entities.notificaciones.eventos;

import grupo5.notificaciones.models.entities.notificaciones.Notificacion;
import grupo5.notificaciones.models.entities.personas.Persona;
import grupo5.notificaciones.models.entities.personas.TipoPersona;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DonanteInactivoTest {
  @Test
  @DisplayName(
      "Dado un donante inactivo por 30 días, debe generar una notificación con el mensaje correcto")
  void testGenerarNotificacionDonanteInactivo() {
    Persona mockDonante =
        new Persona(UUID.randomUUID(), new ArrayList<>(), "Juan Pérez", TipoPersona.HUMANA);

    Integer dias = 30;

    DonanteInactivo eventoInactividad = new DonanteInactivo();
    eventoInactividad.setPersona(mockDonante);
    eventoInactividad.setDiasInactividad(dias);

    List<Notificacion> notificaciones = eventoInactividad.generarNotificaciones();

    Assertions.assertEquals(1, notificaciones.size(), "Debe generar exactamente 1 notificación");

    Notificacion notificacionGenerada = notificaciones.get(0);

    Assertions.assertEquals(
        mockDonante.getId(),
        notificacionGenerada.getPersonaId(),
        "La notificación debe estar dirigida al donante correcto");
    Assertions.assertTrue(
        notificacionGenerada.getMensaje().contains("30 días"),
        "El mensaje debe incluir la cantidad de días de inactividad");
  }
}
