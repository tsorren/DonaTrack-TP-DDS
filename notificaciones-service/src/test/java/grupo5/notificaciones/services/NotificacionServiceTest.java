package grupo5.notificaciones.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
import grupo5.notificaciones.models.repositories.INotificacionRepository;
import grupo5.notificaciones.mothers.NotificacionMother;
import grupo5.notificaciones.services.impl.NotificacionService;
import grupo5.notificaciones.services.mappers.EventoMapper;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
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
            true);

    EventoNotificable evento =
        new EntregaFallida(
            donante, beneficiario, admin, "ropa", "Nadie respondió", true, TEST_DATE_TIME);

    when(mapper.toEntity(dto)).thenReturn(evento);

    service.procesar(dto);

    assertEquals(3, notificacionesGuardadas(3).size());
  }

  @Test
  void obtenerPorPersona_deberiaMeapearEntidadesADTO() {
    Persona persona = new Persona(UUID.randomUUID(), new ArrayList<>(), "Juan", TipoPersona.HUMANA);
    Notificacion notificacion = NotificacionMother.pendiente(persona, "Hola, tenés novedades");

    when(repository.findByPersonaId(persona.getId())).thenReturn(List.of(notificacion));

    List<NotificacionDTO> resultado = service.obtenerPorPersona(persona.getId());

    assertEquals(1, resultado.size());
    assertEquals(notificacion.getId(), resultado.get(0).id());
    assertEquals("Hola, tenés novedades", resultado.get(0).mensaje());
    assertEquals(EstadoNotificacion.PENDIENTE.name(), resultado.get(0).estado());
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
}
