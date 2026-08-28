package grupo5.notificaciones.models.entities.notificaciones;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import grupo5.notificaciones.models.entities.notificaciones.eventos.CambioEstadoNotificacion;
import grupo5.notificaciones.models.entities.notificaciones.events.NotificacionCreada;
import grupo5.notificaciones.models.entities.notificaciones.events.NotificacionDomainEvent;
import grupo5.notificaciones.models.entities.notificaciones.events.NotificacionEnviada;
import grupo5.notificaciones.models.entities.notificaciones.events.NotificacionFallida;
import grupo5.notificaciones.models.entities.personas.Correo;
import grupo5.notificaciones.models.entities.personas.MedioDeContacto;
import grupo5.notificaciones.models.entities.personas.Persona;
import grupo5.notificaciones.models.entities.personas.Telefono;
import grupo5.notificaciones.models.entities.personas.TipoPersona;
import grupo5.notificaciones.models.entities.personas.TipoTelefono;
import grupo5.notificaciones.models.ports.Anonimizable;
import grupo5.notificaciones.models.ports.NotificacionSender;
import grupo5.notificaciones.mothers.MedioDeContactoMother;
import grupo5.notificaciones.mothers.NotificacionMother;
import grupo5.notificaciones.mothers.PersonaMother;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Cubre a {@code Notificacion} de punta a punta (Oleada 8): domain events y snapshots inmutables
 * (introducidos en Oleada 2, RF-02), {@code notificar()} (éxito, fallback, fallida, persona nula),
 * {@code ordenarMedios()} (predeterminado primero, 0/1/N medios, y los 2 casos borde de la Fase 0:
 * {@code esPredeterminado == null} y dos medios del mismo tipo empatados como predeterminados), y
 * que {@code historialEstado} acumula cada transición con un timestamp correcto.
 *
 * <p>Reconciliado con el commit "vuelvo al PersonaId" (Anushig04, c498244e, ver bitácora de la
 * reconciliación en el plan de oleadas): {@code Notificacion} referencia a la persona solo por
 * {@code UUID} — {@code notificar()}/{@code ordenarMedios()} reciben la {@code Persona} completa
 * como parámetro en el momento del envío, en vez de guardarla. Los tests reflejan eso: se guarda la
 * {@code Persona} en una variable local para usarla en la llamada, no dentro de {@code
 * Notificacion}.
 */
@ExtendWith(MockitoExtension.class)
class NotificacionTest {

  @Mock private NotificacionSender sender;

  private Persona persona;

  @BeforeEach
  void setUp() {
    persona = new Persona(UUID.randomUUID(), new ArrayList<>(), "Juan Perez", TipoPersona.HUMANA);
  }

  // ==================== Domain Events (Oleada 2, RF-02) ====================

  @Test
  void constructor_deberiaRegistrarNotificacionCreada() {
    Notificacion notificacion = new Notificacion(persona.getId(), "Hola");

    List<NotificacionDomainEvent> eventos = notificacion.getDomainEvents();

    assertEquals(1, eventos.size());
    assertInstanceOf(NotificacionCreada.class, eventos.get(0));
    NotificacionCreada evento = (NotificacionCreada) eventos.get(0);
    assertEquals(notificacion.getId(), evento.notificacionId());
    assertEquals(persona.getId(), evento.personaId());
  }

  @Test
  void constructor_conPersonaIdNulo_deberiaRegistrarNotificacionCreadaConPersonaIdNulo() {
    Notificacion notificacion = new Notificacion(null, "Hola");

    NotificacionCreada evento = (NotificacionCreada) notificacion.getDomainEvents().get(0);

    assertNull(evento.personaId());
  }

  @Test
  void actualizarEstado_aEnviada_deberiaRegistrarNotificacionEnviada() {
    Notificacion notificacion = new Notificacion(persona.getId(), "Hola");

    notificacion.actualizarEstado(EstadoNotificacion.ENVIADA);

    List<NotificacionDomainEvent> eventos = notificacion.getDomainEvents();
    assertEquals(2, eventos.size());
    assertInstanceOf(NotificacionCreada.class, eventos.get(0));
    assertInstanceOf(NotificacionEnviada.class, eventos.get(1));
  }

