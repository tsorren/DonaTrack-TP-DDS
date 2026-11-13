package grupo5.donaciones.infrastructure.persistencia.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.common.testing.DisabledIfDockerUnavailable;
import grupo5.donaciones.infrastructure.persistencia.repositories.SpringDataActividadRepository;
import grupo5.donaciones.models.entities.actividad.AccionDeAdministracion;
import grupo5.donaciones.models.entities.actividad.Actividad;
import grupo5.donaciones.models.repositories.IActividadRepository;
import grupo5.donaciones.models.repositories.listados.CriterioActividad;
import grupo5.donaciones.models.repositories.listados.Paginado;
import grupo5.donaciones.models.repositories.listados.Tramo;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** R11-26 (C4 de la spec): la consulta del registro resuelta en PostgreSQL. */
@DataJpaTest
@ActiveProfiles("postgres")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ActividadRepositoryJpaAdapter.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Testcontainers
@DisabledIfDockerUnavailable
class ActividadRepositoryJpaAdapterTest {

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  private static final LocalDateTime BASE = LocalDateTime.of(2026, 11, 12, 18, 0);

  @Autowired private IActividadRepository repositorio;
  @Autowired private SpringDataActividadRepository filas;

  @BeforeEach
  void limpiar() {
    filas.deleteAll();
  }

  private Actividad guardar(
      String autor, AccionDeAdministracion accion, LocalDateTime cuando, String detalle) {
    Actividad actividad = new Actividad(UUID.randomUUID(), cuando, autor, accion, "o", detalle);
    repositorio.guardar(actividad);
    return actividad;
  }

  @Test
  void guardaYLeeCadaCampo() {
    Actividad guardada =
        guardar("admin-a", AccionDeAdministracion.CAMION_AGREGADO, BASE, "AB123CD");

    Tramo<Actividad> tramo =
        repositorio.buscar(new CriterioActividad(null, null, null, null, new Paginado(0, 20)));

    assertEquals(1, tramo.total());
    assertEquals(guardada, tramo.elementos().get(0));
  }

  @Test
  void ordenaDeLaMasRecienteALaMasVieja_yCortaLaPagina() {
    IntStream.range(0, 25)
        .forEach(
            i ->
                guardar(
                    "admin-a",
                    AccionDeAdministracion.DONACION_VENCIDA,
                    BASE.minusHours(i),
                    "d" + i));

    Tramo<Actividad> primera =
        repositorio.buscar(new CriterioActividad(null, null, null, null, new Paginado(0, 20)));
    Tramo<Actividad> segunda =
        repositorio.buscar(new CriterioActividad(null, null, null, null, new Paginado(1, 20)));
    Tramo<Actividad> fuera =
        repositorio.buscar(new CriterioActividad(null, null, null, null, new Paginado(9, 20)));

    assertEquals(25, primera.total());
    assertEquals(20, primera.elementos().size());
    assertEquals("d0", primera.elementos().get(0).detalle());
    assertEquals("d19", primera.elementos().get(19).detalle());
    assertEquals(5, segunda.elementos().size());
    assertTrue(fuera.elementos().isEmpty());
    assertEquals(25, fuera.total());
  }

  @Test
  void combinaLosFiltrosYCuentaConLosMismos() {
    guardar("admin-a", AccionDeAdministracion.DONACION_VENCIDA, BASE.minusDays(1), null);
    guardar("admin-a", AccionDeAdministracion.DONACION_VENCIDA, BASE, null);
    guardar("admin-a", AccionDeAdministracion.DONACION_VENCIDA, BASE.minusDays(3), null);
    guardar("admin-a", AccionDeAdministracion.CAMION_AGREGADO, BASE, "AB123CD");
    guardar("admin-b", AccionDeAdministracion.DONACION_VENCIDA, BASE, null);
    // El día pedido incluye hasta las 23:59 de "hasta"
    guardar(
        "admin-a",
        AccionDeAdministracion.DONACION_VENCIDA,
        LocalDate.of(2026, 11, 12).atTime(23, 59),
        null);

    Tramo<Actividad> tramo =
        repositorio.buscar(
            new CriterioActividad(
                Set.of("admin-a"),
                Set.of(AccionDeAdministracion.DONACION_VENCIDA),
                LocalDate.of(2026, 11, 10),
                LocalDate.of(2026, 11, 12),
                new Paginado(0, 1)));

    assertEquals(3, tramo.total());
    assertEquals(1, tramo.elementos().size());
    assertEquals("admin-a", tramo.elementos().get(0).autor());
  }
}
