package grupo5.logistica.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import grupo5.logistica.dto.callback.SolicitudPlanificacionResponseDTO;
import grupo5.logistica.infrastructure.LogisticaEventPublisher;
import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.solicitudes.SolicitudPlanificacion;
import grupo5.logistica.models.repositories.ICamionRepository;
import grupo5.logistica.models.repositories.IEntregasRepository;
import grupo5.logistica.models.repositories.IRutasRepository;
import grupo5.logistica.models.repositories.ISolicitudPlanificacionRepository;
import grupo5.logistica.services.IServicioExternoPlanificacion;
import grupo5.logistica.services.mappers.SolicitudPlanificacionMapper;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Cubre {@code iniciarPlanificacion()}, que centraliza el armado de lotes y la creación de
 * solicitudes. Este comportamiento vivía antes en {@code PlanificadorDeEntregasTest}; se movió acá
 * porque ahora tanto el scheduler como el endpoint manual disparan el mismo método del service.
 */
@ExtendWith(MockitoExtension.class)
class PlanificacionServiceTest {

  @Mock private ISolicitudPlanificacionRepository solicitudRepo;
  @Mock private IRutasRepository rutasRepository;
  @Mock private IEntregasRepository entregasRepository;
  @Mock private ICamionRepository camionesRepository;
  @Mock private IServicioExternoPlanificacion generadorDeRutas;
  @Mock private LogisticaEventPublisher eventPublisher;

  @Captor private ArgumentCaptor<SolicitudPlanificacion> solicitudCaptor;
  @Captor private ArgumentCaptor<List<Entrega>> loteCaptor;

  private PlanificacionService planificacionService;

  @BeforeEach
  void setUp() {
    planificacionService =
        new PlanificacionService(
            solicitudRepo,
            rutasRepository,
            entregasRepository,
            camionesRepository,
            generadorDeRutas,
            new SolicitudPlanificacionMapper(),
            eventPublisher,
            50, // maxDonacionesPorLote
            "http://localhost:8083" // selfBaseUrl
            );
  }

  @Test
  @DisplayName(
      "Cuando no hay entregas pendientes, no debe crear solicitudes ni disparar el generador")
  void iniciarPlanificacion_sinEntregasPendientes_noDebeHacerNada() {
    when(entregasRepository.findAll()).thenReturn(Collections.emptyList());

    List<SolicitudPlanificacionResponseDTO> resultado = planificacionService.iniciarPlanificacion();

    assertTrue(resultado.isEmpty());
    verify(camionesRepository, never()).findAll();
    verify(solicitudRepo, never()).save(any());
    verify(generadorDeRutas, never()).generarRutas(any(), any(), any());
  }

  @Test
  @DisplayName(
      "Cuando hay entregas pero no hay camiones disponibles, debe posponer y no crear nada")
  void iniciarPlanificacion_conEntregasPeroSinCamiones_debePosponer() {
    Entrega entregaPendiente = mock(Entrega.class);
    when(entregaPendiente.getIdRuta()).thenReturn(null);
    when(entregasRepository.findAll()).thenReturn(List.of(entregaPendiente));
    when(camionesRepository.findAll()).thenReturn(Collections.emptyList());

    List<SolicitudPlanificacionResponseDTO> resultado = planificacionService.iniciarPlanificacion();

    assertTrue(resultado.isEmpty());
    verify(solicitudRepo, never()).save(any());
    verify(generadorDeRutas, never()).generarRutas(any(), any(), any());
  }

  @Test
  @DisplayName("Cuando hay 70 entregas y el lote es de 50, debe crear 2 lotes y 2 solicitudes")
  void iniciarPlanificacion_conMasEntregasQueElTamanioDelLote_debeParticionarCorrectamente() {
    List<Entrega> entregas = IntStream.range(0, 70).mapToObj(i -> mock(Entrega.class)).toList();
    when(entregasRepository.findAll()).thenReturn(entregas);

    Camion camionDisponible = mock(Camion.class);
    when(camionDisponible.estaDisponibleParaAsignar()).thenReturn(true);
    when(camionesRepository.findAll()).thenReturn(List.of(camionDisponible));
    when(solicitudRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    List<SolicitudPlanificacionResponseDTO> resultado = planificacionService.iniciarPlanificacion();

    assertEquals(2, resultado.size());
    verify(solicitudRepo, times(2)).save(solicitudCaptor.capture());
    verify(generadorDeRutas, times(2)).generarRutas(any(), loteCaptor.capture(), any());

    List<List<Entrega>> lotesGenerados = loteCaptor.getAllValues();
    assertEquals(50, lotesGenerados.get(0).size(), "El primer lote debe tener 50 entregas");
    assertEquals(20, lotesGenerados.get(1).size(), "El segundo lote debe tener 20 entregas");
  }

  @Test
  @DisplayName("Al iniciar la planificación de un lote, debe guardar una Solicitud y devolverla")
  void iniciarPlanificacion_conUnLote_debeGuardarSolicitudYLlamarGenerador() {
    List<Entrega> entregas = IntStream.range(0, 10).mapToObj(i -> mock(Entrega.class)).toList();
    when(entregasRepository.findAll()).thenReturn(entregas);

    Camion camionDisponible = mock(Camion.class);
    when(camionDisponible.estaDisponibleParaAsignar()).thenReturn(true);
    when(camionesRepository.findAll()).thenReturn(List.of(camionDisponible));
    when(solicitudRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    List<SolicitudPlanificacionResponseDTO> resultado = planificacionService.iniciarPlanificacion();

    assertEquals(1, resultado.size());
    verify(solicitudRepo, times(1)).save(solicitudCaptor.capture());
    verify(generadorDeRutas, times(1)).generarRutas(any(), any(), any());

    SolicitudPlanificacion solicitudGuardada = solicitudCaptor.getValue();
    assertNotNull(solicitudGuardada.getId());
    assertEquals(10, solicitudGuardada.getCantidadDonaciones());
    assertEquals(
        "http://localhost:8083/api/logistica/callback/rutas", solicitudGuardada.getCallbackUrl());

    SolicitudPlanificacionResponseDTO dto = resultado.get(0);
    assertEquals(solicitudGuardada.getId(), dto.id());
    assertEquals(solicitudGuardada.getEstado(), dto.estado());
  }
}
