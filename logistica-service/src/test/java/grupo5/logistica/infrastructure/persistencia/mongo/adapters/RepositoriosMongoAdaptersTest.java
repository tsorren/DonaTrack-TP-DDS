package grupo5.logistica.infrastructure.persistencia.mongo.adapters; // NOSONAR

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

import grupo5.logistica.infrastructure.persistencia.jpa.entities.TipoEventoEntrega;
import grupo5.logistica.infrastructure.persistencia.jpa.entities.TipoTransicionEntrega;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.CamionDocument;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.ChoferDocument;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.EntregaDocument;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.EventoEntregaDocument;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.RutaDocument;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.SolicitudPlanificacionDocument;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.SolicitudTransicionEntregaDocument;
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
import grupo5.logistica.models.entities.entregas.eventos.EntregaConfirmada;
import grupo5.logistica.models.entities.entregas.eventos.EntregaFallida;
import grupo5.logistica.models.entities.rutas.EstadoRuta;
import grupo5.logistica.models.entities.rutas.Ruta;
import grupo5.logistica.models.entities.rutas.direccion.Direccion;
import grupo5.logistica.models.entities.rutas.direccion.Localidad;
import grupo5.logistica.models.entities.rutas.direccion.Pais;
import grupo5.logistica.models.entities.rutas.direccion.Provincia;
import grupo5.logistica.models.entities.rutas.eventos.EventoRutaAsignada;
import grupo5.logistica.models.entities.solicitudes.SolicitudPlanificacion;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@SuppressWarnings("java:S120")
class RepositoriosMongoAdaptersTest {

  @Test
  void camionMongoAdapter_deberiaEjecutarOperacionesCrudYConsultasConRutaDerivada() {
    SpringDataCamionMongoRepository springDataRepo = mock(SpringDataCamionMongoRepository.class);
    SpringDataRutaMongoRepository rutaRepo = mock(SpringDataRutaMongoRepository.class);
    CamionMongoMapper mapper = new CamionMongoMapper();
    CamionRepositoryMongoAdapter adapter =
        new CamionRepositoryMongoAdapter(springDataRepo, rutaRepo, mapper);

    assertNull(adapter.save(null));
    Camion camion = new Camion("AB123CD", 50.0f, 2000.0f, 3.0f);
    when(springDataRepo.findById(camion.getId())).thenReturn(Optional.empty());
    when(springDataRepo.save(any(CamionDocument.class))).thenAnswer(inv -> inv.getArgument(0));

    Camion saved = adapter.save(camion);
    assertNotNull(saved);
    assertEquals(camion.getId(), saved.getId());
    assertEquals(1, adapter.saveAll(List.of(camion)).size());
    assertTrue(adapter.saveAll(null).isEmpty());
    assertTrue(adapter.findByEstado(null).isEmpty());
    assertTrue(adapter.findByPatente(null).isEmpty());
    assertTrue(adapter.findByPatente("   ").isEmpty());

    CamionDocument doc = mapper.toDocument(camion);
    when(springDataRepo.findByEstado(EstadoCamion.DISPONIBLE)).thenReturn(List.of(doc));
    when(springDataRepo.findByEstadoNot(EstadoCamion.DESHABILITADO)).thenReturn(List.of(doc));
    when(springDataRepo.findByPatenteIgnoreCase("AB123CD")).thenReturn(Optional.of(doc));
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

    // CRUD base methods
    when(springDataRepo.findAll()).thenReturn(List.of(doc));
    when(springDataRepo.findById(camion.getId())).thenReturn(Optional.of(doc));
    when(springDataRepo.existsById(camion.getId())).thenReturn(true);
    when(springDataRepo.count()).thenReturn(1L);

    assertEquals(1, adapter.findAll().size());
    assertTrue(adapter.findById(camion.getId()).isPresent());
    assertTrue(adapter.findById(null).isEmpty());
    assertTrue(adapter.existsById(camion.getId()));
    assertFalse(adapter.existsById(null));
    assertEquals(1L, adapter.count());

    adapter.delete(camion);
    verify(springDataRepo).deleteById(camion.getId());
    adapter.delete(null);

    adapter.deleteAll();
    verify(springDataRepo).deleteAll();
  }

