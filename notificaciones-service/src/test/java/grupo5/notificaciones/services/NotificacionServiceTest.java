package grupo5.notificaciones.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import grupo5.notificaciones.dto.NotificacionDTO;
import grupo5.notificaciones.dto.input.EventoDonanteInactivoDTO;
import grupo5.notificaciones.dto.input.EventoEntregaFallidaDTO;
import grupo5.notificaciones.models.entities.notificaciones.EstadoNotificacion;
import grupo5.notificaciones.models.entities.notificaciones.Notificacion;
import grupo5.notificaciones.models.entities.notificaciones.eventos.DonanteInactivo;
import grupo5.notificaciones.models.entities.notificaciones.eventos.EntregaFallida;
import grupo5.notificaciones.models.entities.notificaciones.eventos.EventoNotificable;
import grupo5.notificaciones.models.entities.personas.Correo;
import grupo5.notificaciones.models.entities.personas.Persona;
import grupo5.notificaciones.models.entities.personas.TipoPersona;
import grupo5.notificaciones.models.repositories.INotificacionRepository;
import grupo5.notificaciones.services.events.NotificacionesCreadasEvent;
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
    correo.setEsPredeterminado(true);
    persona.agregarMedioDeContacto(correo);
    return persona;
  }

  @Test
  void procesar_conEventoDeUnDestinatario_deberiaResolverPersonaNotificarYGuardar() {
    Persona donante = personaConCorreoQueSiempreEnvia("Juan");
    EventoDonanteInactivoDTO dto =
        new EventoDonanteInactivoDTO(donante.getId(), TEST_DATE_TIME, 21);
    EventoNotificable evento = new DonanteInactivo(donante, 21, TEST_DATE_TIME);

    when(mapper.toEntity(dto)).thenReturn(evento);

    service.procesar(dto);

    ArgumentCaptor<List<Notificacion>> captor = ArgumentCaptor.forClass(List.class);
    verify(repository, times(1)).saveAll(captor.capture());
    verify(eventPublisher, times(1)).publishEvent(any(NotificacionesCreadasEvent.class));

    assertEquals(1, captor.getValue().size());
    assertEquals(donante, captor.getValue().get(0).getPersona());
  }

  @Test
  void procesar_conEntregaFallida_deberiaGuardarTresNotificacionesYPublicarEvento() {
    Persona donante = personaConCorreoQueSiempreEnvia("Juan");
    Persona beneficiario = personaConCorreoQueSiempreEnvia("ComedorEsperanza");
    Persona admin = personaConCorreoQueSiempreEnvia("Admin");

    EventoEntregaFallidaDTO dto =
        new EventoEntregaFallidaDTO(
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

    ArgumentCaptor<List<Notificacion>> captor = ArgumentCaptor.forClass(List.class);
    verify(repository, times(1)).saveAll(captor.capture());
    verify(eventPublisher, times(1)).publishEvent(any(NotificacionesCreadasEvent.class));

    assertEquals(3, captor.getValue().size());
  }

  @Test
  void obtenerPorPersona_deberiaMeapearEntidadesADTO() {
    Persona persona = new Persona(UUID.randomUUID(), new ArrayList<>(), "Juan", TipoPersona.HUMANA);
    Notificacion notificacion =
        new Notificacion(persona, "Hola, tenés novedades"); // antes: personaId

    when(repository.findByPersonaId(persona.getId())).thenReturn(List.of(notificacion));

    List<NotificacionDTO> resultado = service.obtenerPorPersona(persona.getId());

    assertEquals(1, resultado.size());
    assertEquals(notificacion.getId(), resultado.get(0).id());
    assertEquals("Hola, tenés novedades", resultado.get(0).mensaje());
    assertEquals(EstadoNotificacion.PENDIENTE.name(), resultado.get(0).estado());
  }
}
