package grupo5.notificaciones.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.notificaciones.models.entities.notificaciones.EstadoNotificacion;
import grupo5.notificaciones.models.entities.notificaciones.Notificacion;
import grupo5.notificaciones.models.entities.personas.Persona;
import grupo5.notificaciones.models.entities.personas.TipoPersona;
import grupo5.notificaciones.models.repositories.ConsultaBandeja;
import grupo5.notificaciones.models.repositories.PaginaBandeja;
import grupo5.notificaciones.models.repositories.impl.NotificacionRepositoryEnMemoria;
import grupo5.notificaciones.mothers.NotificacionMother;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

    Notificacion notificacionPendiente = NotificacionMother.pendiente(persona, "Mensaje pendiente");

    Notificacion notificacionEnviada = NotificacionMother.pendiente(persona, "Mensaje enviado");
    notificacionEnviada.actualizarEstado(EstadoNotificacion.ENVIADA);

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

    Notificacion notificacionPersona1 =
        NotificacionMother.pendiente(persona1, "Mensaje para persona 1");
    Notificacion notificacionPersona2 =
        NotificacionMother.pendiente(persona2, "Mensaje para persona 2");

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
    Notificacion notificacion = NotificacionMother.pendiente(persona1, "Mensaje");
    repository.save(notificacion);

    List<Notificacion> resultado =
        repository.findByPersonaId(java.util.UUID.randomUUID()); // ID que no existe

    assertTrue(resultado.isEmpty(), "La lista debería estar vacía para un ID inexistente");
  }

  // ==================== Bandeja paginada (R11-14) ====================

  private static final LocalDateTime BASE = LocalDateTime.of(2026, 10, 1, 9, 0);

  private static ConsultaBandeja pagina(int pagina, int tamanio) {
    return new ConsultaBandeja(null, null, Set.of(), Set.of(), false, pagina, tamanio);
  }

  /** {@code cantidad} notificaciones de la persona, la i-ésima i minutos después de BASE. */
  private List<Notificacion> guardar(UUID personaId, int cantidad) {
    List<Notificacion> guardadas = new ArrayList<>();
    for (int i = 0; i < cantidad; i++) {
      Notificacion n = new Notificacion(personaId, "Aviso " + i, BASE.plusMinutes(i));
      repository.save(n);
      guardadas.add(n);
    }
    return guardadas;
  }

  @Test
  void bandeja_devuelveLaPaginaPedidaDeLaMasNuevaALaMasViejaYElTotal() {
    UUID personaId = UUID.randomUUID();
    List<Notificacion> guardadas = guardar(personaId, 45);
    guardar(UUID.randomUUID(), 3);

    PaginaBandeja segunda = repository.bandeja(personaId, pagina(1, 20));

    assertEquals(45, segunda.total());
    assertEquals(20, segunda.notificaciones().size());
    // La 21.ª más nueva es la de índice 45 - 21 = 24.
    assertEquals(guardadas.get(24).getId(), segunda.notificaciones().get(0).getId());
    assertEquals(guardadas.get(5).getId(), segunda.notificaciones().get(19).getId());
  }

  @Test
  void bandeja_fueraDeRango_devuelveVaciaConElTotalReal() {
    UUID personaId = UUID.randomUUID();
    guardar(personaId, 45);

    PaginaBandeja fuera = repository.bandeja(personaId, pagina(9, 20));

    assertTrue(fuera.notificaciones().isEmpty());
    assertEquals(45, fuera.total());
  }

  @Test
  void bandeja_filtraPorLecturaTextoSinTildesYTipo() {
    UUID personaId = UUID.randomUUID();
    for (int i = 0; i < 10; i++) {
      Notificacion n =
          new Notificacion(
              personaId,
              i < 3 ? "El camión salió con tus lentejas " + i : "Otra novedad " + i,
              BASE.plusMinutes(i));
      n.etiquetar(i < 5 ? "DONACION_EN_CAMINO" : "DONACION_ASIGNADA", null);
      repository.save(n);
    }
    Notificacion leida = new Notificacion(personaId, "El camión llegó", BASE);
    leida.etiquetar("DONACION_EN_CAMINO", null);
    leida.marcarComoLeida();
    repository.save(leida);

    PaginaBandeja camion =
        repository.bandeja(
            personaId,
            new ConsultaBandeja(
                false, "camion", Set.of("DONACION_EN_CAMINO"), Set.of(), false, 0, 20));

    assertEquals(3, camion.total());
    assertTrue(camion.notificaciones().stream().allMatch(n -> n.getMensaje().contains("camión")));
  }

  @Test
  void bandeja_excluirTipoDejaEntrarLasQueNoTienenTipo() {
    UUID personaId = UUID.randomUUID();
    repository.save(new Notificacion(personaId, "Sin tipo 1", BASE));
    repository.save(new Notificacion(personaId, "Sin tipo 2", BASE.plusMinutes(1)));
    Notificacion mision = new Notificacion(personaId, "Misión", BASE.plusMinutes(2));
    mision.etiquetar("MISION_CUMPLIDA", null);
    repository.save(mision);
    for (int i = 0; i < 5; i++) {
      Notificacion asignada = new Notificacion(personaId, "Asignada", BASE.plusMinutes(3 + i));
      asignada.etiquetar("DONACION_ASIGNADA", null);
      repository.save(asignada);
    }

    PaginaBandeja otras =
        repository.bandeja(
            personaId,
            new ConsultaBandeja(null, null, Set.of(), Set.of("DONACION_ASIGNADA"), false, 0, 20));

    assertEquals(3, otras.total());
    assertTrue(
        otras.notificaciones().stream()
            .noneMatch(n -> "DONACION_ASIGNADA".equals(n.getTipoEvento())));
  }

  @Test
  void bandeja_conLaMismaFecha_cadaUnaApareceUnaSolaVezEnCadaSentido() {
    UUID personaId = UUID.randomUUID();
    for (int i = 0; i < 3; i++) {
      repository.save(new Notificacion(personaId, "Igual " + i, BASE));
    }
    for (boolean antiguas : List.of(false, true)) {
      Set<UUID> vistas = new HashSet<>();
      for (int p = 0; p < 3; p++) {
        PaginaBandeja una =
            repository.bandeja(
                personaId, new ConsultaBandeja(null, null, Set.of(), Set.of(), antiguas, p, 1));
        vistas.add(una.notificaciones().get(0).getId());
      }
      assertEquals(3, vistas.size());
    }
  }
}
