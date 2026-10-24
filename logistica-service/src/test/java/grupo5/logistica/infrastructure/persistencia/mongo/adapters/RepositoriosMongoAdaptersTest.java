package grupo5.logistica.infrastructure.persistencia.mongo.adapters; // NOSONAR

import static java.time.ZoneOffset.UTC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import grupo5.logistica.infrastructure.persistencia.mongo.documents.CamionDocument;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.ChoferDocument;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.EntregaDocument;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.EventoEntregaDocument;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.RutaDocument;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.SolicitudPlanificacionDocument;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.SolicitudTransicionEntregaDocument;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.TipoEventoEntrega;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.TipoTransicionEntrega;
import grupo5.logistica.infrastructure.persistencia.mongo.mappers.CamionMongoMapper;
import grupo5.logistica.infrastructure.persistencia.mongo.mappers.ChoferMongoMapper;
import grupo5.logistica.infrastructure.persistencia.mongo.mappers.EntregaMongoMapper;
import grupo5.logistica.infrastructure.persistencia.mongo.mappers.RutaMongoMapper;
import grupo5.logistica.infrastructure.persistencia.mongo.mappers.SolicitudPlanificacionMongoMapper;
import grupo5.logistica.infrastructure.persistencia.mongo.repositories.SpringDataCamionMongoRepository;
import grupo5.logistica.infrastructure.persistencia.mongo.repositories.SpringDataChoferMongoRepository;
import grupo5.logistica.infrastructure.persistencia.mongo.repositories.SpringDataEntregaMongoRepository;
import grupo5.logistica.infrastructure.persistencia.mongo.repositories.SpringDataEventoEntregaMongoRepository;
import grupo5.logistica.infrastructure.persistencia.mongo.repositories.SpringDataRutaMongoRepository;
import grupo5.logistica.infrastructure.persistencia.mongo.repositories.SpringDataSolicitudPlanificacionMongoRepository;
import grupo5.logistica.infrastructure.persistencia.mongo.repositories.SpringDataSolicitudTransicionEntregaMongoRepository;
import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.camiones.EstadoCamion;
import grupo5.logistica.models.entities.choferes.Chofer;
import grupo5.logistica.models.entities.choferes.EstadoChofer;
import grupo5.logistica.models.entities.entregas.ConfirmacionRecepcion;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.entregas.EstadoEntrega;
import grupo5.logistica.models.entities.entregas.NoRecepcion;
import grupo5.logistica.models.entities.entregas.RegresoDeposito;
import grupo5.logistica.models.entities.entregas.RevisionEntrega;
import grupo5.logistica.models.entities.entregas.SolicitudTransicionEntrega;
import grupo5.logistica.models.entities.entregas.eventos.EntregaConfirmada;
import grupo5.logistica.models.entities.entregas.eventos.EntregaFallida;
import grupo5.logistica.models.entities.rutas.EstadoRuta;
import grupo5.logistica.models.entities.rutas.Ruta;
import grupo5.logistica.models.entities.rutas.eventos.EventoRutaAsignada;
import grupo5.logistica.models.entities.solicitudes.SolicitudPlanificacion;
import grupo5.logistica.testutils.EntregaMother;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@SuppressWarnings("java:S120")
class RepositoriosMongoAdaptersTest {

  private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-10-24T13:00:00Z"), UTC);

