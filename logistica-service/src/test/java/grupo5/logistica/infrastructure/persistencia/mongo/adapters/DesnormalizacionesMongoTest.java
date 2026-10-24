package grupo5.logistica.infrastructure.persistencia.mongo.adapters; // NOSONAR

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.common.testing.DisabledIfDockerUnavailable;
import grupo5.logistica.infrastructure.persistencia.mongo.MongoDePrueba;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.AsignacionSubdocument;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.CamionDocument;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.EntregaDocument;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.ParadaSubdocument;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.RutaDocument;
import grupo5.logistica.infrastructure.persistencia.mongo.mappers.CamionMongoMapper;
import grupo5.logistica.infrastructure.persistencia.mongo.mappers.EntregaMongoMapper;
import grupo5.logistica.infrastructure.persistencia.mongo.mappers.RutaMongoMapper;
import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.rutas.Ruta;
import grupo5.logistica.testutils.EntregaMother;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/** Asignación embebida en la entrega y paradas embebidas en la ruta, contra un MongoDB real. */
@DisabledIfDockerUnavailable
@ActiveProfiles("mongo")
@SpringJUnitConfig(
    classes = DesnormalizacionesMongoTest.Contexto.class,
    initializers = ConfigDataApplicationContextInitializer.class)
class DesnormalizacionesMongoTest {

  private static final LocalDate FECHA_RUTA = LocalDate.of(2030, 1, 1);
  private static final String PATENTE = "AB123CD";

  @DynamicPropertySource
  static void propiedades(DynamicPropertyRegistry registry) {
    MongoDePrueba.conectar(registry, "desnormalizaciones");
  }

  @Import({
    MongoDePrueba.Infraestructura.class,
    CamionRepositoryMongoAdapter.class,
    EntregasRepositoryMongoAdapter.class,
    RutasRepositoryMongoAdapter.class,
    CamionMongoMapper.class,
    EntregaMongoMapper.class,
    RutaMongoMapper.class
  })
  static class Contexto {

    @Bean
    Clock reloj() {
      return Clock.systemUTC();
    }
  }

  @Autowired MongoTemplate mongoTemplate;
  @Autowired CamionRepositoryMongoAdapter camiones;
  @Autowired EntregasRepositoryMongoAdapter entregas;
  @Autowired RutasRepositoryMongoAdapter rutas;

  private Camion camion;

  @BeforeEach
  void limpiarYCrearCamion() {
    MongoDePrueba.vaciar(mongoTemplate);
    camion = camiones.save(new Camion(PATENTE, 20f, 5000f, 3f));
  }

  @Test
  void alAsignarUnaEntrega_quedaLaAsignacionCompletaEnElMismoDocumento() {
    Entrega entrega = entregas.save(EntregaMother.pendiente());
    Ruta ruta = rutaCon(camion, entrega);

    asignarYGuardar(ruta, entrega);

    AsignacionSubdocument asignacion = documentoDe(entrega).getAsignacion();
    assertEquals(ruta.getId(), asignacion.getIdRuta());
    assertEquals(FECHA_RUTA, asignacion.getFecha());
    assertEquals(camion.getId(), asignacion.getIdCamion());
    assertEquals(PATENTE, asignacion.getPatente());
  }

  @Test
  void laAsignacionConservaLaPatenteDelDiaAunqueDespuesCambie() {
    Entrega entrega = entregas.save(EntregaMother.pendiente());
    asignarYGuardar(rutaCon(camion, entrega), entrega);
    cambiarPatenteEnBase("ZZ999ZZ");
    Entrega asignada = entregas.findById(entrega.getId()).orElseThrow();
    asignada.iniciarRuta("Chofer");

    entregas.save(asignada);

    assertEquals(PATENTE, documentoDe(entrega).getAsignacion().getPatente());
  }

