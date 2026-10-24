package grupo5.logistica.infrastructure.persistencia.mongo.adapters; // NOSONAR

import static grupo5.logistica.models.entities.rutas.EstadoRuta.EN_TRASLADO;
import static java.time.ZoneOffset.UTC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
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
import grupo5.logistica.models.entities.entregas.eventos.EntregaConfirmada;
import grupo5.logistica.models.entities.entregas.eventos.EntregaFallida;
import grupo5.logistica.models.entities.rutas.Ruta;
import grupo5.logistica.models.entities.rutas.eventos.EventoRutaAsignada;
import grupo5.logistica.models.entities.solicitudes.SolicitudPlanificacion;
import grupo5.logistica.testutils.EntregaMother;
import grupo5.logistica.testutils.SolicitudPlanificacionMother;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@SuppressWarnings("java:S120")
@ExtendWith(MockitoExtension.class)
class RepositoriosMongoAdaptersTest {

  private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-10-24T13:00:00Z"), UTC);
  private static final LocalDate FECHA = LocalDate.of(2026, 10, 12);

  @Mock SpringDataCamionMongoRepository camionRepo;
  @Mock SpringDataChoferMongoRepository choferRepo;
  @Mock SpringDataRutaMongoRepository rutaRepo;
  @Mock SpringDataEntregaMongoRepository entregaRepo;
  @Mock SpringDataSolicitudPlanificacionMongoRepository solicitudRepo;
  @Mock SpringDataSolicitudTransicionEntregaMongoRepository transicionRepo;
  @Mock SpringDataEventoEntregaMongoRepository eventoRepo;
  @Captor ArgumentCaptor<SolicitudTransicionEntregaDocument> documentoDeTransicion;
  @Captor ArgumentCaptor<EventoEntregaDocument> documentoDeEvento;

  private CamionMongoMapper camionMapper;
  private ChoferMongoMapper choferMapper;
  private RutaMongoMapper rutaMapper;
  private EntregaMongoMapper entregaMapper;
  private SolicitudPlanificacionMongoMapper solicitudMapper;

  private CamionRepositoryMongoAdapter camiones;
  private ChoferesRepositoryMongoAdapter choferes;
  private RutasRepositoryMongoAdapter rutas;
  private EntregasRepositoryMongoAdapter entregas;
  private SolicitudPlanificacionRepositoryMongoAdapter solicitudes;
  private SolicitudesTransicionEntregaRepositoryMongoAdapter transiciones;
  private EventosEntregaRepositoryMongoAdapter eventos;

  @BeforeEach
  void crearAdaptadores() {
    camionMapper = new CamionMongoMapper();
    choferMapper = new ChoferMongoMapper();
    rutaMapper = new RutaMongoMapper();
    entregaMapper = new EntregaMongoMapper(RELOJ);
    solicitudMapper = new SolicitudPlanificacionMongoMapper();
    camiones = new CamionRepositoryMongoAdapter(camionRepo, rutaRepo, camionMapper);
    choferes = new ChoferesRepositoryMongoAdapter(choferRepo, rutaRepo, choferMapper);
    rutas = new RutasRepositoryMongoAdapter(rutaRepo, entregaRepo, rutaMapper);
    entregas = new EntregasRepositoryMongoAdapter(entregaRepo, rutaRepo, camionRepo, entregaMapper);
    solicitudes = new SolicitudPlanificacionRepositoryMongoAdapter(solicitudRepo, solicitudMapper);
    transiciones = new SolicitudesTransicionEntregaRepositoryMongoAdapter(transicionRepo);
    eventos = new EventosEntregaRepositoryMongoAdapter(eventoRepo, RELOJ);
  }

  @Test
  void camionMongoAdapter_conEstadoOPatenteVacios_devuelveVacio() {
    assertTrue(camiones.findByEstado(null).isEmpty());
    assertTrue(camiones.findByPatente(null).isEmpty());
    assertTrue(camiones.findByPatente("   ").isEmpty());
  }

  @Test
  void camionMongoAdapter_findByEstado_devuelveLosCamionesDeEseEstado() {
    CamionDocument doc = camionMapper.toDocument(nuevoCamion());
    when(camionRepo.findByEstado(EstadoCamion.DISPONIBLE)).thenReturn(List.of(doc));

    assertEquals(1, camiones.findByEstado(EstadoCamion.DISPONIBLE).size());
  }