  @Test
  void choferMongoAdapter_deberiaEjecutarOperacionesCrudYConsultasConRutaDerivada() {
    SpringDataChoferMongoRepository springDataRepo = mock(SpringDataChoferMongoRepository.class);
    SpringDataRutaMongoRepository rutaRepo = mock(SpringDataRutaMongoRepository.class);
    ChoferMongoMapper mapper = new ChoferMongoMapper();
    ChoferesRepositoryMongoAdapter adapter =
        new ChoferesRepositoryMongoAdapter(springDataRepo, rutaRepo, mapper);

    assertNull(adapter.save(null));
    Chofer chofer = new Chofer("Luis", "Paz", "LIC-50", "1122334455");
    when(springDataRepo.findById(chofer.getId())).thenReturn(Optional.empty());
    when(springDataRepo.save(any(ChoferDocument.class))).thenAnswer(inv -> inv.getArgument(0));

    Chofer saved = adapter.save(chofer);
    assertNotNull(saved);
    assertEquals(chofer.getId(), saved.getId());
    assertEquals(1, adapter.saveAll(List.of(chofer)).size());
    assertTrue(adapter.saveAll(null).isEmpty());

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

    // CRUD base methods
    when(springDataRepo.findAll()).thenReturn(List.of(doc));
    when(springDataRepo.findById(chofer.getId())).thenReturn(Optional.of(doc));
    when(springDataRepo.existsById(chofer.getId())).thenReturn(true);
    when(springDataRepo.count()).thenReturn(1L);

    assertEquals(1, adapter.findAll().size());
    assertTrue(adapter.findById(chofer.getId()).isPresent());
    assertTrue(adapter.findById(null).isEmpty());
    assertTrue(adapter.existsById(chofer.getId()));
    assertFalse(adapter.existsById(null));
    assertEquals(1L, adapter.count());

    adapter.delete(chofer);
    verify(springDataRepo).deleteById(chofer.getId());

    adapter.deleteAll();
    verify(springDataRepo).deleteAll();
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

    // CRUD base methods
    when(springDataRepo.findAll()).thenReturn(List.of(doc));
    when(springDataRepo.findById(ruta.getId())).thenReturn(Optional.of(doc));
    when(springDataRepo.existsById(ruta.getId())).thenReturn(true);
    when(springDataRepo.count()).thenReturn(1L);

    assertEquals(1, adapter.findAll().size());
    assertTrue(adapter.findById(ruta.getId()).isPresent());
    assertTrue(adapter.findById(null).isEmpty());
    assertTrue(adapter.existsById(ruta.getId()));
    assertFalse(adapter.existsById(null));
    assertEquals(1L, adapter.count());

    adapter.delete(ruta);
    verify(springDataRepo).deleteById(ruta.getId());

    adapter.deleteAll();
    verify(springDataRepo).deleteAll();
  }

  @Test
  void entregasMongoAdapter_deberiaPersistirDireccionEmbebidaYConsultarEntregas() {
    SpringDataEntregaMongoRepository springDataRepo = mock(SpringDataEntregaMongoRepository.class);
    EntregaMongoMapper mapper = new EntregaMongoMapper(Clock.systemUTC());
    EntregasRepositoryMongoAdapter adapter =
        new EntregasRepositoryMongoAdapter(
            springDataRepo,
            mock(SpringDataRutaMongoRepository.class),
            mock(SpringDataCamionMongoRepository.class),
            mapper);

    assertNull(adapter.save(null));
    Localidad caba = new Localidad("CABA", new Provincia("Buenos Aires", new Pais("Argentina")));
    Direccion dir = new Direccion("Callao", 500, null, null, "1022", caba);
    Entrega entrega = new Entrega(UUID.randomUUID(), UUID.randomUUID(), dir, 10.0f, 1.0f);

    when(springDataRepo.findById(entrega.getId())).thenReturn(Optional.empty());
    when(springDataRepo.save(any(EntregaDocument.class))).thenAnswer(inv -> inv.getArgument(0));

    Entrega saved = adapter.save(entrega);
    assertNotNull(saved);
    assertEquals(entrega.getId(), saved.getId());
    assertEquals(1, adapter.saveAll(List.of(entrega)).size());
    assertTrue(adapter.saveAll(null).isEmpty());
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

    // CRUD base methods
    when(springDataRepo.findAll()).thenReturn(List.of(doc));
    when(springDataRepo.findById(entrega.getId())).thenReturn(Optional.of(doc));
    when(springDataRepo.existsById(entrega.getId())).thenReturn(true);
    when(springDataRepo.count()).thenReturn(1L);

    assertEquals(1, adapter.findAll().size());
    assertTrue(adapter.findById(entrega.getId()).isPresent());
    assertTrue(adapter.findById(null).isEmpty());
    assertTrue(adapter.existsById(entrega.getId()));
    assertFalse(adapter.existsById(null));
    assertEquals(1L, adapter.count());

    adapter.delete(entrega);
    verify(springDataRepo).deleteById(entrega.getId());

    adapter.deleteAll();
    verify(springDataRepo).deleteAll();
  }

