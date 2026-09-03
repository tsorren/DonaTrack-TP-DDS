package grupo5.logistica.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import grupo5.logistica.dto.callback.CallbackPlanificacionRequestDTO;
import grupo5.logistica.dto.callback.RutaPlanificadaDTO;
import grupo5.logistica.dto.callback.SolicitudPlanificacionResponseDTO;
import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.choferes.Chofer;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.rutas.GeneradorDeRutas;
import grupo5.logistica.models.entities.rutas.GeneradorLotesSimple;
import grupo5.logistica.models.entities.rutas.PlanificacionSolicitada;
import grupo5.logistica.models.entities.rutas.Ruta;
import grupo5.logistica.models.entities.rutas.direccion.Direccion;
import grupo5.logistica.models.entities.rutas.direccion.Localidad;
import grupo5.logistica.models.entities.rutas.direccion.Pais;
import grupo5.logistica.models.entities.rutas.direccion.Provincia;
import grupo5.logistica.models.entities.rutas.eventos.EventoRutaAsignada;
import grupo5.logistica.models.entities.solicitudes.SolicitudPlanificacion;
import grupo5.logistica.models.repositories.ICamionRepository;
import grupo5.logistica.models.repositories.IChoferesRepository;
import grupo5.logistica.models.repositories.IEntregasRepository;
import grupo5.logistica.models.repositories.IRutasRepository;
import grupo5.logistica.models.repositories.ISolicitudPlanificacionRepository;
import grupo5.logistica.services.impl.PlanificacionService;
import grupo5.logistica.services.mappers.SolicitudPlanificacionMapper;
import grupo5.logistica.testutils.EntregaMother;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PlanificacionServiceTest {

  private ISolicitudPlanificacionRepository solicitudesRepository;
  private IRutasRepository rutasRepository;
  private IEntregasRepository entregasRepository;
  private ICamionRepository camionesRepository;
  private IChoferesRepository choferesRepository;
  private SolicitudPlanificacionMapper solicitudMapper;
  private ComunicadorEventosLogistica comunicadorEventos;
  private IServicioExternoPlanificacion planificadorExterno;
  private PlanificacionService service;

  @BeforeEach
  void setUp() {
    solicitudesRepository = mock(ISolicitudPlanificacionRepository.class);
    rutasRepository = mock(IRutasRepository.class);
    entregasRepository = mock(IEntregasRepository.class);
    camionesRepository = mock(ICamionRepository.class);
    choferesRepository = mock(IChoferesRepository.class);
    solicitudMapper = mock(SolicitudPlanificacionMapper.class);
    comunicadorEventos = mock(ComunicadorEventosLogistica.class);
    planificadorExterno = mock(IServicioExternoPlanificacion.class);
    service =
        new PlanificacionService(
            solicitudesRepository,
            rutasRepository,
            entregasRepository,
            camionesRepository,
            choferesRepository,
            solicitudMapper,
            comunicadorEventos,
            planificadorExterno,
            new GeneradorDeRutas(new GeneradorLotesSimple()),
            50,
            "http://logistica");
  }

  @Test
  void iniciarPlanificacionSinEntregasPendientesNoHaceNada() {
    when(entregasRepository.findSinRuta()).thenReturn(List.of());

    service.iniciarPlanificacion();

    verify(camionesRepository, never()).findDisponibles();
    verify(solicitudesRepository, never()).save(any());
    verify(planificadorExterno, never()).solicitarPlanificacion(any(), any());
  }

  @Test
  void iniciarPlanificacionConEntregasPeroSinCamionesTerminaSinSolicitar() {
    when(entregasRepository.findSinRuta()).thenReturn(List.of(EntregaMother.pendiente()));
    when(camionesRepository.findDisponibles()).thenReturn(List.of());
    when(choferesRepository.findDisponibles())
        .thenReturn(List.of(new Chofer("Ada", "Lovelace", "LIC-1", "1111")));

    service.iniciarPlanificacion();

    verify(solicitudesRepository, never()).save(any());
    verify(planificadorExterno, never()).solicitarPlanificacion(any(), any());
  }

  @Test
  void iniciarPlanificacionConSetentaEntregasYLoteDeCincuentaCreaDosLotesDeCincuentaYVeinte() {
    List<Entrega> entregas =
        IntStream.range(0, 70).mapToObj(i -> EntregaMother.pendiente()).toList();
    when(entregasRepository.findSinRuta()).thenReturn(entregas);
    when(camionesRepository.findDisponibles())
        .thenReturn(List.of(new Camion("AB123CD", 20f, 5000f, 3f)));
    when(choferesRepository.findDisponibles())
        .thenReturn(List.of(new Chofer("Ada", "Lovelace", "LIC-1", "1111")));

    service.iniciarPlanificacion();

    ArgumentCaptor<PlanificacionSolicitada> captor =
        ArgumentCaptor.forClass(PlanificacionSolicitada.class);
    verify(solicitudesRepository, times(2)).save(any(SolicitudPlanificacion.class));
    verify(planificadorExterno, times(2))
        .solicitarPlanificacion(any(SolicitudPlanificacion.class), captor.capture());
    assertEquals(50, captor.getAllValues().getFirst().entregas().size());
    assertEquals(20, captor.getAllValues().getLast().entregas().size());
  }

  @Test
  void iniciarPlanificacionConUnLoteGuardaSolicitudYLlamaAlClienteExterno() {
    List<Entrega> entregas =
        IntStream.range(0, 10).mapToObj(i -> EntregaMother.pendiente()).toList();
    when(entregasRepository.findSinRuta()).thenReturn(entregas);
    when(camionesRepository.findDisponibles())
        .thenReturn(List.of(new Camion("AB123CD", 20f, 5000f, 3f)));
    when(choferesRepository.findDisponibles())
        .thenReturn(List.of(new Chofer("Ada", "Lovelace", "LIC-1", "1111")));

    service.iniciarPlanificacion();

    ArgumentCaptor<SolicitudPlanificacion> solicitudCaptor =
        ArgumentCaptor.forClass(SolicitudPlanificacion.class);
    ArgumentCaptor<PlanificacionSolicitada> planificacionCaptor =
        ArgumentCaptor.forClass(PlanificacionSolicitada.class);
    verify(solicitudesRepository).save(solicitudCaptor.capture());
    verify(planificadorExterno)
        .solicitarPlanificacion(eq(solicitudCaptor.getValue()), planificacionCaptor.capture());
    assertNotNull(solicitudCaptor.getValue().getId());
    assertEquals(10, solicitudCaptor.getValue().getCantidadDonaciones());
    assertEquals(
        "http://logistica/api/logistica/callback/rutas",
        solicitudCaptor.getValue().getCallbackUrl());
    assertEquals(10, planificacionCaptor.getValue().entregas().size());
  }

  @Test
  void procesarCallbackDelegaLaCreacionAlDominioYPersisteElResultado() {
    Entrega entrega = crearEntrega();
    Camion camion = new Camion("AB123CD", 20f, 5000f, 3f);
    Chofer chofer = new Chofer("Ada", "Lovelace", "LIC-1", "1111");
    UUID solicitudId = UUID.randomUUID();
    SolicitudPlanificacion seguimiento =
        new SolicitudPlanificacion(solicitudId, LocalDate.now(), 1, "http://callback");
    CallbackPlanificacionRequestDTO callback =
        new CallbackPlanificacionRequestDTO(
            solicitudId,
            List.of(
                new RutaPlanificadaDTO(
                    camion.getId(),
                    chofer.getId(),
                    LocalDate.now().plusDays(1),
                    List.of(entrega.getId()))),
            "OK",
            null);
    SolicitudPlanificacionResponseDTO response = mock(SolicitudPlanificacionResponseDTO.class);
    when(solicitudesRepository.findById(solicitudId)).thenReturn(Optional.of(seguimiento));
    when(camionesRepository.findById(camion.getId())).thenReturn(Optional.of(camion));
    when(choferesRepository.findById(chofer.getId())).thenReturn(Optional.of(chofer));
    when(entregasRepository.findById(entrega.getId())).thenReturn(Optional.of(entrega));
    when(solicitudesRepository.save(any(SolicitudPlanificacion.class)))
        .thenAnswer(invocacion -> invocacion.getArgument(0));
    when(solicitudMapper.toResponseDTO(seguimiento)).thenReturn(response);

    SolicitudPlanificacionResponseDTO resultado = service.procesarCallback(callback);

    ArgumentCaptor<Ruta> rutaCaptor = ArgumentCaptor.forClass(Ruta.class);
    ArgumentCaptor<EventoRutaAsignada> eventoCaptor =
        ArgumentCaptor.forClass(EventoRutaAsignada.class);
    verify(rutasRepository).save(rutaCaptor.capture());
    verify(entregasRepository).save(entrega);
    verify(comunicadorEventos).comunicarRutaAsignada(eventoCaptor.capture(), eq(entrega));
    assertEquals(rutaCaptor.getValue().getId(), eventoCaptor.getValue().getRutaId());
    assertEquals(entrega.getId(), eventoCaptor.getValue().getEntregaId());
    assertEquals(0, rutaCaptor.getValue().getDomainEvents().size());
    assertEquals(rutaCaptor.getValue().getId(), entrega.getIdRuta());
    assertEquals(response, resultado);
  }

  @Test
  void procesarCallbackSobreUnaSolicitudYaProcesadaDevuelveSinReprocesar() {
    UUID solicitudId = UUID.randomUUID();
    SolicitudPlanificacion procesada =
        new SolicitudPlanificacion(solicitudId, LocalDate.now(), 1, "http://logistica");
    procesada.procesarResultados(List.of(UUID.randomUUID()));
    CallbackPlanificacionRequestDTO callback =
        new CallbackPlanificacionRequestDTO(solicitudId, List.of(), "OK", null);
    SolicitudPlanificacionResponseDTO response = mock(SolicitudPlanificacionResponseDTO.class);
    when(solicitudesRepository.findById(solicitudId)).thenReturn(Optional.of(procesada));
    when(solicitudMapper.toResponseDTO(procesada)).thenReturn(response);

    SolicitudPlanificacionResponseDTO resultado = service.procesarCallback(callback);

    verify(rutasRepository, never()).save(any());
    verify(entregasRepository, never()).save(any());
    verify(comunicadorEventos, never()).comunicarRutaAsignada(any(), any());
    assertEquals(response, resultado);
  }

  private static Entrega crearEntrega() {
    Pais pais = new Pais("Argentina");
    Provincia provincia = new Provincia("Buenos Aires", pais);
    Localidad localidad = new Localidad("CABA", provincia);
    Direccion direccion = new Direccion("Calle", 123, null, null, "1000", localidad);
    return new Entrega(UUID.randomUUID(), UUID.randomUUID(), direccion, 10f, 2f);
  }
}
