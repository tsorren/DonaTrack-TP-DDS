package grupo5.logistica.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.dto.callback.CallbackPlanificacionRequestDTO;
import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.choferes.Chofer;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.solicitudes.SolicitudPlanificacion;
import grupo5.logistica.models.repositories.IChoferesRepository;
import grupo5.logistica.services.impl.GeneradorDeRutas;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

class GeneradorDeRutasTest {

  private AlgoritmoOrdenadorDeEntrega ordenadorEntregas;
  private AlgoritmoAsignadorDeEntregas asignadorDeEntregas;
  private IChoferesRepository choferesRepository;
  private RestTemplate restTemplate;

  private GeneradorDeRutas generador;

  private final UUID CAMION_ID = UUID.randomUUID();
  private final UUID CHOFER_ID = UUID.randomUUID();
  private final UUID ENTREGA_ID = UUID.randomUUID();

  @BeforeEach
  void setUp() {

    ordenadorEntregas = mock(AlgoritmoOrdenadorDeEntrega.class);
    asignadorDeEntregas = mock(AlgoritmoAsignadorDeEntregas.class);
    choferesRepository = mock(IChoferesRepository.class);
    restTemplate = mock(RestTemplate.class);

    generador =
        new GeneradorDeRutas(
            ordenadorEntregas, asignadorDeEntregas, choferesRepository, restTemplate);
  }

  private SolicitudPlanificacion solicitudMock() {

    SolicitudPlanificacion solicitud = mock(SolicitudPlanificacion.class);

    when(solicitud.getId()).thenReturn(UUID.randomUUID());

    when(solicitud.getCallbackUrl()).thenReturn("http://localhost:8080/callback");

    return solicitud;
  }

  // ======================================================
  // generarRutas OK
  // ======================================================

  @Test
  void generarRutas_deberiaCrearRutaYEnviarCallbackOK() {

    SolicitudPlanificacion solicitud = solicitudMock();

    Entrega entrega = mock(Entrega.class);
    Camion camion = mock(Camion.class);
    Chofer chofer = mock(Chofer.class);

    when(entrega.getId()).thenReturn(ENTREGA_ID);

    when(camion.estaDisponibleParaAsignar()).thenReturn(true);

    when(camion.getId()).thenReturn(CAMION_ID);

    when(chofer.estaDisponibleParaAsignar()).thenReturn(true);

    when(chofer.getId()).thenReturn(CHOFER_ID);

    when(ordenadorEntregas.obtenerEntregasOrdenadas(any())).thenReturn(List.of(entrega));

    when(asignadorDeEntregas.asignar(any(), any())).thenReturn(Map.of(camion, List.of(entrega)));

    when(choferesRepository.findAll()).thenReturn(List.of(chofer));

    generador.generarRutas(solicitud, List.of(entrega), List.of(camion));

    verify(entrega).asignarRuta(any());

    verify(restTemplate)
        .postForEntity(
            eq("http://localhost:8080/callback"),
            argThat((CallbackPlanificacionRequestDTO body) -> body.estado().equals("OK")),
            eq(Void.class));
  }

  // ======================================================
  // sin choferes disponibles
  // ======================================================

  @Test
  void generarRutas_deberiaEnviarOKAunqueNoHayaChoferDisponible() {

    SolicitudPlanificacion solicitud = solicitudMock();

    Entrega entrega = mock(Entrega.class);
    Camion camion = mock(Camion.class);

    when(camion.estaDisponibleParaAsignar()).thenReturn(true);

    when(asignadorDeEntregas.asignar(any(), any())).thenReturn(Map.of(camion, List.of(entrega)));

    when(choferesRepository.findAll()).thenReturn(List.of());

    generador.generarRutas(solicitud, List.of(entrega), List.of(camion));

    verify(restTemplate)
        .postForEntity(
            eq("http://localhost:8080/callback"),
            argThat((CallbackPlanificacionRequestDTO body) -> body.estado().equals("OK")),
            eq(Void.class));

    verify(entrega, never()).asignarRuta(any());
  }

  // ======================================================
  // camiones null
  // ======================================================

  @Test
  void generarRutas_deberiaEnviarErrorCuandoCamionesSonNull() {

    SolicitudPlanificacion solicitud = solicitudMock();

    generador.generarRutas(solicitud, List.of(), null);

    verify(restTemplate)
        .postForEntity(
            eq("http://localhost:8080/callback"),
            argThat(
                (CallbackPlanificacionRequestDTO body) ->
                    body.estado().equals("ERROR") && body.motivoError() != null),
            eq(Void.class));
  }

  // ======================================================
  // constructor
  // ======================================================

  @Test
  void constructor_deberiaLanzarExcepcionSiFaltanDependencias() {

    assertThrows(
        ValidationException.class,
        () -> new GeneradorDeRutas(null, asignadorDeEntregas, choferesRepository, restTemplate));
  }
}