  @Test
  void solicitudPlanificacionMongoAdapter_deberiaGuardarYSincronizar() {
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
  void solicitudesTransicionEntregaMongoAdapter_deberiaRegistrarTransiciones() {
    SpringDataSolicitudTransicionEntregaMongoRepository springDataRepo =
        mock(SpringDataSolicitudTransicionEntregaMongoRepository.class);
    SolicitudesTransicionEntregaRepositoryMongoAdapter adapter =
        new SolicitudesTransicionEntregaRepositoryMongoAdapter(springDataRepo);

    Localidad caba = new Localidad("CABA", new Provincia("Buenos Aires", new Pais("Argentina")));
    Direccion dir = new Direccion("Salta", 100, null, null, "1074", caba);
    Entrega entrega = new Entrega(UUID.randomUUID(), UUID.randomUUID(), dir, 5.0f, 0.5f);

    adapter.registrar(null);
    verify(springDataRepo, never()).save(any());

    ArgumentCaptor<SolicitudTransicionEntregaDocument> captor =
        ArgumentCaptor.forClass(SolicitudTransicionEntregaDocument.class);

    // ConfirmacionRecepcion
    adapter.registrar(new ConfirmacionRecepcion(entrega, "Actor1", "http://foto"));
    verify(springDataRepo).save(captor.capture());
    assertEquals(TipoTransicionEntrega.CONFIRMAR_ENTREGA, captor.getValue().getTipoTransicion());
    assertEquals(List.of("http://foto"), captor.getValue().getFotosRecepcion());
    assertEquals(entrega.getId(), captor.getValue().getIdEntrega());

    // NoRecepcion
    adapter.registrar(new NoRecepcion(entrega, "Actor2", "Cerrado", true));
    verify(springDataRepo, org.mockito.Mockito.times(2)).save(captor.capture());
    assertEquals(TipoTransicionEntrega.NO_RECEPCION, captor.getValue().getTipoTransicion());
    assertEquals("Cerrado", captor.getValue().getJustificacion());
    assertTrue(captor.getValue().getReplanificable());

    // RevisionEntrega
    adapter.registrar(new RevisionEntrega(entrega, "Actor3"));
    verify(springDataRepo, org.mockito.Mockito.times(3)).save(captor.capture());
    assertEquals(TipoTransicionEntrega.REVISION, captor.getValue().getTipoTransicion());

    // RegresoDeposito
    adapter.registrar(new RegresoDeposito(entrega, "Actor4"));
    verify(springDataRepo, org.mockito.Mockito.times(4)).save(captor.capture());
    assertEquals(TipoTransicionEntrega.REGRESO_DEPOSITO, captor.getValue().getTipoTransicion());
  }

  @Test
  void eventosEntregaMongoAdapter_deberiaRegistrarEventos() {
    SpringDataEventoEntregaMongoRepository springDataRepo =
        mock(SpringDataEventoEntregaMongoRepository.class);
    EventosEntregaRepositoryMongoAdapter adapter =
        new EventosEntregaRepositoryMongoAdapter(springDataRepo);

    Localidad caba = new Localidad("CABA", new Provincia("Buenos Aires", new Pais("Argentina")));
    Direccion dir = new Direccion("Salta", 100, null, null, "1074", caba);
    Entrega entrega = new Entrega(UUID.randomUUID(), UUID.randomUUID(), dir, 5.0f, 0.5f);

    adapter.registrarRutaAsignada(null, entrega);
    adapter.registrarRutaAsignada(new EventoRutaAsignada(UUID.randomUUID(), entrega.getId()), null);
    adapter.registrarEntregaExitosa(null);
    adapter.registrarEntregaFallida(null);
    verify(springDataRepo, never()).save(any());

    ArgumentCaptor<EventoEntregaDocument> captor =
        ArgumentCaptor.forClass(EventoEntregaDocument.class);

    // RutaAsignada
    UUID rutaId = UUID.randomUUID();
    EventoRutaAsignada evRuta = new EventoRutaAsignada(rutaId, entrega.getId());
    adapter.registrarRutaAsignada(evRuta, entrega);
    verify(springDataRepo).save(captor.capture());
    assertEquals(TipoEventoEntrega.RUTA_ASIGNADA, captor.getValue().getTipo());
    assertEquals(rutaId, captor.getValue().getIdRuta());
    assertEquals(entrega.getId(), captor.getValue().getIdEntrega());
    assertEquals(entrega.getIdDonacion(), captor.getValue().getIdDonacion());

    // EntregaConfirmada
    EntregaConfirmada evExito =
        new EntregaConfirmada(entrega.getId(), entrega.getIdDonacion(), rutaId);
    adapter.registrarEntregaExitosa(evExito);
    verify(springDataRepo, org.mockito.Mockito.times(2)).save(captor.capture());
    assertEquals(TipoEventoEntrega.ENTREGA_EXITOSA, captor.getValue().getTipo());
    assertEquals(entrega.getId(), captor.getValue().getIdEntrega());
    assertEquals(rutaId, captor.getValue().getIdRuta());

    // EntregaFallida
    EntregaFallida evFallo =
        new EntregaFallida(entrega.getId(), entrega.getIdDonacion(), "No estaba", true);
    adapter.registrarEntregaFallida(evFallo);
    verify(springDataRepo, org.mockito.Mockito.times(3)).save(captor.capture());
    assertEquals(TipoEventoEntrega.ENTREGA_FALLIDA, captor.getValue().getTipo());
    assertEquals("No estaba", captor.getValue().getJustificacion());
    assertTrue(captor.getValue().getReplanificable());
    assertNull(captor.getValue().getIdRuta());
  }
}