  @Test
  void camionMongoAdapter_deberiaConsultarPorEstadoYPatenteConRutaDerivada() {
    SpringDataCamionMongoRepository springDataRepo = mock(SpringDataCamionMongoRepository.class);
    SpringDataRutaMongoRepository rutaRepo = mock(SpringDataRutaMongoRepository.class);
    CamionMongoMapper mapper = new CamionMongoMapper();
    CamionRepositoryMongoAdapter adapter =
        new CamionRepositoryMongoAdapter(springDataRepo, rutaRepo, mapper);
    Camion camion = new Camion("AB123CD", 50.0f, 2000.0f, 3.0f);

    assertTrue(adapter.findByEstado(null).isEmpty());
    assertTrue(adapter.findByPatente(null).isEmpty());
    assertTrue(adapter.findByPatente("   ").isEmpty());

    CamionDocument doc = mapper.toDocument(camion);
    when(springDataRepo.findByEstado(EstadoCamion.DISPONIBLE)).thenReturn(List.of(doc));
    when(springDataRepo.findByEstadoNot(EstadoCamion.DESHABILITADO)).thenReturn(List.of(doc));
    when(springDataRepo.findByPatente("AB123CD")).thenReturn(Optional.of(doc));
    when(rutaRepo.findFirstByIdCamionAndEstado(camion.getId(), EstadoRuta.EN_TRASLADO))
        .thenReturn(Optional.empty());

    assertEquals(1, adapter.findByEstado(EstadoCamion.DISPONIBLE).size());
    assertEquals(1, adapter.findActivos().size());
    assertEquals(1, adapter.findDisponibles().size());
    assertTrue(adapter.findByPatente("ab-123-cd").isPresent());

    UUID rutaActivaId = UUID.randomUUID();
    camion.asignarARuta(rutaActivaId);
    RutaDocument rutaDoc = RutaDocument.builder().id(rutaActivaId).build();
    CamionDocument docEnRuta = mapper.toDocument(camion);
    when(springDataRepo.findByEstado(EstadoCamion.EN_RUTA)).thenReturn(List.of(docEnRuta));
    when(rutaRepo.findFirstByIdCamionAndEstado(camion.getId(), EstadoRuta.EN_TRASLADO))
        .thenReturn(Optional.of(rutaDoc));
    assertEquals(rutaActivaId, adapter.findByEstado(EstadoCamion.EN_RUTA).getFirst().getRutaId());
  }

  @Test
  void choferMongoAdapter_deberiaConsultarActivosYDisponiblesConRutaDerivada() {
    SpringDataChoferMongoRepository springDataRepo = mock(SpringDataChoferMongoRepository.class);
    SpringDataRutaMongoRepository rutaRepo = mock(SpringDataRutaMongoRepository.class);
    ChoferMongoMapper mapper = new ChoferMongoMapper();
    ChoferesRepositoryMongoAdapter adapter =
        new ChoferesRepositoryMongoAdapter(springDataRepo, rutaRepo, mapper);
    Chofer chofer = new Chofer("Luis", "Paz", "LIC-50", "1122334455");

    ChoferDocument doc = mapper.toDocument(chofer);
    when(springDataRepo.findByEstadoNot(EstadoChofer.DESHABILITADO)).thenReturn(List.of(doc));
    when(springDataRepo.findByEstado(EstadoChofer.DISPONIBLE)).thenReturn(List.of(doc));
    when(rutaRepo.findFirstByIdChoferAndEstado(chofer.getId(), EstadoRuta.EN_TRASLADO))
        .thenReturn(Optional.empty());

    assertEquals(1, adapter.findActivos().size());
    assertEquals(1, adapter.findDisponibles().size());

    UUID rutaActivaId = UUID.randomUUID();
    chofer.asignarARuta(rutaActivaId);
    RutaDocument rutaDoc = RutaDocument.builder().id(rutaActivaId).build();
    ChoferDocument docEnRuta = mapper.toDocument(chofer);
    when(springDataRepo.findByEstadoNot(EstadoChofer.DESHABILITADO)).thenReturn(List.of(docEnRuta));
    when(rutaRepo.findFirstByIdChoferAndEstado(chofer.getId(), EstadoRuta.EN_TRASLADO))
        .thenReturn(Optional.of(rutaDoc));
    assertEquals(rutaActivaId, adapter.findActivos().getFirst().getRutaId());
  }

