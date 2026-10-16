package grupo5.notificaciones.infrastructure.amqp;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import grupo5.notificaciones.controllers.NotificacionController;
import grupo5.notificaciones.dto.ConteoNoLeidasDTO;
import grupo5.notificaciones.dto.NotificacionDTO;
import grupo5.notificaciones.dto.input.DestinoEventoDTO;
import grupo5.notificaciones.dto.input.EventoDonacionAsignadaV1;
import grupo5.notificaciones.dto.input.EventoDonacionRecibidaV1;
import grupo5.notificaciones.dto.input.EventoIncentivoMisionCumplidaV1;
import grupo5.notificaciones.dto.input.EventoIncentivoSubioCategoriaV1;
import grupo5.notificaciones.models.entities.personas.Persona;
import grupo5.notificaciones.models.entities.personas.TipoPersona;
import grupo5.notificaciones.models.repositories.impl.NotificacionRepositoryEnMemoria;
import grupo5.notificaciones.models.repositories.impl.PersonaRepositoryEnMemoria;
import grupo5.notificaciones.services.impl.NotificacionService;
import grupo5.notificaciones.services.mappers.EventoMapper;
import grupo5.notificaciones.services.mappers.MedioDeContactoMapper;
import grupo5.notificaciones.services.mappers.PersonaMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;

class FlujoEventosNotificacionesTest {

  private PersonaRepositoryEnMemoria personaRepository;
  private NotificacionRepositoryEnMemoria notificacionRepository;
  private NotificacionService notificacionService;
  private DonacionEventListener donacionListener;
  private IncentivoEventListener incentivoListener;
  private NotificacionController notificacionController;

  private Persona donante;
  private Persona beneficiario;
  private final LocalDateTime fechaBase = LocalDateTime.of(2026, 10, 16, 10, 0);

  @BeforeEach
  void setUp() {
    personaRepository = new PersonaRepositoryEnMemoria();
    notificacionRepository = new NotificacionRepositoryEnMemoria();

    donante =
        new Persona(UUID.randomUUID(), new ArrayList<>(), "Carlos Donante", TipoPersona.HUMANA);
    beneficiario =
        new Persona(UUID.randomUUID(), new ArrayList<>(), "Comedor Sol", TipoPersona.JURIDICA);

    personaRepository.save(donante);
    personaRepository.save(beneficiario);

    EventoMapper eventoMapper = new EventoMapper(personaRepository);
    PersonaMapper personaMapper = new PersonaMapper(new MedioDeContactoMapper());
    ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

    notificacionService =
        new NotificacionService(notificacionRepository, eventoMapper, eventPublisher, null, null);

    donacionListener = new DonacionEventListener(notificacionService, personaMapper);
    incentivoListener = new IncentivoEventListener(notificacionService);
    notificacionController = new NotificacionController(notificacionService);
  }

