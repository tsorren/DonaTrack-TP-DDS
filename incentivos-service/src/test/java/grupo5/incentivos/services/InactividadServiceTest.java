package grupo5.incentivos.services;

import static org.mockito.Mockito.*;

import grupo5.incentivos.infrastructure.INotificacionesClient;
import grupo5.incentivos.models.entities.inactividad.CriterioInactividad;
import grupo5.incentivos.models.entities.inactividad.DonanteInactivo;
import grupo5.incentivos.models.entities.inactividad.GestorDeInactivos;
import grupo5.incentivos.models.repositories.DonanteIncentivosRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InactividadServiceTest {

  @Mock private INotificacionesClient notificacionesClient;
  @Mock private GestorDeInactivos gestorDeInactivos;
  @Mock private CriterioInactividad criterio;

  private InactividadService service;
  private DonanteIncentivosRepository repository;

  @BeforeEach
  void setUp() {
    repository = new DonanteIncentivosRepository();
    service =
        new InactividadService(
            repository, gestorDeInactivos, List.of(criterio), notificacionesClient);
  }

  @Test
  void procesarInactividad_deberiaNotificarDonantesInactivos() {
    UUID donanteId = UUID.randomUUID();
    UUID personaId = UUID.randomUUID();
    DonanteInactivo inactivo = new DonanteInactivo(donanteId, personaId, 25, LocalDate.now());
    when(gestorDeInactivos.procesarInactividad(any(), any())).thenReturn(List.of(inactivo));

    service.procesarInactividad();

    verify(notificacionesClient).notificarInactividad(personaId, 25);
  }

  @Test
  void procesarInactividad_noDeberiaLanzarExcepcionSiFallaNotificacion() {
    UUID donanteId = UUID.randomUUID();
    UUID personaId = UUID.randomUUID();
    DonanteInactivo inactivo = new DonanteInactivo(donanteId, personaId, 25, LocalDate.now());
    when(gestorDeInactivos.procesarInactividad(any(), any())).thenReturn(List.of(inactivo));
    doThrow(new RuntimeException("Error simulado"))
        .when(notificacionesClient)
        .notificarInactividad(any(), anyInt());

    service.procesarInactividad();

    verify(notificacionesClient).notificarInactividad(personaId, 25);
  }
}