  @Test
  void rutasMongoAdapter_deberiaGuardarYConsultarRutas() {
    SpringDataRutaMongoRepository springDataRepo = mock(SpringDataRutaMongoRepository.class);
    SpringDataEntregaMongoRepository entregaRepo = mock(SpringDataEntregaMongoRepository.class);
    RutaMongoMapper mapper = new RutaMongoMapper();
    RutasRepositoryMongoAdapter adapter =
        new RutasRepositoryMongoAdapter(springDataRepo, entregaRepo, mapper);

    assertNull(adapter.save(null));
    UUID entregaId = UUID.randomUUID();
    Ruta ruta = new Ruta(LocalDate.of(2026, 10, 12), UUID.randomUUID(), UUID.randomUUID());
    ruta.agregarEntrega(entregaId);
    when(entregaRepo.findAllById(List.of(entregaId)))
        .thenReturn(List.of(EntregaDocument.builder().id(entregaId).build()));

    when(springDataRepo.findById(ruta.getId())).thenReturn(Optional.empty());
    when(springDataRepo.save(any(RutaDocument.class))).thenAnswer(inv -> inv.getArgument(0));

    Ruta saved = adapter.save(ruta);
    assertNotNull(saved);
    assertEquals(ruta.getId(), saved.getId());
    assertEquals(List.of(entregaId), saved.getEntregaIds());
    assertEquals(1, adapter.saveAll(List.of(ruta)).size());
    assertTrue(adapter.saveAll(null).isEmpty());
    assertTrue(adapter.findByFecha(null).isEmpty());
    assertTrue(adapter.findByCamionId(null).isEmpty());
    assertTrue(adapter.findByCamionIdAndFecha(null, LocalDate.now()).isEmpty());
    assertTrue(adapter.findByCamionIdAndFecha(UUID.randomUUID(), null).isEmpty());

    RutaDocument doc = mapper.toDocument(ruta);
    when(springDataRepo.findByFecha(ruta.getFecha())).thenReturn(List.of(doc));
    when(springDataRepo.findByIdCamion(ruta.getCamionId())).thenReturn(List.of(doc));
    when(springDataRepo.findByIdCamionAndFecha(ruta.getCamionId(), ruta.getFecha()))
        .thenReturn(List.of(doc));

    assertEquals(1, adapter.findByFecha(ruta.getFecha()).size());
    assertEquals(1, adapter.findByCamionId(ruta.getCamionId()).size());
    assertEquals(1, adapter.findByCamionIdAndFecha(ruta.getCamionId(), ruta.getFecha()).size());
  }

  @Test
  void entregasMongoAdapter_deberiaConsultarPorEstadoRutaDonacionYBeneficiaria() {
    SpringDataEntregaMongoRepository springDataRepo = mock(SpringDataEntregaMongoRepository.class);
    EntregaMongoMapper mapper = new EntregaMongoMapper(Clock.systemUTC());
    EntregasRepositoryMongoAdapter adapter =
        new EntregasRepositoryMongoAdapter(
            springDataRepo,
            mock(SpringDataRutaMongoRepository.class),
            mock(SpringDataCamionMongoRepository.class),
            mapper);
    Entrega entrega = EntregaMother.pendiente();

    assertTrue(adapter.findByEstado(null).isEmpty());
    assertTrue(adapter.findByRutaId(null).isEmpty());
    assertFalse(adapter.existsByIdDonacion(null));

    EntregaDocument doc = mapper.toDocument(entrega);
    when(springDataRepo.findByEstado(EstadoEntrega.PENDIENTE)).thenReturn(List.of(doc));
    when(springDataRepo.findByAsignacionIdRutaOrderByIdAsc(any())).thenReturn(List.of(doc));
    when(springDataRepo.findByAsignacionIsNull()).thenReturn(List.of(doc));
    when(springDataRepo.existsByIdDonacion(entrega.getIdDonacion())).thenReturn(true);

    assertEquals(1, adapter.findByEstado(EstadoEntrega.PENDIENTE).size());
    assertEquals(1, adapter.findByRutaId(UUID.randomUUID()).size());
    assertEquals(1, adapter.findSinRuta().size());
    assertTrue(adapter.existsByIdDonacion(entrega.getIdDonacion()));

    assertTrue(adapter.findByIdDonacion(null).isEmpty());
    assertTrue(adapter.findByIdBeneficiaria(null).isEmpty());
    when(springDataRepo.findByIdDonacion(entrega.getIdDonacion())).thenReturn(Optional.of(doc));
    when(springDataRepo.findByIdBeneficiaria(entrega.getIdBeneficiaria())).thenReturn(List.of(doc));
    assertEquals(entrega.getId(), adapter.findByIdDonacion(entrega.getIdDonacion()).get().getId());
    assertEquals(1, adapter.findByIdBeneficiaria(entrega.getIdBeneficiaria()).size());
  }

