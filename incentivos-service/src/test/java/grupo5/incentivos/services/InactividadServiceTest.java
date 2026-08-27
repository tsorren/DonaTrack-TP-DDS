package grupo5.incentivos.services;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import grupo5.incentivos.fixtures.DonanteIncentivosMotherTest;
import grupo5.incentivos.fixtures.EventoDonacionMotherTest;
import grupo5.incentivos.infrastructure.INotificacionesClient;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import grupo5.incentivos.models.entities.inactividad.CriterioInactividad;
import grupo5.incentivos.models.entities.inactividad.GestorDeInactivos;
import grupo5.incentivos.models.entities.inactividad.InactividadDonaciones;
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

  private InactividadService service;
  private DonanteIncentivosRepository repository;
  private GestorDeInactivos gestorDeInactivos;
  private List<CriterioInactividad> criterios;

  @Mock private INotificacionesClient notificacionesClient;

  @BeforeEach
  void setUp() {
    repository = new DonanteIncentivosRepository();
    criterios = List.of(new InactividadDonaciones(30));
    gestorDeInactivos = new GestorDeInactivos();
    service =
        new InactividadService(repository, gestorDeInactivos, criterios, notificacionesClient);
  }

  @Test
  void procesarInactividad_deberiaNotificarDonantesInactivosIdentificados() {
    UUID id = UUID.randomUUID();
    DonanteIncentivos inactivo =
        DonanteIncentivosMotherTest.conDonacion(
            id, EventoDonacionMotherTest.enFecha(LocalDate.now().minusDays(40)));
    repository.save(inactivo);

    service.procesarInactividad();

    verify(notificacionesClient, times(1)).notificarInactividad(inactivo.getIdPersona(), 40);
  }

  @Test
  void procesarInactividad_cuandoNoHayInactivos_noDeberiaEnviarNotificaciones() {
    UUID id = UUID.randomUUID();
    DonanteIncentivos activo =
        DonanteIncentivosMotherTest.conDonacion(
            id, EventoDonacionMotherTest.enFecha(LocalDate.now().minusDays(5)));
    repository.save(activo);

    service.procesarInactividad();

    verify(notificacionesClient, never()).notificarInactividad(any(), anyInt());
  }

  @Test
  void
      procesarInactividad_cuandoClienteNotificacionesFalla_noDeberiaLanzarExcepcionNiInterrumpirOtros() {
    UUID id1 = UUID.randomUUID();
    UUID id2 = UUID.randomUUID();
    DonanteIncentivos inactivo1 =
        DonanteIncentivosMotherTest.conDonacion(
            id1, EventoDonacionMotherTest.enFecha(LocalDate.now().minusDays(40)));
    DonanteIncentivos inactivo2 =
        DonanteIncentivosMotherTest.conDonacion(
            id2, EventoDonacionMotherTest.enFecha(LocalDate.now().minusDays(50)));
    repository.save(inactivo1);
    repository.save(inactivo2);

    doThrow(new RuntimeException("Error de red"))
        .when(notificacionesClient)
        .notificarInactividad(eq(inactivo1.getIdPersona()), anyInt());

    assertDoesNotThrow(() -> service.procesarInactividad());
    verify(notificacionesClient, times(1))
        .notificarInactividad(eq(inactivo2.getIdPersona()), anyInt());
  }
}
