package grupo5.notificaciones.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.notificaciones.dto.FiltroBandeja;
import grupo5.notificaciones.dto.NotificacionDTO;
import grupo5.notificaciones.dto.input.DestinoEventoDTO;
import grupo5.notificaciones.dto.input.EventoDonacionAsignadaV1;
import grupo5.notificaciones.dto.input.EventoDonanteInactivoDTO;
import grupo5.notificaciones.dto.input.EventoEntregaFallidaDTO;
import grupo5.notificaciones.models.entities.notificaciones.EstadoNotificacion;
import grupo5.notificaciones.models.entities.notificaciones.Notificacion;
import grupo5.notificaciones.models.entities.notificaciones.eventos.DonacionAsignada;
import grupo5.notificaciones.models.entities.notificaciones.eventos.DonanteInactivo;
import grupo5.notificaciones.models.entities.notificaciones.eventos.EntregaFallida;
import grupo5.notificaciones.models.entities.notificaciones.eventos.EventoNotificable;
import grupo5.notificaciones.models.entities.notificaciones.events.NotificacionCreada;
import grupo5.notificaciones.models.entities.personas.Correo;
import grupo5.notificaciones.models.entities.personas.Persona;
import grupo5.notificaciones.models.entities.personas.TipoPersona;
import grupo5.notificaciones.models.repositories.ConsultaBandeja;
import grupo5.notificaciones.models.repositories.INotificacionRepository;
import grupo5.notificaciones.models.repositories.PaginaBandeja;
import grupo5.notificaciones.mothers.NotificacionMother;
import grupo5.notificaciones.services.impl.NotificacionService;
import grupo5.notificaciones.services.mappers.EventoMapper;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class NotificacionServiceTest {

  private static final LocalDateTime TEST_DATE_TIME =
      LocalDateTime.of(2026, Month.JULY, 2, 12, 0, 0);

  @Mock private INotificacionRepository repository;
  @Mock private EventoMapper mapper;
  @Mock private ApplicationEventPublisher eventPublisher;

  private NotificacionService service;

  @BeforeEach
  void setUp() {
    service = new NotificacionService(repository, mapper, eventPublisher);
  }

  private Persona personaConCorreoQueSiempreEnvia(String denominacion) {
    Persona persona =
        new Persona(UUID.randomUUID(), new ArrayList<>(), denominacion, TipoPersona.HUMANA);
    Correo correo = new Correo();
    correo.setDireccionCorreo(denominacion.toLowerCase() + "@test.com");
    correo.marcarComoPredeterminado();
    persona.agregarMedioDeContacto(correo);
    return persona;
  }

  /**
   * Verifica el guardado único en el repositorio y la cantidad de NotificacionCreada publicadas, y
   * devuelve las notificaciones capturadas. Es el cierre común de los casos de procesar(...).
   */
  private List<Notificacion> notificacionesGuardadas(int eventosPublicadosEsperados) {
    ArgumentCaptor<List<Notificacion>> captor = ArgumentCaptor.forClass(List.class);
    verify(repository, times(1)).saveAll(captor.capture());
    verify(eventPublisher, times(eventosPublicadosEsperados))
        .publishEvent(any(NotificacionCreada.class));
    return captor.getValue();
  }

  @Test
  void procesar_conEventoDeUnDestinatario_deberiaResolverPersonaNotificarYGuardar() {
    Persona donante = personaConCorreoQueSiempreEnvia("Juan");
    EventoDonanteInactivoDTO dto =
        new EventoDonanteInactivoDTO(UUID.randomUUID(), donante.getId(), TEST_DATE_TIME, 21);
    EventoNotificable evento = new DonanteInactivo(donante, 21, TEST_DATE_TIME);

    when(mapper.toEntity(dto)).thenReturn(evento);

    service.procesar(dto);

    List<Notificacion> guardadas = notificacionesGuardadas(1);
    assertEquals(1, guardadas.size());
    assertEquals(donante.getId(), guardadas.get(0).getPersonaId());
  }

  @Test
  void procesar_conEntregaFallida_deberiaGuardarTresNotificacionesYPublicarEvento() {
    Persona donante = personaConCorreoQueSiempreEnvia("Juan");
    Persona beneficiario = personaConCorreoQueSiempreEnvia("ComedorEsperanza");
    Persona admin = personaConCorreoQueSiempreEnvia("Admin");

    EventoEntregaFallidaDTO dto =
        new EventoEntregaFallidaDTO(
            UUID.randomUUID(),
            donante.getId(),
            TEST_DATE_TIME,
            beneficiario.getId(),
            "ropa",
            admin.getId(),
            "Nadie respondió",
            true,
            null);

    EventoNotificable evento =
        new EntregaFallida(
            donante, beneficiario, admin, "ropa", "Nadie respondió", true, TEST_DATE_TIME);

    when(mapper.toEntity(dto)).thenReturn(evento);

    service.procesar(dto);

    assertEquals(3, notificacionesGuardadas(3).size());
  }

  @Test
  void bandeja_mapeaLaPaginaADTOConSuTotalYSinParametrosPideLaPrimeraDe20() {
    Persona persona = new Persona(UUID.randomUUID(), new ArrayList<>(), "Juan", TipoPersona.HUMANA);
    UUID donacionId = UUID.randomUUID();
    Notificacion conTipo = NotificacionMother.pendiente(persona, "Salió");
    conTipo.etiquetar("DONACION_EN_CAMINO", donacionId);
    Notificacion anterior = NotificacionMother.pendiente(persona, "Vieja");
    ArgumentCaptor<ConsultaBandeja> consulta = ArgumentCaptor.forClass(ConsultaBandeja.class);
    when(repository.bandeja(eq(persona.getId()), consulta.capture()))
        .thenReturn(new PaginaBandeja(List.of(conTipo, anterior), 45));

    NotificacionService.PaginaNotificaciones pagina =
        service.bandeja(persona.getId(), FiltroBandeja.deLectura(null));

    assertEquals(45, pagina.total());
    assertEquals(conTipo.getId(), pagina.notificaciones().get(0).id());
    assertEquals(EstadoNotificacion.PENDIENTE.name(), pagina.notificaciones().get(0).estado());
    assertEquals("DONACION_EN_CAMINO", pagina.notificaciones().get(0).tipoEvento());
    assertEquals(donacionId, pagina.notificaciones().get(0).referenciaId());
    assertNull(pagina.notificaciones().get(1).tipoEvento());
    assertEquals(
        new ConsultaBandeja(null, null, Set.of(), Set.of(), false, 0, 20), consulta.getValue());
  }

  @Test
  void bandeja_normalizaLaBusquedaYTraduceTiposOrdenYPagina() {
    UUID personaId = UUID.randomUUID();
    ArgumentCaptor<ConsultaBandeja> consulta = ArgumentCaptor.forClass(ConsultaBandeja.class);
    when(repository.bandeja(eq(personaId), consulta.capture()))
        .thenReturn(new PaginaBandeja(List.of(), 0));

    service.bandeja(
        personaId,
        new FiltroBandeja(
            false,
            "  CAMIÓN ",
            List.of("donacion_en_camino"),
            List.of("DONACION_ASIGNADA", "ENTREGA_FALLIDA"),
            "antiguas",
            2,
            50));

    assertEquals(
        new ConsultaBandeja(
            false,
            "camion",
            Set.of("DONACION_EN_CAMINO"),
            Set.of("DONACION_ASIGNADA", "ENTREGA_FALLIDA"),
            true,
            2,
            50),
        consulta.getValue());
  }

  @Test
  void bandeja_conUnParametroFueraDeRango_daErrorDeArgumentoSinLlamarAlRepositorio() {
    UUID personaId = UUID.randomUUID();
    List<FiltroBandeja> invalidos =
        List.of(
            new FiltroBandeja(null, null, null, null, null, -1, null),
            new FiltroBandeja(null, null, null, null, null, null, 0),
            new FiltroBandeja(null, null, null, null, null, null, 101),
            new FiltroBandeja(null, "x".repeat(101), null, null, null, null, null),
            new FiltroBandeja(null, null, null, null, "otro", null, null),
            new FiltroBandeja(null, null, List.of("NO_EXISTE"), null, null, null, null),
            new FiltroBandeja(null, null, null, List.of("NO_EXISTE"), null, null, null));

    for (FiltroBandeja filtro : invalidos) {
      ValidationException error =
          assertThrows(ValidationException.class, () -> service.bandeja(personaId, filtro));
      assertEquals(ErrorCatalog.ARGUMENTO_INVALIDO.getCode(), error.getErrorCode());
    }
    verify(repository, never()).bandeja(any(), any());
  }

  @Test
  void bandeja_conBusquedaVaciaOSoloEspacios_noFiltraPorTexto() {
    UUID personaId = UUID.randomUUID();
    ArgumentCaptor<ConsultaBandeja> consulta = ArgumentCaptor.forClass(ConsultaBandeja.class);
    when(repository.bandeja(eq(personaId), consulta.capture()))
        .thenReturn(new PaginaBandeja(List.of(), 0));

    service.bandeja(personaId, new FiltroBandeja(null, "   ", null, null, "RECIENTES", null, null));

    assertNull(consulta.getValue().texto());
    assertFalse(consulta.getValue().antiguas());
  }

  @Test
  void procesar_conEventoDonacionAsignadaV1_deberiaPersistirNotificacionesYPublicarEventos() {
    Persona donante = personaConCorreoQueSiempreEnvia("Juan");
    Persona beneficiario = personaConCorreoQueSiempreEnvia("ComedorEsperanza");

    DestinoEventoDTO destino =
        new DestinoEventoDTO(
            "Av. Corrientes", 1234, null, null, "C1043", "San Nicolás", "CABA", "Argentina");
    EventoDonacionAsignadaV1 eventoV1 =
        new EventoDonacionAsignadaV1(
            UUID.randomUUID(),
            donante.getId(),
            donante.getId(),
            TEST_DATE_TIME,
            beneficiario.getId(),
            "10kg de arroz",
            destino,
            10.0,
            0.2);

    DonacionAsignada entidad =
        new DonacionAsignada(donante, beneficiario, "10kg de arroz", TEST_DATE_TIME);
    when(mapper.toEntity(eventoV1)).thenReturn(entidad);

    service.procesar(eventoV1, UUID.randomUUID().toString());

    assertEquals(2, notificacionesGuardadas(2).size());
  }

  @Test
  void procesar_conEventoDonacionAsignadaV1_duplicadoEnInbox_noDeberiaProcesarNiGuardar() {
    JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    NotificacionService serviceConDb =
        new NotificacionService(repository, mapper, eventPublisher, jdbcTemplate);

    EventoDonacionAsignadaV1 eventoV1 =
        new EventoDonacionAsignadaV1(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            TEST_DATE_TIME,
            UUID.randomUUID(),
            "Ropa",
            new DestinoEventoDTO("Calle", 100, null, null, "1000", "Loc", "Prov", "Arg"),
            5.0,
            0.1);

    when(jdbcTemplate.update(anyString(), any(UUID.class), any(LocalDateTime.class))).thenReturn(0);

    serviceConDb.procesar(eventoV1, UUID.randomUUID().toString());

    verify(repository, never()).saveAll(any());
    verify(eventPublisher, never()).publishEvent(any());
    verify(mapper, never()).toEntity(eventoV1);
  }

  @Test
  void procesarPersonaSincronizada_conDtoValido_deberiaDelegarEnPersonasService() {
    grupo5.notificaciones.services.IPersonasService personasService =
        mock(grupo5.notificaciones.services.IPersonasService.class);
    NotificacionService serviceConPersonas =
        new NotificacionService(repository, mapper, eventPublisher, null, personasService);

    grupo5.notificaciones.dto.PersonaReplicaDTO dto =
        new grupo5.notificaciones.dto.PersonaReplicaDTO(
            UUID.randomUUID(),
            "Fundación Ayuda",
            grupo5.notificaciones.models.entities.personas.TipoPersona.JURIDICA,
            List.of());

    serviceConPersonas.procesarPersonaSincronizada(dto, UUID.randomUUID().toString());

    verify(personasService, times(1)).sincronizar(dto);
  }

  @Test
  void procesarPersonaSincronizada_duplicadoEnInbox_noDeberiaSincronizar() {
    JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    grupo5.notificaciones.services.IPersonasService personasService =
        mock(grupo5.notificaciones.services.IPersonasService.class);
    NotificacionService serviceConDb =
        new NotificacionService(repository, mapper, eventPublisher, jdbcTemplate, personasService);

    grupo5.notificaciones.dto.PersonaReplicaDTO dto =
        new grupo5.notificaciones.dto.PersonaReplicaDTO(
            UUID.randomUUID(),
            "Fundación Ayuda",
            grupo5.notificaciones.models.entities.personas.TipoPersona.JURIDICA,
            List.of());

    when(jdbcTemplate.update(anyString(), any(UUID.class), any(LocalDateTime.class))).thenReturn(0);

    serviceConDb.procesarPersonaSincronizada(dto, UUID.randomUUID().toString());

    verify(personasService, never()).sincronizar(any());
  }

  // ==================== Dueño de la notificación (pertenencia) ====================

  @Test
  void personaDe_cuandoExiste_deberiaDevolverLaPersonaDestinataria() {
    UUID notifId = UUID.randomUUID();
    Persona destinataria = personaConCorreoQueSiempreEnvia("Juan");
    when(repository.findById(notifId))
        .thenReturn(Optional.of(NotificacionMother.pendiente(destinataria)));

    assertEquals(Optional.of(destinataria.getId()), service.personaDe(notifId));
  }

  @Test
  void personaDe_cuandoNoExiste_deberiaDevolverVacio() {
    UUID notifId = UUID.randomUUID();
    when(repository.findById(notifId)).thenReturn(Optional.empty());

    assertTrue(service.personaDe(notifId).isEmpty());
  }

  // ==================== Ciclo de Lectura In-App ====================

  @Test
  void marcarComoLeida_cuandoExiste_deberiaMarcarGuardarYRetornarDTO() {
    UUID notifId = UUID.randomUUID();
    Persona personaDummy = personaConCorreoQueSiempreEnvia("Juan");
    Notificacion notif = NotificacionMother.pendiente(personaDummy);
    when(repository.findById(notifId)).thenReturn(Optional.of(notif));
    when(repository.save(any(Notificacion.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Optional<NotificacionDTO> resultado = service.marcarComoLeida(notifId);

    assertTrue(resultado.isPresent());
    assertTrue(resultado.get().leida());
    assertNotNull(resultado.get().fechaLectura());
    verify(repository).save(notif);
  }

  @Test
  void marcarComoNoLeida_cuandoExiste_deberiaRestablecerYRetornarDTO() {
    UUID notifId = UUID.randomUUID();
    Persona personaDummy = personaConCorreoQueSiempreEnvia("Juan");
    Notificacion notif = NotificacionMother.pendiente(personaDummy);
    notif.marcarComoLeida();
    when(repository.findById(notifId)).thenReturn(Optional.of(notif));
    when(repository.save(any(Notificacion.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Optional<NotificacionDTO> resultado = service.marcarComoNoLeida(notifId);

    assertTrue(resultado.isPresent());
    assertFalse(resultado.get().leida());
    assertNull(resultado.get().fechaLectura());
    verify(repository).save(notif);
  }

  @Test
  void marcarTodasComoLeidas_deberiaDelegarEnRepositorio() {
    UUID personaId = UUID.randomUUID();

    service.marcarTodasComoLeidas(personaId);

    verify(repository, times(1)).marcarTodasComoLeidas(personaId);
  }

  @Test
  void contarNoLeidas_deberiaDelegarEnRepositorio() {
    UUID personaId = UUID.randomUUID();
    when(repository.countByPersonaIdAndLeidaFalse(personaId)).thenReturn(5L);

    long conteo = service.contarNoLeidas(personaId);

    assertEquals(5L, conteo);
    verify(repository, times(1)).countByPersonaIdAndLeidaFalse(personaId);
  }

  private static EventoDonacionAsignadaV1 donacionAsignada() {
    return new EventoDonacionAsignadaV1(
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        TEST_DATE_TIME,
        UUID.randomUUID(),
        "Ropa",
        new DestinoEventoDTO("Calle", 100, null, null, "1000", "Loc", "Prov", "Arg"),
        5.0,
        0.1);
  }

  @Test
  void procesar_conMessageId_usaElMessageIdComoClaveDelInbox() {
    JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    NotificacionService serviceConDb =
        new NotificacionService(repository, mapper, eventPublisher, jdbcTemplate);
    EventoDonacionAsignadaV1 evento = donacionAsignada();
    DonacionAsignada entidad = mock(DonacionAsignada.class);
    when(mapper.toEntity(evento)).thenReturn(entidad);
    when(jdbcTemplate.update(anyString(), any(UUID.class), any(LocalDateTime.class))).thenReturn(1);
    UUID messageId = UUID.randomUUID();

    serviceConDb.procesar(evento, messageId.toString());

    verify(jdbcTemplate).update(anyString(), eq(messageId), any(LocalDateTime.class));
    verify(repository).saveAll(any());
  }

  @Test
  void procesar_sinMessageId_procesaSinRegistrarEnInbox() {
    JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    NotificacionService serviceConDb =
        new NotificacionService(repository, mapper, eventPublisher, jdbcTemplate);
    EventoDonacionAsignadaV1 evento = donacionAsignada();
    DonacionAsignada entidad = mock(DonacionAsignada.class);
    when(mapper.toEntity(evento)).thenReturn(entidad);

    serviceConDb.procesar(evento, null);

    verifyNoInteractions(jdbcTemplate);
    verify(repository).saveAll(any());
  }

  @Test
  void procesar_eventoLegacySinEventId_procesaSinRegistrarEnInbox() {
    JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    NotificacionService serviceConDb =
        new NotificacionService(repository, mapper, eventPublisher, jdbcTemplate);
    EventoDonanteInactivoDTO dto =
        new EventoDonanteInactivoDTO(null, UUID.randomUUID(), TEST_DATE_TIME, 21);
    EventoNotificable entidad = mock(EventoNotificable.class);
    when(mapper.toEntity(dto)).thenReturn(entidad);

    serviceConDb.procesar(dto);

    verifyNoInteractions(jdbcTemplate);
    verify(repository).saveAll(any());
  }

  @Test
  void notificarPersona_guardaUnaNotificacionPendienteYPublicaNotificacionCreada() {
    UUID adminId = UUID.fromString("00000000-0000-0000-0000-000000000001");

    service.notificarPersona(adminId, "Nuevo mensaje de contacto Nº 7, sobre una entrega.");

    ArgumentCaptor<Notificacion> guardada = ArgumentCaptor.forClass(Notificacion.class);
    verify(repository).save(guardada.capture());
    assertEquals(adminId, guardada.getValue().getPersonaId());
    assertEquals(EstadoNotificacion.PENDIENTE, guardada.getValue().getEstadoNotificacion());
    verify(eventPublisher).publishEvent(any(NotificacionCreada.class));
  }
}
