package grupo5.notificaciones.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
import grupo5.notificaciones.models.ports.NotificacionSender;
import grupo5.notificaciones.models.repositories.INotificacionRepository;
import grupo5.notificaciones.models.repositories.IPersonaRepository;
import grupo5.notificaciones.services.impl.NotificacionService;
import grupo5.notificaciones.services.mappers.EventoMapper;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificacionServiceTest {

  private static final LocalDateTime TEST_DATE_TIME =
      LocalDateTime.of(2026, Month.JULY, 2, 12, 0, 0);

  @Mock private INotificacionRepository repository;
  @Mock private IPersonaRepository personaRepository;
  @Mock private NotificacionSender sender;
  @Mock private EventoMapper mapper;

  private NotificacionService service;

  @BeforeEach
  void setUp() {
    service = new NotificacionService(repository, personaRepository, sender, mapper);
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
    when(personaRepository.findById(donante.getId())).thenReturn(Optional.of(donante));
    when(sender.enviarA(any(Correo.class), anyString())).thenReturn(true);

    service.procesar(dto);

    ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
    verify(repository, times(1)).save(captor.capture());
    verify(sender, times(1)).enviarA(any(Correo.class), anyString());

    assertEquals(EstadoNotificacion.ENVIADA, captor.getValue().getEstadoNotificacion());
    assertEquals(donante.getId(), captor.getValue().getPersonaId());
  }

  @Test
  void procesar_conEntregaFallida_deberiaNotificarYGuardarATresDestinatarios() {
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
    when(personaRepository.findById(donante.getId())).thenReturn(Optional.of(donante));
    when(personaRepository.findById(beneficiario.getId())).thenReturn(Optional.of(beneficiario));
    when(personaRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
    when(sender.enviarA(any(Correo.class), anyString())).thenReturn(true);

    service.procesar(dto);

    verify(repository, times(3)).save(any(Notificacion.class));
    verify(sender, times(3)).enviarA(any(Correo.class), anyString());
    verify(personaRepository, times(1)).findById(donante.getId());
    verify(personaRepository, times(1)).findById(beneficiario.getId());
    verify(personaRepository, times(1)).findById(admin.getId());
  }

  @Test
  void
      procesar_cuandoDestinatarioNoExisteEnElRepositorio_deberiaGuardarComoFallidaSinLlamarAlSender() {
    Persona donante = personaConCorreoQueSiempreEnvia("Juan");
    EventoDonanteInactivoDTO dto =
        new EventoDonanteInactivoDTO(donante.getId(), TEST_DATE_TIME, 21);
    EventoNotificable evento = new DonanteInactivo(donante, 21, TEST_DATE_TIME);

    when(mapper.toEntity(dto)).thenReturn(evento);
    // La persona fue borrada/no está sincronizada: no se encuentra en el repositorio local.
    when(personaRepository.findById(donante.getId())).thenReturn(Optional.empty());

    service.procesar(dto);

    ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
    verify(repository, times(1)).save(captor.capture());
    verify(sender, never()).enviarA(any(Correo.class), anyString());

    assertEquals(EstadoNotificacion.FALLIDA, captor.getValue().getEstadoNotificacion());
  }

  @Test
  void obtenerPorPersona_deberiaMapearEntidadesADTO() {
    UUID personaId = UUID.randomUUID();
    Notificacion notificacion = new Notificacion(personaId, "Hola, tenés novedades");

    when(repository.findByPersonaId(personaId)).thenReturn(List.of(notificacion));

    List<NotificacionDTO> resultado = service.obtenerPorPersona(personaId);

    assertEquals(1, resultado.size());
    assertEquals(notificacion.getId(), resultado.get(0).id());
    assertEquals("Hola, tenés novedades", resultado.get(0).mensaje());
    assertEquals(EstadoNotificacion.PENDIENTE.name(), resultado.get(0).estado());
  }
}