  @Test
  void actualizarEstado_aFallida_deberiaRegistrarNotificacionFallida() {
    Notificacion notificacion = new Notificacion(persona.getId(), "Hola");

    notificacion.actualizarEstado(EstadoNotificacion.FALLIDA);

    List<NotificacionDomainEvent> eventos = notificacion.getDomainEvents();
    assertEquals(2, eventos.size());
    assertInstanceOf(NotificacionFallida.class, eventos.get(1));
  }

  @Test
  void clearDomainEvents_deberiaVaciarLaListaInterna() {
    Notificacion notificacion = new Notificacion(persona.getId(), "Hola");

    notificacion.clearDomainEvents();

    assertEquals(List.of(), notificacion.getDomainEvents());
  }

  @Test
  void getDomainEvents_deberiaSerUnSnapshotInmutableQueNoCreceConTransicionesPosteriores() {
    Notificacion notificacion = new Notificacion(persona.getId(), "Hola");

    List<NotificacionDomainEvent> snapshot = notificacion.getDomainEvents();
    notificacion.actualizarEstado(EstadoNotificacion.ENVIADA);

    assertEquals(1, snapshot.size(), "el snapshot tomado antes de la transición no debe crecer");
    assertEquals(
        2, notificacion.getDomainEvents().size(), "un snapshot nuevo sí ve el evento nuevo");
    assertThrows(
        UnsupportedOperationException.class,
        () -> snapshot.add(new NotificacionEnviada(notificacion.getId(), null)));
  }

  @Test
  void getHistorialEstado_deberiaSerUnSnapshotInmutableQueNoCreceConTransicionesPosteriores() {
    Notificacion notificacion = new Notificacion(persona.getId(), "Hola");

    List<CambioEstadoNotificacion> snapshot = notificacion.getHistorialEstado();
    notificacion.actualizarEstado(EstadoNotificacion.ENVIADA);

    assertEquals(1, snapshot.size(), "el snapshot tomado antes de la transición no debe crecer");
    assertEquals(
        2, notificacion.getHistorialEstado().size(), "un snapshot nuevo sí ve la transición nueva");
    assertThrows(UnsupportedOperationException.class, () -> snapshot.add(null));
  }

  @Test
  void getHistorialEstado_yGetDomainEvents_noDebenDevolverLaMismaInstanciaEntreLlamadas() {
    Notificacion notificacion = new Notificacion(persona.getId(), "Hola");

    assertNotSame(notificacion.getHistorialEstado(), notificacion.getHistorialEstado());
    assertNotSame(notificacion.getDomainEvents(), notificacion.getDomainEvents());
  }

  // ==================== anonimizar() (Oleada 8 — sin cobertura antes) ====================

  @Test
  void anonimizar_deberiaReemplazarElMensajePorElValorAnonimizadoYNoTocarOtrosCampos() {
    Notificacion notificacion = new Notificacion(persona.getId(), "Mensaje con datos sensibles");

    notificacion.anonimizar();

    assertEquals(Anonimizable.VALOR_STRING, notificacion.getMensaje());
    assertEquals(persona.getId(), notificacion.getPersonaId());
    assertEquals(EstadoNotificacion.PENDIENTE, notificacion.getEstadoNotificacion());
  }

  // ==================== historialEstado: timestamps (Oleada 8) ====================

  @Test
  void historialEstado_deberiaAcumularCadaTransicionConTimestampDentroDeLaVentanaDeEjecucion() {
    LocalDateTime antesDeEjecutar = LocalDateTime.now();

    Notificacion notificacion = new Notificacion(persona.getId(), "Hola");
    notificacion.actualizarEstado(EstadoNotificacion.ENVIADA);

    LocalDateTime despuesDeEjecutar = LocalDateTime.now();
    List<CambioEstadoNotificacion> historial = notificacion.getHistorialEstado();

    assertEquals(2, historial.size());
    assertNull(historial.get(0).getEstadoAnterior());
    assertEquals(EstadoNotificacion.PENDIENTE, historial.get(0).getEstadoNuevo());
    assertEquals(EstadoNotificacion.PENDIENTE, historial.get(1).getEstadoAnterior());
    assertEquals(EstadoNotificacion.ENVIADA, historial.get(1).getEstadoNuevo());
    for (CambioEstadoNotificacion cambio : historial) {
      assertFalse(cambio.getTimestamp().isBefore(antesDeEjecutar), "timestamp antes de la ventana");
      assertFalse(
          cambio.getTimestamp().isAfter(despuesDeEjecutar), "timestamp después de la ventana");
    }
    assertFalse(
        historial.get(1).getTimestamp().isBefore(historial.get(0).getTimestamp()),
        "la segunda transición no puede tener un timestamp anterior a la primera");
  }