  @Test
  void unaEntregaQueCambiaDeRuta_tomaLaAsignacionDeLaRutaNueva() {
    Entrega entrega = entregas.save(EntregaMother.pendiente());
    asignarYGuardar(rutaCon(camion, entrega), entrega);
    Camion otroCamion = camiones.save(new Camion("XY987ZW", 20f, 5000f, 3f));
    Ruta rutaNueva = rutaCon(otroCamion, entrega);
    rutas.save(rutaNueva);
    Entrega reasignada = regresadaAlDeposito(entrega);
    reasignada.asignarRuta(rutaNueva.getId());

    entregas.save(reasignada);

    AsignacionSubdocument asignacion = documentoDe(entrega).getAsignacion();
    assertEquals(rutaNueva.getId(), asignacion.getIdRuta());
    assertEquals("XY987ZW", asignacion.getPatente());
  }

  @Test
  void guardarUnaEntregaAsignadaAUnaRutaInexistente_lanzaRecursoNoEncontrado() {
    Entrega entrega = entregas.save(EntregaMother.pendiente());
    UUID rutaInexistente = UUID.randomUUID();
    entrega.asignarRuta(rutaInexistente);

    RecursoNoEncontradoException error =
        assertThrows(RecursoNoEncontradoException.class, () -> entregas.save(entrega));

    assertEquals(rutaInexistente, error.getId());
  }

  @Test
  void guardarUnaEntregaDeUnaRutaConCamionInexistente_lanzaRecursoNoEncontrado() {
    Entrega entrega = entregas.save(EntregaMother.pendiente());
    UUID camionInexistente = UUID.randomUUID();
    Ruta ruta = new Ruta(FECHA_RUTA, UUID.randomUUID(), camionInexistente);
    ruta.agregarEntrega(entrega.getId());
    rutas.save(ruta);
    entrega.asignarRuta(ruta.getId());

    RecursoNoEncontradoException error =
        assertThrows(RecursoNoEncontradoException.class, () -> entregas.save(entrega));

    assertEquals(camionInexistente, error.getId());
  }

  @Test
  void guardarUnaRutaConUnaEntregaInexistente_lanzaRecursoNoEncontrado() {
    UUID entregaInexistente = UUID.randomUUID();
    Ruta ruta = new Ruta(FECHA_RUTA, UUID.randomUUID(), camion.getId());
    ruta.agregarEntrega(entregaInexistente);

    RecursoNoEncontradoException error =
        assertThrows(RecursoNoEncontradoException.class, () -> rutas.save(ruta));

    assertEquals(entregaInexistente, error.getId());
  }

  @Test
  void lasParadasSeNumeranEnElOrdenDeLaRuta() {
    Ruta ruta =
        rutaCon(
            camion,
            entregas.save(EntregaMother.pendiente()),
            entregas.save(EntregaMother.pendiente()),
            entregas.save(EntregaMother.pendiente()));

    rutas.save(ruta);

    assertEquals(
        List.of(1, 2, 3), paradasDe(ruta).stream().map(ParadaSubdocument::getOrden).toList());
  }

  @Test
  void cadaParadaCopiaLaDonacionYElDestinoDeSuEntrega() {
    Entrega entrega = entregas.save(EntregaMother.pendiente());
    Ruta ruta = rutaCon(camion, entrega);

    rutas.save(ruta);

    ParadaSubdocument parada = paradasDe(ruta).getFirst();
    assertEquals(entrega.getIdDonacion(), parada.getIdDonacion());
    assertEquals(entrega.getDestino().calle(), parada.getDestino().getCalle());
  }

  @Test
  void laRutaLeidaDevuelveLasEntregasEnElOrdenDeSusParadas() {
    Entrega primera = entregas.save(EntregaMother.pendiente());
    Entrega segunda = entregas.save(EntregaMother.pendiente());
    Entrega tercera = entregas.save(EntregaMother.pendiente());
    Ruta ruta = rutaCon(camion, primera, segunda, tercera);
    rutas.save(ruta);

    Ruta leida = rutas.findById(ruta.getId()).orElseThrow();

    assertEquals(List.of(primera.getId(), segunda.getId(), tercera.getId()), leida.getEntregaIds());
  }