  @Test
  void solicitudPlanificacionMongoAdapter_deberiaResolverElCrudConElAdaptadorBase() {
    SpringDataSolicitudPlanificacionMongoRepository springDataRepo =
        mock(SpringDataSolicitudPlanificacionMongoRepository.class);
    SolicitudPlanificacionMongoMapper mapper = new SolicitudPlanificacionMongoMapper();
    SolicitudPlanificacionRepositoryMongoAdapter adapter =
        new SolicitudPlanificacionRepositoryMongoAdapter(springDataRepo, mapper);

    assertNull(adapter.save(null));
    SolicitudPlanificacion solicitud =
        new SolicitudPlanificacion(
            UUID.randomUUID(), LocalDate.of(2026, 10, 15), 2, "http://localhost/cb");

    when(springDataRepo.findById(solicitud.getId())).thenReturn(Optional.empty());
    when(springDataRepo.save(any(SolicitudPlanificacionDocument.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    SolicitudPlanificacion saved = adapter.save(solicitud);
    assertNotNull(saved);
    assertEquals(solicitud.getId(), saved.getId());
    assertEquals(1, adapter.saveAll(List.of(solicitud)).size());
    assertTrue(adapter.saveAll(null).isEmpty());

    SolicitudPlanificacionDocument doc = mapper.toDocument(solicitud);
    when(springDataRepo.findAll()).thenReturn(List.of(doc));
    when(springDataRepo.findById(solicitud.getId())).thenReturn(Optional.of(doc));
    when(springDataRepo.existsById(solicitud.getId())).thenReturn(true);
    when(springDataRepo.count()).thenReturn(1L);

    assertEquals(1, adapter.findAll().size());
    assertTrue(adapter.findById(solicitud.getId()).isPresent());
    assertTrue(adapter.findById(null).isEmpty());
    assertTrue(adapter.existsById(solicitud.getId()));
    assertFalse(adapter.existsById(null));
    assertEquals(1L, adapter.count());

    adapter.delete(solicitud);
    verify(springDataRepo).deleteById(solicitud.getId());

    adapter.deleteAll();
    verify(springDataRepo).deleteAll();
  }

  @Test
  void solicitudesTransicionEntregaMongoAdapter_sinSolicitud_noGuardaNada() {
    SpringDataSolicitudTransicionEntregaMongoRepository springDataRepo =
        mock(SpringDataSolicitudTransicionEntregaMongoRepository.class);

    new SolicitudesTransicionEntregaRepositoryMongoAdapter(springDataRepo).registrar(null);

    verify(springDataRepo, never()).save(any());
  }

  @Test
  void solicitudesTransicionEntregaMongoAdapter_registraLaConfirmacionConSusFotos() {
    Entrega entrega = EntregaMother.pendiente();

    SolicitudTransicionEntregaDocument doc =
        transicionRegistrada(new ConfirmacionRecepcion(entrega, "Actor1", "http://foto"));

    assertEquals(TipoTransicionEntrega.CONFIRMAR_ENTREGA, doc.getTipoTransicion());
    assertEquals(List.of("http://foto"), doc.getFotosRecepcion());
    assertEquals(entrega.getId(), doc.getIdEntrega());
  }

  @Test
  void solicitudesTransicionEntregaMongoAdapter_registraLaNoRecepcionConSuJustificacion() {
    SolicitudTransicionEntregaDocument doc =
        transicionRegistrada(new NoRecepcion(EntregaMother.pendiente(), "Actor2", "Cerrado", true));

    assertEquals(TipoTransicionEntrega.NO_RECEPCION, doc.getTipoTransicion());
    assertEquals("Cerrado", doc.getJustificacion());
    assertTrue(doc.getReplanificable());
  }

  @Test
  void solicitudesTransicionEntregaMongoAdapter_registraLaRevisionYElRegresoAlDeposito() {
    Entrega entrega = EntregaMother.pendiente();

    assertEquals(
        TipoTransicionEntrega.REVISION,
        transicionRegistrada(new RevisionEntrega(entrega, "Actor3")).getTipoTransicion());
    assertEquals(
        TipoTransicionEntrega.REGRESO_DEPOSITO,
        transicionRegistrada(new RegresoDeposito(entrega, "Actor4")).getTipoTransicion());
  }

  @Test
  void eventosEntregaMongoAdapter_sinEventoOEntrega_noGuardaNada() {
    SpringDataEventoEntregaMongoRepository springDataRepo =
        mock(SpringDataEventoEntregaMongoRepository.class);
    EventosEntregaRepositoryMongoAdapter adapter =
        new EventosEntregaRepositoryMongoAdapter(springDataRepo, RELOJ);
    Entrega entrega = EntregaMother.pendiente();

    adapter.registrarRutaAsignada(null, entrega);
    adapter.registrarRutaAsignada(new EventoRutaAsignada(UUID.randomUUID(), entrega.getId()), null);
    adapter.registrarEntregaExitosa(null);
    adapter.registrarEntregaFallida(null);

    verify(springDataRepo, never()).save(any());
  }

  @Test
  void eventosEntregaMongoAdapter_registraLaRutaAsignadaConLaDonacionDeLaEntrega() {
    Entrega entrega = EntregaMother.pendiente();
    UUID rutaId = UUID.randomUUID();

    EventoEntregaDocument doc =
        eventoRegistrado(
            adapter ->
                adapter.registrarRutaAsignada(
                    new EventoRutaAsignada(rutaId, entrega.getId()), entrega));

    assertEquals(TipoEventoEntrega.RUTA_ASIGNADA, doc.getTipo());
    assertEquals(rutaId, doc.getIdRuta());
    assertEquals(entrega.getId(), doc.getIdEntrega());
    assertEquals(entrega.getIdDonacion(), doc.getIdDonacion());
  }

  @Test
  void eventosEntregaMongoAdapter_registraLaEntregaExitosaConSuRuta() {
    Entrega entrega = EntregaMother.pendiente();
    UUID rutaId = UUID.randomUUID();

    EventoEntregaDocument doc =
        eventoRegistrado(
            adapter ->
                adapter.registrarEntregaExitosa(
                    new EntregaConfirmada(entrega.getId(), entrega.getIdDonacion(), rutaId)));

    assertEquals(TipoEventoEntrega.ENTREGA_EXITOSA, doc.getTipo());
    assertEquals(entrega.getId(), doc.getIdEntrega());
    assertEquals(rutaId, doc.getIdRuta());
  }

  @Test
  void eventosEntregaMongoAdapter_registraLaEntregaFallidaSinRuta() {
    Entrega entrega = EntregaMother.pendiente();

    EventoEntregaDocument doc =
        eventoRegistrado(
            adapter ->
                adapter.registrarEntregaFallida(
                    new EntregaFallida(
                        entrega.getId(), entrega.getIdDonacion(), "No estaba", true)));

    assertEquals(TipoEventoEntrega.ENTREGA_FALLIDA, doc.getTipo());
    assertEquals("No estaba", doc.getJustificacion());
    assertTrue(doc.getReplanificable());
    assertNull(doc.getIdRuta());
  }

  @Test
  void eventosEntregaMongoAdapter_guardaElInstanteEnQueOcurrioElEvento() {
    Instant antes = Instant.now();
    EntregaConfirmada evento = new EntregaConfirmada(UUID.randomUUID(), UUID.randomUUID(), null);
    Instant despues = Instant.now();

    Instant ocurrioEn = exitosaRegistrada(evento).getOcurrioEn();

    assertFalse(ocurrioEn.isBefore(antes));
    assertFalse(ocurrioEn.isAfter(despues));
  }

  @Test
  void eventosEntregaMongoAdapter_sinTimestamp_usaLaHoraDelReloj() {
    EntregaFallida eventoSinTimestamp = mock(EntregaFallida.class);

    Instant ocurrioEn = fallidaRegistrada(eventoSinTimestamp).getOcurrioEn();

    assertEquals(RELOJ.instant(), ocurrioEn);
  }

  private static EventoEntregaDocument exitosaRegistrada(EntregaConfirmada evento) {
    return eventoRegistrado(adapter -> adapter.registrarEntregaExitosa(evento));
  }

  private static EventoEntregaDocument fallidaRegistrada(EntregaFallida evento) {
    return eventoRegistrado(adapter -> adapter.registrarEntregaFallida(evento));
  }

  private static SolicitudTransicionEntregaDocument transicionRegistrada(
      SolicitudTransicionEntrega solicitud) {
    SpringDataSolicitudTransicionEntregaMongoRepository springDataRepo =
        mock(SpringDataSolicitudTransicionEntregaMongoRepository.class);
    new SolicitudesTransicionEntregaRepositoryMongoAdapter(springDataRepo).registrar(solicitud);
    ArgumentCaptor<SolicitudTransicionEntregaDocument> captor =
        ArgumentCaptor.forClass(SolicitudTransicionEntregaDocument.class);
    verify(springDataRepo).save(captor.capture());
    return captor.getValue();
  }

  private static EventoEntregaDocument eventoRegistrado(
      Consumer<EventosEntregaRepositoryMongoAdapter> registrar) {
    SpringDataEventoEntregaMongoRepository springDataRepo =
        mock(SpringDataEventoEntregaMongoRepository.class);
    registrar.accept(new EventosEntregaRepositoryMongoAdapter(springDataRepo, RELOJ));
    ArgumentCaptor<EventoEntregaDocument> captor =
        ArgumentCaptor.forClass(EventoEntregaDocument.class);
    verify(springDataRepo).save(captor.capture());
    return captor.getValue();
  }
}