  // ==================== notificar(Persona, NotificacionSender) ====================

  @Test
  void notificar_conPrimerMedioExitoso_deberiaQuedarEnviada() {
    Correo correo = MedioDeContactoMother.correoPredeterminado();
    Persona destinatario = PersonaMother.conMedios(correo);
    Notificacion notificacion = NotificacionMother.pendiente(destinatario);
    when(sender.enviarA(eq(correo), anyString())).thenReturn(true);

    notificacion.notificar(destinatario, sender);

    assertEquals(EstadoNotificacion.ENVIADA, notificacion.getEstadoNotificacion());
  }

  @Test
  void notificar_conPrimerMedioLanzandoExcepcion_deberiaContinuarConElSegundoYQuedarEnviada() {
    Correo correo = MedioDeContactoMother.correoPredeterminado();
    Telefono telefono = MedioDeContactoMother.telefono();
    Persona destinatario = PersonaMother.conMedios(correo, telefono);
    Notificacion notificacion = NotificacionMother.pendiente(destinatario);
    when(sender.enviarA(eq(correo), anyString())).thenThrow(new RuntimeException("SMTP timeout"));
    when(sender.enviarA(eq(telefono), anyString())).thenReturn(true);

    notificacion.notificar(destinatario, sender);

    assertEquals(EstadoNotificacion.ENVIADA, notificacion.getEstadoNotificacion());
  }

  @Test
  void notificar_conPrimerMedioDevolviendoFalse_deberiaContinuarConElSegundoYQuedarEnviada() {
    Correo correo = MedioDeContactoMother.correoPredeterminado();
    Telefono telefono = MedioDeContactoMother.telefono();
    Persona destinatario = PersonaMother.conMedios(correo, telefono);
    Notificacion notificacion = NotificacionMother.pendiente(destinatario);
    when(sender.enviarA(eq(correo), anyString())).thenReturn(false);
    when(sender.enviarA(eq(telefono), anyString())).thenReturn(true);

    notificacion.notificar(destinatario, sender);

    assertEquals(EstadoNotificacion.ENVIADA, notificacion.getEstadoNotificacion());
  }

  @Test
  void notificar_conTodosLosMediosFallando_deberiaQuedarFallida() {
    Correo correo = MedioDeContactoMother.correoPredeterminado();
    Telefono telefono = MedioDeContactoMother.telefono();
    Persona destinatario = PersonaMother.conMedios(correo, telefono);
    Notificacion notificacion = NotificacionMother.pendiente(destinatario);
    when(sender.enviarA(eq(correo), anyString())).thenReturn(false);
    when(sender.enviarA(eq(telefono), anyString())).thenReturn(false);

    notificacion.notificar(destinatario, sender);

    assertEquals(EstadoNotificacion.FALLIDA, notificacion.getEstadoNotificacion());
  }

  @Test
  void notificar_conPersonaNula_deberiaQuedarFallidaInmediatamenteSinConsultarAlSender() {
    // La resolución de "no hay destinatario" ahora vive un nivel más arriba (quien busca la
    // Persona por personaId antes de llamar a notificar(), ej. NotificacionGestor) — acá se
    // caracteriza pasando null directamente, sin importar qué personaId tenga la Notificacion.
    Notificacion notificacion = new Notificacion(UUID.randomUUID(), "Hola");

    notificacion.notificar(null, sender);

    assertEquals(EstadoNotificacion.FALLIDA, notificacion.getEstadoNotificacion());
    verifyNoInteractions(sender);
  }

  // ==================== ordenarMedios(Persona) ====================