  @Test
  void camionMongoAdapter_findActivos_excluyeLosDeshabilitados() {
    CamionDocument doc = camionMapper.toDocument(nuevoCamion());
    when(camionRepo.findByEstadoNot(EstadoCamion.DESHABILITADO)).thenReturn(List.of(doc));

    assertEquals(1, camiones.findActivos().size());
  }

  @Test
  void camionMongoAdapter_findDisponibles_devuelveLosDisponiblesSinRutaEnTraslado() {
    CamionDocument doc = camionMapper.toDocument(nuevoCamion());
    when(camionRepo.findByEstado(EstadoCamion.DISPONIBLE)).thenReturn(List.of(doc));

    assertEquals(1, camiones.findDisponibles().size());
  }

  @Test
  void camionMongoAdapter_findByPatente_buscaConLaPatenteNormalizada() {
    CamionDocument doc = camionMapper.toDocument(nuevoCamion());
    when(camionRepo.findByPatente("AB123CD")).thenReturn(Optional.of(doc));

    assertTrue(camiones.findByPatente("ab-123-cd").isPresent());
  }

  @Test
  void camionMongoAdapter_unCamionEnRuta_seLeeConSuRutaEnTraslado() {
    Camion camion = nuevoCamion();
    UUID rutaId = UUID.randomUUID();
    camion.asignarARuta(rutaId);
    CamionDocument doc = camionMapper.toDocument(camion);
    Optional<RutaDocument> ruta = rutaConId(rutaId);
    when(camionRepo.findByEstado(EstadoCamion.EN_RUTA)).thenReturn(List.of(doc));
    when(rutaRepo.findFirstByIdCamionAndEstado(camion.getId(), EN_TRASLADO)).thenReturn(ruta);

    Camion leido = camiones.findByEstado(EstadoCamion.EN_RUTA).getFirst();

    assertEquals(rutaId, leido.getRutaId());
  }

  @Test
  void choferMongoAdapter_findActivos_excluyeLosDeshabilitados() {
    ChoferDocument doc = choferMapper.toDocument(nuevoChofer());
    when(choferRepo.findByEstadoNot(EstadoChofer.DESHABILITADO)).thenReturn(List.of(doc));

    assertEquals(1, choferes.findActivos().size());
  }

  @Test
  void choferMongoAdapter_findDisponibles_devuelveLosDisponiblesSinRutaEnTraslado() {
    ChoferDocument doc = choferMapper.toDocument(nuevoChofer());
    when(choferRepo.findByEstado(EstadoChofer.DISPONIBLE)).thenReturn(List.of(doc));

    assertEquals(1, choferes.findDisponibles().size());
  }

  @Test
  void choferMongoAdapter_unChoferEnRuta_seLeeConSuRutaEnTraslado() {
    Chofer chofer = nuevoChofer();
    UUID rutaId = UUID.randomUUID();
    chofer.asignarARuta(rutaId);
    ChoferDocument doc = choferMapper.toDocument(chofer);
    Optional<RutaDocument> ruta = rutaConId(rutaId);
    when(choferRepo.findByEstadoNot(EstadoChofer.DESHABILITADO)).thenReturn(List.of(doc));
    when(rutaRepo.findFirstByIdChoferAndEstado(chofer.getId(), EN_TRASLADO)).thenReturn(ruta);

    Chofer leido = choferes.findActivos().getFirst();

    assertEquals(rutaId, leido.getRutaId());
  }

  @Test
  void rutasMongoAdapter_conArgumentosNulos_devuelveVacio() {
    assertNull(rutas.save(null));
    assertTrue(rutas.saveAll(null).isEmpty());
    assertTrue(rutas.findByFecha(null).isEmpty());
    assertTrue(rutas.findByCamionId(null).isEmpty());
    assertTrue(rutas.findByCamionIdAndFecha(null, FECHA).isEmpty());
    assertTrue(rutas.findByCamionIdAndFecha(UUID.randomUUID(), null).isEmpty());
  }

  @Test
  void rutasMongoAdapter_alGuardar_devuelveLaRutaConSusEntregas() {
    UUID entregaId = UUID.randomUUID();
    Ruta ruta = nuevaRuta();
    ruta.agregarEntrega(entregaId);
    when(entregaRepo.findAllById(List.of(entregaId))).thenReturn(List.of(entregaConId(entregaId)));
    when(rutaRepo.save(any(RutaDocument.class))).thenAnswer(returnsFirstArg());

    Ruta guardada = rutas.save(ruta);

    assertEquals(ruta.getId(), guardada.getId());
    assertEquals(List.of(entregaId), guardada.getEntregaIds());
  }