  @Test
  void unaEntregaQueRegresaAlDeposito_quedaSinAsignacion() {
    Entrega entrega = entregas.save(EntregaMother.pendiente());
    asignarYGuardar(rutaCon(camion, entrega), entrega);

    entregas.save(regresadaAlDeposito(entrega));

    assertNull(documentoDe(entrega).getAsignacion());
  }

  @Test
  void unaEntregaQueRegresaAlDeposito_vuelveAEstarEntreLasSinRuta() {
    Entrega entrega = entregas.save(EntregaMother.pendiente());
    asignarYGuardar(rutaCon(camion, entrega), entrega);

    entregas.save(regresadaAlDeposito(entrega));

    assertEquals(List.of(entrega.getId()), idsDe(entregas.findSinRuta()));
  }

  @Test
  void unaEntregaQueRegresaAlDeposito_dejaDeEncontrarsePorSuRuta() {
    Entrega entrega = entregas.save(EntregaMother.pendiente());
    Ruta ruta = rutaCon(camion, entrega);
    asignarYGuardar(ruta, entrega);

    entregas.save(regresadaAlDeposito(entrega));

    assertEquals(List.of(), entregas.findByRutaId(ruta.getId()));
  }

  @Test
  void unaEntregaQueRegresaAlDeposito_noSacaLaParadaDeLaRuta() {
    Entrega entrega = entregas.save(EntregaMother.pendiente());
    Ruta ruta = rutaCon(camion, entrega);
    asignarYGuardar(ruta, entrega);

    entregas.save(regresadaAlDeposito(entrega));

    assertEquals(
        List.of(entrega.getId()), rutas.findById(ruta.getId()).orElseThrow().getEntregaIds());
  }

  @Test
  void findByRutaId_buscaPorLaAsignacionEmbebida() {
    Entrega entrega = entregas.save(EntregaMother.pendiente());
    Ruta ruta = rutaCon(camion, entrega);

    asignarYGuardar(ruta, entrega);

    assertEquals(List.of(entrega.getId()), idsDe(entregas.findByRutaId(ruta.getId())));
  }

  @Test
  void unaEntregaAsignada_noEstaEntreLasSinRuta() {
    Entrega entrega = entregas.save(EntregaMother.pendiente());

    asignarYGuardar(rutaCon(camion, entrega), entrega);

    assertEquals(List.of(), entregas.findSinRuta());
  }

  private Ruta rutaCon(Camion camionDeLaRuta, Entrega... entregasDeLaRuta) {
    Ruta ruta = new Ruta(FECHA_RUTA, UUID.randomUUID(), camionDeLaRuta.getId());
    for (Entrega entrega : entregasDeLaRuta) {
      ruta.agregarEntrega(entrega.getId());
    }
    return ruta;
  }

  /** Mismo orden que la planificación: primero la ruta, después la entrega. */
  private void asignarYGuardar(Ruta ruta, Entrega entrega) {
    rutas.save(ruta);
    Entrega guardada = entregas.findById(entrega.getId()).orElseThrow();
    guardada.asignarRuta(ruta.getId());
    entregas.save(guardada);
  }

  private Entrega regresadaAlDeposito(Entrega entrega) {
    Entrega enRuta = entregas.findById(entrega.getId()).orElseThrow();
    enRuta.iniciarRuta("Chofer");
    enRuta.negarEntrega("Entidad", "No había nadie", true);
    enRuta.mandarARevision("Admin");
    enRuta.regresarAlDeposito("Admin");
    return enRuta;
  }

  private EntregaDocument documentoDe(Entrega entrega) {
    return mongoTemplate.findById(entrega.getId(), EntregaDocument.class);
  }

  private List<ParadaSubdocument> paradasDe(Ruta ruta) {
    return mongoTemplate.findById(ruta.getId(), RutaDocument.class).getParadas();
  }

  private void cambiarPatenteEnBase(String patente) {
    mongoTemplate.updateFirst(
        Query.query(Criteria.where("_id").is(camion.getId())),
        Update.update("patente", patente),
        CamionDocument.class);
  }

  private static List<UUID> idsDe(List<Entrega> lista) {
    return lista.stream().map(Entrega::getId).toList();
  }
}