  @Test
  void ordenarMedios_conMedioPredeterminadoAgregadoSegundo_deberiaQuedarPrimero() {
    Telefono telefono = MedioDeContactoMother.telefono();
    Correo correoPredeterminado = MedioDeContactoMother.correoPredeterminado();
    Persona destinatario = PersonaMother.conMedios(telefono, correoPredeterminado);
    Notificacion notificacion = NotificacionMother.pendiente(destinatario);

    List<MedioDeContacto> ordenados = notificacion.ordenarMedios(destinatario);

    assertEquals(List.of(correoPredeterminado, telefono), ordenados);
  }

  @Test
  void ordenarMedios_conPersonaSinMedios_deberiaDevolverListaVacia() {
    Persona destinatario = PersonaMother.sinMedios();
    Notificacion notificacion = NotificacionMother.pendiente(destinatario);

    assertEquals(List.of(), notificacion.ordenarMedios(destinatario));
  }

  @Test
  void ordenarMedios_conUnSoloMedio_deberiaDevolverEseMedioSolo() {
    Correo correo = MedioDeContactoMother.correoPredeterminado();
    Persona destinatario = PersonaMother.conMedios(correo);
    Notificacion notificacion = NotificacionMother.pendiente(destinatario);

    assertEquals(List.of(correo), notificacion.ordenarMedios(destinatario));
  }

  @Test
  void ordenarMedios_conNMedios_deberiaMantenerAlPredeterminadoPrimero() {
    Correo correo = MedioDeContactoMother.correo();
    Telefono telefono = MedioDeContactoMother.telefono();
    Telefono whatsappPredeterminado =
        MedioDeContactoMother.telefonoPredeterminado(TipoTelefono.WHATSAPP);
    Persona destinatario = PersonaMother.conMedios(correo, telefono, whatsappPredeterminado);
    Notificacion notificacion = NotificacionMother.pendiente(destinatario);

    List<MedioDeContacto> ordenados = notificacion.ordenarMedios(destinatario);

    assertEquals(3, ordenados.size());
    assertEquals(whatsappPredeterminado, ordenados.get(0));
  }

  // ==================== Casos borde (Fase 0) ====================

  @Test
  void ordenarMedios_conEsPredeterminadoNulo_deberiaTratarloComoNoPredeterminado() {
    // RF-07 (Oleada 9.5): guard agregado en ordenarMedios() — antes explotaba con
    // NullPointerException (Comparator.comparing sobre un Boolean nulo), ahora Boolean.TRUE.equals
    // trata el null como "no predeterminado", igual que ya hacía MedioDeContactoMapper.toEntity()
    // para el mapeo inverso. Ambos medios quedan "no predeterminados" y empatan: por el criterio de
    // desempate explícito (orden de alta, sort estable), correoConNull queda primero.
    Correo correoConNull = MedioDeContactoMother.correoConEsPredeterminadoNulo();
    Correo correoNormal = MedioDeContactoMother.correo();
    Persona destinatario = PersonaMother.conMedios(correoConNull, correoNormal);
    Notificacion notificacion = NotificacionMother.pendiente(destinatario);

    List<MedioDeContacto> ordenados = notificacion.ordenarMedios(destinatario);

    assertEquals(List.of(correoConNull, correoNormal), ordenados);
  }

  @Test
  void ordenarMedios_conDosMediosDelMismoTipoAmbosPredeterminados_noDeberiaExplotar() {
    // Estado inconsistente que la propia ADR de "Medios de Contacto" advertía como posible: dos
    // Telefono (uno ESTANDAR, uno WHATSAPP) marcados predeterminados a la vez. Desempate explícito
    // (RF-07, Oleada 9.5): Stream.sorted() está garantizado estable, así que el orden entre
    // empatados es el orden de alta — comportamiento documentado como criterio deliberado, no
    // accidental.
    Telefono estandarPredeterminado =
        MedioDeContactoMother.telefonoPredeterminado(TipoTelefono.ESTANDAR);
    Telefono whatsappPredeterminado =
        MedioDeContactoMother.telefonoPredeterminado(TipoTelefono.WHATSAPP);
    Persona destinatario = PersonaMother.conMedios(estandarPredeterminado, whatsappPredeterminado);
    Notificacion notificacion = NotificacionMother.pendiente(destinatario);

    List<MedioDeContacto> ordenados = notificacion.ordenarMedios(destinatario);

    assertEquals(List.of(estandarPredeterminado, whatsappPredeterminado), ordenados);
  }
}