  @Test
  void rutasMongoAdapter_saveAll_guardaCadaRuta() {
    when(rutaRepo.save(any(RutaDocument.class))).thenAnswer(returnsFirstArg());

    assertEquals(1, rutas.saveAll(List.of(nuevaRuta())).size());
  }

  @Test
  void rutasMongoAdapter_findByFecha_devuelveLasRutasDelDia() {
    RutaDocument doc = rutaMapper.toDocument(nuevaRuta());
    when(rutaRepo.findByFecha(FECHA)).thenReturn(List.of(doc));

    assertEquals(1, rutas.findByFecha(FECHA).size());
  }

  @Test
  void rutasMongoAdapter_findByCamionId_devuelveLasRutasDelCamion() {
    Ruta ruta = nuevaRuta();
    RutaDocument doc = rutaMapper.toDocument(ruta);
    when(rutaRepo.findByIdCamion(ruta.getCamionId())).thenReturn(List.of(doc));

    assertEquals(1, rutas.findByCamionId(ruta.getCamionId()).size());
  }

  @Test
  void rutasMongoAdapter_findByCamionIdAndFecha_devuelveLasRutasDelCamionEseDia() {
    Ruta ruta = nuevaRuta();
    RutaDocument doc = rutaMapper.toDocument(ruta);
    when(rutaRepo.findByIdCamionAndFecha(ruta.getCamionId(), FECHA)).thenReturn(List.of(doc));

    assertEquals(1, rutas.findByCamionIdAndFecha(ruta.getCamionId(), FECHA).size());
  }

  @Test
  void entregasMongoAdapter_conArgumentosNulos_devuelveVacio() {
    assertTrue(entregas.findByEstado(null).isEmpty());
    assertTrue(entregas.findByRutaId(null).isEmpty());
    assertFalse(entregas.existsByIdDonacion(null));
    assertTrue(entregas.findByIdDonacion(null).isEmpty());
    assertTrue(entregas.findByIdBeneficiaria(null).isEmpty());
  }

  @Test
  void entregasMongoAdapter_findByEstado_devuelveLasEntregasDeEseEstado() {
    EntregaDocument doc = entregaMapper.toDocument(EntregaMother.pendiente());
    when(entregaRepo.findByEstado(EstadoEntrega.PENDIENTE)).thenReturn(List.of(doc));

    assertEquals(1, entregas.findByEstado(EstadoEntrega.PENDIENTE).size());
  }

  @Test
  void entregasMongoAdapter_findByRutaId_buscaPorLaRutaDeLaAsignacion() {
    UUID rutaId = UUID.randomUUID();
    EntregaDocument doc = entregaMapper.toDocument(EntregaMother.pendiente());
    when(entregaRepo.findByAsignacionIdRutaOrderByIdAsc(rutaId)).thenReturn(List.of(doc));

    assertEquals(1, entregas.findByRutaId(rutaId).size());
  }

  @Test
  void entregasMongoAdapter_findSinRuta_devuelveLasEntregasSinAsignacion() {
    EntregaDocument doc = entregaMapper.toDocument(EntregaMother.pendiente());
    when(entregaRepo.findByAsignacionIsNull()).thenReturn(List.of(doc));

    assertEquals(1, entregas.findSinRuta().size());
  }

  @Test
  void entregasMongoAdapter_existsByIdDonacion_consultaPorLaDonacion() {
    UUID idDonacion = UUID.randomUUID();
    when(entregaRepo.existsByIdDonacion(idDonacion)).thenReturn(true);

    assertTrue(entregas.existsByIdDonacion(idDonacion));
  }

  @Test
  void entregasMongoAdapter_findByIdDonacion_devuelveLaEntregaDeEsaDonacion() {
    Entrega entrega = EntregaMother.pendiente();
    UUID idDonacion = entrega.getIdDonacion();
    EntregaDocument doc = entregaMapper.toDocument(entrega);
    when(entregaRepo.findByIdDonacion(idDonacion)).thenReturn(Optional.of(doc));

    assertEquals(entrega.getId(), entregas.findByIdDonacion(idDonacion).orElseThrow().getId());
  }

  @Test
  void entregasMongoAdapter_findByIdBeneficiaria_devuelveLasEntregasDeLaBeneficiaria() {
    Entrega entrega = EntregaMother.pendiente();
    UUID idBeneficiaria = entrega.getIdBeneficiaria();
    EntregaDocument doc = entregaMapper.toDocument(entrega);
    when(entregaRepo.findByIdBeneficiaria(idBeneficiaria)).thenReturn(List.of(doc));

    assertEquals(1, entregas.findByIdBeneficiaria(idBeneficiaria).size());
  }

