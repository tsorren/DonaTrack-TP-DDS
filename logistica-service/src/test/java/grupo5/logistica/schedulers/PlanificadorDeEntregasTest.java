package grupo5.logistica.schedulers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.solicitudes.SolicitudPlanificacion;
import grupo5.logistica.models.repositories.ICamionRepository;
import grupo5.logistica.models.repositories.IEntregasRepository;
import grupo5.logistica.models.repositories.ISolicitudPlanificacionRepository;
import grupo5.logistica.services.IServicioExternoPlanificacion;
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

@ExtendWith(MockitoExtension.class)
class PlanificadorDeEntregasTest {

  @Mock private IEntregasRepository entregasRepository;

  @Mock private ICamionRepository camionesRepository;

  @Mock private ISolicitudPlanificacionRepository solicitudRepo;

  @Mock private IServicioExternoPlanificacion generadorDeRutas;

  @Captor private ArgumentCaptor<SolicitudPlanificacion> solicitudCaptor;

  @Captor private ArgumentCaptor<List<Entrega>> loteCaptor;

  private PlanificadorDeEntregas planificador;

  @BeforeEach
  void setUp() {
    planificador =
        new PlanificadorDeEntregas(
            entregasRepository,
            camionesRepository,
            solicitudRepo,
            generadorDeRutas,
            50, // maxDonacionesPorLote
            "http://localhost:8083" // selfBaseUrl
            );
  }

  @Test
  @DisplayName("Cuando no hay entregas pendientes, el planificador no debe hacer nada")
  void ejecutar_sinEntregasPendientes_noDebeHacerNada() {
    when(entregasRepository.findAll()).thenReturn(Collections.emptyList());

    planificador.ejecutar();

    verify(camionesRepository, never()).findAll();
    verify(solicitudRepo, never()).save(any());
    verify(generadorDeRutas, never()).generarRutas(any(), any(), any());
  }

  @Test
  @DisplayName("Cuando hay entregas pero no hay camiones, el planificador debe loguear y terminar")
  void ejecutar_conEntregasPeroSinCamiones_debeTerminar() {
    // Corregido: es necesario filtrar por entregas sin ruta.
    Entrega entregaPendiente = mock(Entrega.class);
    when(entregaPendiente.getIdRuta()).thenReturn(null);
    when(entregasRepository.findAll()).thenReturn(List.of(entregaPendiente));
    when(camionesRepository.findAll()).thenReturn(Collections.emptyList());

    planificador.ejecutar();

    verify(solicitudRepo, never()).save(any());
    verify(generadorDeRutas, never()).generarRutas(any(), any(), any());
  }

  @Test
  @DisplayName("Cuando hay 70 entregas y el lote es de 50, debe crear 2 lotes y 2 solicitudes")
  void ejecutar_conMasEntregasQueElTamanioDelLote_debeParticionarCorrectamente() {
    List<Entrega> entregas = IntStream.range(0, 70).mapToObj(i -> mock(Entrega.class)).toList();
    when(entregasRepository.findAll()).thenReturn(entregas);

    Camion camionDisponible = mock(Camion.class);
    when(camionDisponible.estaDisponibleParaAsignar()).thenReturn(true);
    when(camionesRepository.findAll()).thenReturn(List.of(camionDisponible));

    planificador.ejecutar();

    verify(solicitudRepo, times(2)).save(solicitudCaptor.capture());
    verify(generadorDeRutas, times(2)).generarRutas(any(), loteCaptor.capture(), any());

    List<List<Entrega>> lotesGenerados = loteCaptor.getAllValues();
    assertEquals(50, lotesGenerados.get(0).size(), "El primer lote debe tener 50 entregas");
    assertEquals(20, lotesGenerados.get(1).size(), "El segundo lote debe tener 20 entregas");
  }

  @Test
  @DisplayName(
      "Al solicitar planificación, debe guardar una Solicitud y llamar al generador de rutas")
  void ejecutar_conUnLote_debeGuardarSolicitudYLlamarGenerador() {
    List<Entrega> entregas = IntStream.range(0, 10).mapToObj(i -> mock(Entrega.class)).toList();
    when(entregasRepository.findAll()).thenReturn(entregas);

    Camion camionDisponible = mock(Camion.class);
    when(camionDisponible.estaDisponibleParaAsignar()).thenReturn(true);
    when(camionesRepository.findAll()).thenReturn(List.of(camionDisponible));

    planificador.ejecutar();

    verify(solicitudRepo, times(1)).save(solicitudCaptor.capture());
    verify(generadorDeRutas, times(1)).generarRutas(any(), any(), any());

    SolicitudPlanificacion solicitudGuardada = solicitudCaptor.getValue();
    assertNotNull(solicitudGuardada.getId());
    assertEquals(10, solicitudGuardada.getCantidadDonaciones());
    assertEquals(
        "http://localhost:8083/api/logistica/callback/rutas", solicitudGuardada.getCallbackUrl());
  }
}
