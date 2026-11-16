package grupo5.donaciones.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import grupo5.donaciones.dto.comunicaciones.EventoDonacionEnCaminoV1;
import grupo5.donaciones.dto.comunicaciones.EventoDonacionEntregaFallidaV1;
import grupo5.donaciones.dto.comunicaciones.EventoDonacionRecibidaV1;
import grupo5.donaciones.dto.comunicaciones.EventoDonacionVencidaV1;
import grupo5.donaciones.infrastructure.outbox.OutboxStore;
import grupo5.donaciones.models.entities.beneficiarios.EntidadBeneficiaria;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.events.EventoDonacionFallida;
import grupo5.donaciones.models.entities.donacionesIndependientes.events.EventoDonacionRecibida;
import grupo5.donaciones.models.entities.donacionesIndependientes.events.EventoDonacionVencida;
import grupo5.donaciones.models.entities.donacionesIndependientes.events.EventoRutaIniciada;
import grupo5.donaciones.models.entities.donantes.Donante;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository;
import grupo5.donaciones.models.repositories.IDonacionesRepository;
import grupo5.donaciones.models.repositories.IDonantesRepository;
import grupo5.donaciones.models.repositories.IEntidadesBeneficiariasRepository;
import grupo5.donaciones.models.repositories.INecesidadesRepository;
import grupo5.donaciones.services.impl.DonacionesIndependientesNotificacionesService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DonacionesIndependientesNotificacionesServiceTest {

  @Mock private IDonacionesEventPublisher eventPublisher;
  @Mock private IDonacionesRepository donacionRepository;
  @Mock private IDonantesRepository donantesRepository;
  @Mock private IEntidadesBeneficiariasRepository entidadesBeneficiariasRepository;
  @Mock private INecesidadesRepository necesidadRepository;
  @Mock private IDonacionesIndependientesRepository donacionesIndependientesRepository;
  @Mock private IPersonasService personasService;
  @Mock private OutboxStore outboxStore;

  @InjectMocks private DonacionesIndependientesNotificacionesService notificacionesService;

  private UUID donacionIndependienteId;
  private UUID donacionOriginalId;
  private UUID necesidadId;
  private UUID donanteId;
  private UUID personaDonanteId;
  private UUID entidadId;
  private UUID personaBeneficiariaId;
  private UUID personaAdminId;

  @BeforeEach
  void setUp() {
    donacionIndependienteId = UUID.randomUUID();
    donacionOriginalId = UUID.randomUUID();
    necesidadId = UUID.randomUUID();
    donanteId = UUID.randomUUID();
    personaDonanteId = UUID.randomUUID();
    entidadId = UUID.randomUUID();
    personaBeneficiariaId = UUID.randomUUID();
    personaAdminId = UUID.randomUUID();

    Donacion donacion = mock(Donacion.class);
    when(donacion.getDonanteId()).thenReturn(donanteId);
    when(donacionRepository.findById(donacionOriginalId)).thenReturn(Optional.of(donacion));

    Donante donante = mock(Donante.class);
    when(donante.personaId()).thenReturn(personaDonanteId);
    when(donantesRepository.findById(donanteId)).thenReturn(Optional.of(donante));

    // El evento de vencida no lleva a la entidad: esos dos datos no se piden en ese caso.
    Necesidad necesidad = mock(Necesidad.class);
    lenient().when(necesidad.getEntidadId()).thenReturn(entidadId);
    lenient().when(necesidadRepository.findById(necesidadId)).thenReturn(Optional.of(necesidad));

    EntidadBeneficiaria entidad = mock(EntidadBeneficiaria.class);
    lenient().when(entidad.juridicaId()).thenReturn(personaBeneficiariaId);
    lenient()
        .when(entidadesBeneficiariasRepository.findById(entidadId))
        .thenReturn(Optional.of(entidad));

    DonacionIndependiente donacionIndependiente = mock(DonacionIndependiente.class);
    when(donacionIndependiente.getDescripcion()).thenReturn("Abrigo de lana");
    when(donacionesIndependientesRepository.findById(donacionIndependienteId))
        .thenReturn(Optional.of(donacionIndependiente));
  }

  @Test
  void procesarRutaIniciada_deberiaPublicarEvento() {
    EventoRutaIniciada event =
        new EventoRutaIniciada(
            donacionIndependienteId, donacionOriginalId, necesidadId, "http://mapa/ruta");

    notificacionesService.procesarRutaIniciada(event);

    var captor = ArgumentCaptor.forClass(EventoDonacionEnCaminoV1.class);
    verify(eventPublisher, times(1)).publicarDonacionEnCamino(captor.capture());
    assertEquals(donacionIndependienteId, captor.getValue().donacionIndependienteId());
  }

  @Test
  void procesarDonacionRecibida_deberiaPublicarUnSoloEvento() {
    EventoDonacionRecibida event =
        new EventoDonacionRecibida(
            donacionIndependienteId, donacionOriginalId, necesidadId, "ABC-123");

    notificacionesService.procesarDonacionRecibida(event);

    var captor = ArgumentCaptor.forClass(EventoDonacionRecibidaV1.class);
    verify(eventPublisher, times(1)).publicarDonacionRecibida(captor.capture());
    assertEquals(donacionIndependienteId, captor.getValue().donacionIndependienteId());
  }

  @Test
  void procesarDonacionFallida_deberiaPublicarEventoConAdmin() {
    when(personasService.obtenerIdPersonaAdministradora()).thenReturn(personaAdminId);

    EventoDonacionFallida event =
        new EventoDonacionFallida(
            donacionIndependienteId, donacionOriginalId, necesidadId, "Dirección no existe", false);

    notificacionesService.procesarDonacionFallida(event);

    var captor = ArgumentCaptor.forClass(EventoDonacionEntregaFallidaV1.class);
    verify(eventPublisher, times(1)).publicarDonacionEntregaFallida(captor.capture());
    assertEquals(donacionIndependienteId, captor.getValue().donacionIndependienteId());
  }

  @Test
  void procesarDonacionVencida_deberiaPublicarEventoConElIdDeLaDonacion() {
    when(personasService.obtenerIdPersonaAdministradora()).thenReturn(personaAdminId);

    notificacionesService.procesarDonacionVencida(
        new EventoDonacionVencida(donacionIndependienteId, donacionOriginalId, "Venció"));

    var captor = ArgumentCaptor.forClass(EventoDonacionVencidaV1.class);
    verify(eventPublisher, times(1)).publicarDonacionVencida(captor.capture());
    assertEquals(donacionIndependienteId, captor.getValue().donacionIndependienteId());
  }

  @Test
  void procesarRutaIniciada_cuandoPublicacionFalla_encolarEnOutbox() {
    EventoRutaIniciada event =
        new EventoRutaIniciada(
            donacionIndependienteId, donacionOriginalId, necesidadId, "http://mapa/ruta");
    doThrow(new RuntimeException("rabbitmq no disponible"))
        .when(eventPublisher)
        .publicarDonacionEnCamino(any());

    notificacionesService.procesarRutaIniciada(event);

    verify(outboxStore, times(1)).agregar(any());
  }

  @Test
  void procesarDonacionRecibida_cuandoPublicacionFalla_encolarEnOutbox() {
    EventoDonacionRecibida event =
        new EventoDonacionRecibida(
            donacionIndependienteId, donacionOriginalId, necesidadId, "ABC-123");
    doThrow(new RuntimeException("rabbitmq no disponible"))
        .when(eventPublisher)
        .publicarDonacionRecibida(any());

    notificacionesService.procesarDonacionRecibida(event);

    verify(outboxStore, times(1)).agregar(any());
  }
}