  @Test
  void solicitudPlanificacionMongoAdapter_conArgumentosNulos_devuelveVacio() {
    assertNull(solicitudes.save(null));
    assertTrue(solicitudes.saveAll(null).isEmpty());
    assertTrue(solicitudes.findById(null).isEmpty());
    assertFalse(solicitudes.existsById(null));
  }

  @Test
  void solicitudPlanificacionMongoAdapter_save_devuelveLaSolicitudGuardada() {
    SolicitudPlanificacion solicitud = SolicitudPlanificacionMother.pendiente();
    when(solicitudRepo.save(any(SolicitudPlanificacionDocument.class)))
        .thenAnswer(returnsFirstArg());

    assertEquals(solicitud.getId(), solicitudes.save(solicitud).getId());
  }

  @Test
  void solicitudPlanificacionMongoAdapter_saveAll_guardaCadaSolicitud() {
    SolicitudPlanificacion solicitud = SolicitudPlanificacionMother.pendiente();
    when(solicitudRepo.save(any(SolicitudPlanificacionDocument.class)))
        .thenAnswer(returnsFirstArg());

    assertEquals(1, solicitudes.saveAll(List.of(solicitud)).size());
  }

  @Test
  void solicitudPlanificacionMongoAdapter_findAll_devuelveTodasLasSolicitudes() {
    SolicitudPlanificacion solicitud = SolicitudPlanificacionMother.pendiente();
    SolicitudPlanificacionDocument doc = solicitudMapper.toDocument(solicitud);
    when(solicitudRepo.findAll()).thenReturn(List.of(doc));

    assertEquals(1, solicitudes.findAll().size());
  }

  @Test
  void solicitudPlanificacionMongoAdapter_findById_devuelveLaSolicitudGuardada() {
    SolicitudPlanificacion solicitud = SolicitudPlanificacionMother.pendiente();
    SolicitudPlanificacionDocument doc = solicitudMapper.toDocument(solicitud);
    when(solicitudRepo.findById(solicitud.getId())).thenReturn(Optional.of(doc));

    assertTrue(solicitudes.findById(solicitud.getId()).isPresent());
  }

  @Test
  void solicitudPlanificacionMongoAdapter_existsById_consultaPorId() {
    UUID id = UUID.randomUUID();
    when(solicitudRepo.existsById(id)).thenReturn(true);

    assertTrue(solicitudes.existsById(id));
  }

  @Test
  void solicitudPlanificacionMongoAdapter_count_devuelveLaCantidadGuardada() {
    when(solicitudRepo.count()).thenReturn(1L);

    assertEquals(1L, solicitudes.count());
  }

  @Test
  void solicitudPlanificacionMongoAdapter_delete_borraPorId() {
    SolicitudPlanificacion solicitud = SolicitudPlanificacionMother.pendiente();

    solicitudes.delete(solicitud);

    verify(solicitudRepo).deleteById(solicitud.getId());
  }

  @Test
  void solicitudPlanificacionMongoAdapter_deleteAll_borraTodo() {
    solicitudes.deleteAll();

    verify(solicitudRepo).deleteAll();
  }

  @Test
  void solicitudesTransicionEntregaMongoAdapter_sinSolicitud_noGuardaNada() {
    transiciones.registrar(null);

    verify(transicionRepo, never()).save(any());
  }

  @Test
  void solicitudesTransicionEntregaMongoAdapter_registraLaConfirmacionConSusFotos() {
    Entrega entrega = EntregaMother.pendiente();

    transiciones.registrar(new ConfirmacionRecepcion(entrega, "Actor1", "http://foto"));

    SolicitudTransicionEntregaDocument doc = transicionGuardada();
    assertEquals(TipoTransicionEntrega.CONFIRMAR_ENTREGA, doc.getTipoTransicion());
    assertEquals(List.of("http://foto"), doc.getFotosRecepcion());
    assertEquals(entrega.getId(), doc.getIdEntrega());
  }

  @Test
  void solicitudesTransicionEntregaMongoAdapter_registraLaNoRecepcionConSuJustificacion() {
    transiciones.registrar(new NoRecepcion(EntregaMother.pendiente(), "Actor2", "Cerrado", true));

    SolicitudTransicionEntregaDocument doc = transicionGuardada();
    assertEquals(TipoTransicionEntrega.NO_RECEPCION, doc.getTipoTransicion());
    assertEquals("Cerrado", doc.getJustificacion());
    assertTrue(doc.getReplanificable());
  }