  @Test
  @DisplayName(
      "Flujo integral: eventos AMQP de donaciones e incentivos hasta consulta y marcado en API REST")
  void flujoCompletoDeEventosParaDonanteYBeneficiario() {
    DestinoEventoDTO destino =
        new DestinoEventoDTO(
            "Av. Corrientes", 1234, 3, "B", "C1043", "San Nicolás", "CABA", "Argentina");

    // 1. Asignación de Donación (notifica a Donante y Beneficiario)
    EventoDonacionAsignadaV1 eventoAsignada =
        new EventoDonacionAsignadaV1(
            UUID.randomUUID(),
            donante.getId(),
            donante.getId(),
            fechaBase,
            beneficiario.getId(),
            "Ropa de abrigo",
            destino,
            12.5,
            0.25);
    donacionListener.onDonacionAsignada(eventoAsignada, UUID.randomUUID().toString());

    // 2. Misión Cumplida de Incentivos (notifica a Donante)
    EventoIncentivoMisionCumplidaV1 eventoMision =
        new EventoIncentivoMisionCumplidaV1(
            donante.getId(), fechaBase.plusHours(1), "Abrigo Solidario", "50 puntos");
    incentivoListener.onMisionCumplida(eventoMision, UUID.randomUUID().toString());

    // 3. Ascenso de Categoría de Incentivos (notifica a Donante)
    EventoIncentivoSubioCategoriaV1 eventoCategoria =
        new EventoIncentivoSubioCategoriaV1(
            donante.getId(), fechaBase.plusHours(2), "Oro", "Plata");
    incentivoListener.onSubioCategoria(eventoCategoria, UUID.randomUUID().toString());

    // 4. Recepción de Donación con comprobante (notifica a Donante y Beneficiario)
    EventoDonacionRecibidaV1 eventoRecibida =
        new EventoDonacionRecibidaV1(
            donante.getId(),
            donante.getId(),
            fechaBase.plusHours(3),
            beneficiario.getId(),
            "Ropa de abrigo",
            "AF-123-ZZ");
    donacionListener.onDonacionRecibida(eventoRecibida, UUID.randomUUID().toString());

    // --- VERIFICACIÓN DONANTE ---
    // El donante debe tener 4 notificaciones pendientes y no leídas
    ResponseEntity<ConteoNoLeidasDTO> conteoDonante =
        notificacionController.contarNoLeidas(donante.getId());
    assertNotNull(conteoDonante.getBody());
    assertEquals(4, conteoDonante.getBody().noLeidas());

    ResponseEntity<List<NotificacionDTO>> notificacionesDonante =
        notificacionController.obtenerPorPersona(donante.getId(), false);
    List<NotificacionDTO> listaDonante = notificacionesDonante.getBody();
    assertNotNull(listaDonante);
    assertEquals(4, listaDonante.size());
    assertTrue(listaDonante.stream().allMatch(n -> !n.leida()));

    // Verificar que los mensajes corresponden a los eventos
    assertTrue(
        listaDonante.stream().anyMatch(n -> n.mensaje().contains("fue asignada a Comedor Sol")));
    assertTrue(
        listaDonante.stream()
            .anyMatch(n -> n.mensaje().contains("Completaste la misión Abrigo Solidario")));
    assertTrue(
        listaDonante.stream()
            .anyMatch(n -> n.mensaje().contains("Has ascendido de la categoría Plata a Oro")));
    assertTrue(
        listaDonante.stream()
            .anyMatch(
                n ->
                    n.mensaje().contains("recibió tu donación")
                        && n.mensaje().contains("AF-123-ZZ")));

    // Marcar una como leída
    UUID primeraNotifId = listaDonante.get(0).id();
    ResponseEntity<NotificacionDTO> leidaResp =
        notificacionController.marcarLeida(primeraNotifId, true);
    assertNotNull(leidaResp.getBody());
    assertTrue(leidaResp.getBody().leida());

    // El conteo no leídas baja a 3
    assertEquals(3, notificacionController.contarNoLeidas(donante.getId()).getBody().noLeidas());

    // Marcar todas como leídas
    notificacionController.marcarTodasComoLeidas(donante.getId());
    assertEquals(0, notificacionController.contarNoLeidas(donante.getId()).getBody().noLeidas());
    assertEquals(
        0, notificacionController.obtenerPorPersona(donante.getId(), false).getBody().size());
    assertEquals(
        4, notificacionController.obtenerPorPersona(donante.getId(), true).getBody().size());

    // --- VERIFICACIÓN ENTIDAD BENEFICIARIA ---
    // La entidad beneficiaria debe tener exactamente 2 notificaciones (Asignación y Recepción)
    ResponseEntity<ConteoNoLeidasDTO> conteoBeneficiario =
        notificacionController.contarNoLeidas(beneficiario.getId());
    assertNotNull(conteoBeneficiario.getBody());
    assertEquals(2, conteoBeneficiario.getBody().noLeidas());

    ResponseEntity<List<NotificacionDTO>> notificacionesBeneficiario =
        notificacionController.obtenerPorPersona(beneficiario.getId(), false);
    List<NotificacionDTO> listaBeneficiario = notificacionesBeneficiario.getBody();
    assertNotNull(listaBeneficiario);
    assertEquals(2, listaBeneficiario.size());
    assertTrue(listaBeneficiario.stream().allMatch(n -> !n.leida()));

    assertTrue(
        listaBeneficiario.stream()
            .anyMatch(
                n ->
                    n.mensaje()
                        .contains("Se te ha asignado la donación del donante Carlos Donante")));
    assertTrue(
        listaBeneficiario.stream()
            .anyMatch(
                n ->
                    n.mensaje().contains("Ya llegó la donación")
                        && n.mensaje().contains("AF-123-ZZ")));
  }
}