  @Test
  void solicitudesTransicionEntregaMongoAdapter_registraLaRevision() {
    transiciones.registrar(new RevisionEntrega(EntregaMother.pendiente(), "Actor3"));

    assertEquals(TipoTransicionEntrega.REVISION, transicionGuardada().getTipoTransicion());
  }

  @Test
  void solicitudesTransicionEntregaMongoAdapter_registraElRegresoAlDeposito() {
    transiciones.registrar(new RegresoDeposito(EntregaMother.pendiente(), "Actor4"));

    assertEquals(TipoTransicionEntrega.REGRESO_DEPOSITO, transicionGuardada().getTipoTransicion());
  }

  @Test
  void eventosEntregaMongoAdapter_sinEventoOEntrega_noGuardaNada() {
    Entrega entrega = EntregaMother.pendiente();
    EventoRutaAsignada evento = new EventoRutaAsignada(UUID.randomUUID(), entrega.getId());

    eventos.registrarRutaAsignada(null, entrega);
    eventos.registrarRutaAsignada(evento, null);
    eventos.registrarEntregaExitosa(null);
    eventos.registrarEntregaFallida(null);

    verify(eventoRepo, never()).save(any());
  }

  @Test
  void eventosEntregaMongoAdapter_registraLaRutaAsignadaConLaDonacionDeLaEntrega() {
    Entrega entrega = EntregaMother.pendiente();
    UUID rutaId = UUID.randomUUID();

    eventos.registrarRutaAsignada(new EventoRutaAsignada(rutaId, entrega.getId()), entrega);

    EventoEntregaDocument doc = eventoGuardado();
    assertEquals(TipoEventoEntrega.RUTA_ASIGNADA, doc.getTipo());
    assertEquals(rutaId, doc.getIdRuta());
    assertEquals(entrega.getId(), doc.getIdEntrega());
    assertEquals(entrega.getIdDonacion(), doc.getIdDonacion());
  }

  @Test
  void eventosEntregaMongoAdapter_registraLaEntregaExitosaConSuRuta() {
    UUID idEntrega = UUID.randomUUID();
    UUID rutaId = UUID.randomUUID();

    eventos.registrarEntregaExitosa(new EntregaConfirmada(idEntrega, UUID.randomUUID(), rutaId));

    EventoEntregaDocument doc = eventoGuardado();
    assertEquals(TipoEventoEntrega.ENTREGA_EXITOSA, doc.getTipo());
    assertEquals(idEntrega, doc.getIdEntrega());
    assertEquals(rutaId, doc.getIdRuta());
  }

  @Test
  void eventosEntregaMongoAdapter_registraLaEntregaFallidaSinRuta() {
    eventos.registrarEntregaFallida(entregaFallida("No estaba"));

    EventoEntregaDocument doc = eventoGuardado();
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

    eventos.registrarEntregaExitosa(evento);

    Instant ocurrioEn = eventoGuardado().getOcurrioEn();
    assertFalse(ocurrioEn.isBefore(antes));
    assertFalse(ocurrioEn.isAfter(despues));
  }

  @Test
  void eventosEntregaMongoAdapter_sinTimestamp_usaLaHoraDelReloj() {
    EntregaFallida eventoSinTimestamp = mock(EntregaFallida.class);

    eventos.registrarEntregaFallida(eventoSinTimestamp);

    assertEquals(RELOJ.instant(), eventoGuardado().getOcurrioEn());
  }

  private SolicitudTransicionEntregaDocument transicionGuardada() {
    verify(transicionRepo).save(documentoDeTransicion.capture());
    return documentoDeTransicion.getValue();
  }

  private EventoEntregaDocument eventoGuardado() {
    verify(eventoRepo).save(documentoDeEvento.capture());
    return documentoDeEvento.getValue();
  }

  private static Camion nuevoCamion() {
    return new Camion("AB123CD", 50.0f, 2000.0f, 3.0f);
  }

  private static Chofer nuevoChofer() {
    return new Chofer("Luis", "Paz", "LIC-50", "1122334455");
  }

  private static Ruta nuevaRuta() {
    return new Ruta(FECHA, UUID.randomUUID(), UUID.randomUUID());
  }

  private static Optional<RutaDocument> rutaConId(UUID id) {
    return Optional.of(RutaDocument.builder().id(id).build());
  }

  private static EntregaDocument entregaConId(UUID id) {
    return EntregaDocument.builder().id(id).build();
  }

  private static EntregaFallida entregaFallida(String justificacion) {
    return new EntregaFallida(UUID.randomUUID(), UUID.randomUUID(), justificacion, true);
  }
}
