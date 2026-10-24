package grupo5.logistica.infrastructure.persistencia.mongo; // NOSONAR

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import grupo5.common.testing.DisabledIfDockerUnavailable;
import grupo5.logistica.infrastructure.persistencia.mongo.adapters.CamionRepositoryMongoAdapter;
import grupo5.logistica.infrastructure.persistencia.mongo.adapters.ChoferesRepositoryMongoAdapter;
import grupo5.logistica.infrastructure.persistencia.mongo.adapters.EntregasRepositoryMongoAdapter;
import grupo5.logistica.infrastructure.persistencia.mongo.adapters.EventosEntregaRepositoryMongoAdapter;
import grupo5.logistica.infrastructure.persistencia.mongo.adapters.RutasRepositoryMongoAdapter;
import grupo5.logistica.infrastructure.persistencia.mongo.adapters.SolicitudPlanificacionRepositoryMongoAdapter;
import grupo5.logistica.infrastructure.persistencia.mongo.adapters.SolicitudesTransicionEntregaRepositoryMongoAdapter;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.EventoEntregaDocument;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.SolicitudTransicionEntregaDocument;
import grupo5.logistica.infrastructure.persistencia.mongo.mappers.CamionMongoMapper;
import grupo5.logistica.infrastructure.persistencia.mongo.mappers.ChoferMongoMapper;
import grupo5.logistica.infrastructure.persistencia.mongo.mappers.EntregaMongoMapper;
import grupo5.logistica.infrastructure.persistencia.mongo.mappers.RutaMongoMapper;
import grupo5.logistica.infrastructure.persistencia.mongo.mappers.SolicitudPlanificacionMongoMapper;
import grupo5.logistica.models.entities.camiones.CambioEstadoCamion;
import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.camiones.EstadoCamion;
import grupo5.logistica.models.entities.choferes.Chofer;
import grupo5.logistica.models.entities.entregas.CambioEstadoEntrega;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.entregas.EstadoEntrega;
import grupo5.logistica.models.entities.entregas.NoRecepcion;
import grupo5.logistica.models.entities.entregas.RevisionEntrega;
import grupo5.logistica.models.entities.entregas.eventos.EntregaConfirmada;
import grupo5.logistica.models.entities.entregas.eventos.EntregaFallida;
import grupo5.logistica.models.entities.rutas.Ruta;
import grupo5.logistica.models.entities.solicitudes.SolicitudPlanificacion;
import grupo5.logistica.testutils.CamionMother;
import grupo5.logistica.testutils.ChoferMother;
import grupo5.logistica.testutils.EntregaMother;
import grupo5.logistica.testutils.SolicitudPlanificacionMother;
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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/** Los adaptadores Mongo de los cinco agregados y las dos auditorías, contra un MongoDB real. */
@DisabledIfDockerUnavailable
@ActiveProfiles("mongo")
@SpringJUnitConfig(
    classes = RepositoriosLogisticaMongoTest.Contexto.class,
    initializers = ConfigDataApplicationContextInitializer.class)
class RepositoriosLogisticaMongoTest {

  private static final LocalDate FECHA_RUTA = LocalDate.of(2030, 1, 1);

  @DynamicPropertySource
  static void propiedades(DynamicPropertyRegistry registry) {
    MongoDePrueba.conectar(registry, "repositorios");
  }

  @Import({
    MongoDePrueba.Infraestructura.class,
    CamionRepositoryMongoAdapter.class,
    ChoferesRepositoryMongoAdapter.class,
    EntregasRepositoryMongoAdapter.class,
    RutasRepositoryMongoAdapter.class,
    SolicitudPlanificacionRepositoryMongoAdapter.class,
    SolicitudesTransicionEntregaRepositoryMongoAdapter.class,
    EventosEntregaRepositoryMongoAdapter.class,
    CamionMongoMapper.class,
    ChoferMongoMapper.class,
    EntregaMongoMapper.class,
    RutaMongoMapper.class,
    SolicitudPlanificacionMongoMapper.class
  })
  static class Contexto {

    @Bean
    Clock reloj() {
      return Clock.systemUTC();
    }
  }

  @Autowired MongoTemplate mongoTemplate;
  @Autowired CamionRepositoryMongoAdapter camiones;
  @Autowired ChoferesRepositoryMongoAdapter choferes;
  @Autowired EntregasRepositoryMongoAdapter entregas;
  @Autowired RutasRepositoryMongoAdapter rutas;
  @Autowired SolicitudPlanificacionRepositoryMongoAdapter solicitudes;
  @Autowired SolicitudesTransicionEntregaRepositoryMongoAdapter transiciones;
  @Autowired EventosEntregaRepositoryMongoAdapter eventos;

  @BeforeEach
  void vaciarBase() {
    MongoDePrueba.vaciar(mongoTemplate);
  }

  @Test
  void unCamionGuardadoSeLeeConSusDatos() {
    Camion camion = camiones.save(CamionMother.disponible());

    Camion leido = camiones.findById(camion.getId()).orElseThrow();

    assertEquals(camion.getPatente(), leido.getPatente());
    assertEquals(camion.getCapacidadKG(), leido.getCapacidadKG());
    assertEquals(EstadoCamion.DISPONIBLE, leido.getEstado());
  }

  @Test
  void unChoferGuardadoSeLeeConSusDatos() {
    Chofer chofer = choferes.save(ChoferMother.disponible());

    Chofer leido = choferes.findById(chofer.getId()).orElseThrow();

    assertEquals(chofer.getLicencia(), leido.getLicencia());
    assertEquals(chofer.getTelefonoContacto(), leido.getTelefonoContacto());
  }

  @Test
  void unaEntregaGuardadaSeLeeConSuDestino() {
    Entrega entrega = entregas.save(EntregaMother.pendiente());

    Entrega leida = entregas.findById(entrega.getId()).orElseThrow();

    assertEquals(entrega.getIdDonacion(), leida.getIdDonacion());
    assertEquals(entrega.getDestino(), leida.getDestino());
  }

  @Test
  void unaRutaGuardadaSeLeeConSuCamionYSusEntregas() {
    Entrega entrega = entregas.save(EntregaMother.pendiente());
    Ruta ruta = rutaCon(UUID.randomUUID(), UUID.randomUUID(), entrega);
    rutas.save(ruta);

    Ruta leida = rutas.findById(ruta.getId()).orElseThrow();

    assertEquals(ruta.getCamionId(), leida.getCamionId());
    assertEquals(List.of(entrega.getId()), leida.getEntregaIds());
  }

  @Test
  void unaSolicitudDePlanificacionGuardadaSeLeeConSuEstado() {
    SolicitudPlanificacion solicitud = solicitudes.save(SolicitudPlanificacionMother.pendiente());

    SolicitudPlanificacion leida = solicitudes.findById(solicitud.getId()).orElseThrow();

    assertEquals(solicitud.getEstado(), leida.getEstado());
    assertEquals(solicitud.getCallbackUrl(), leida.getCallbackUrl());
  }

  @Test
  void elHistorialDelCamionCreceSinPerderLosCambiosAnteriores() {
    Camion camion = camiones.save(CamionMother.disponible());
    camion.deshabilitar();
    Camion deshabilitado = camiones.save(camion);
    deshabilitado.habilitar();
    camiones.save(deshabilitado);

    List<CambioEstadoCamion> historial =
        camiones.findById(camion.getId()).orElseThrow().getHistorialEstado();

    assertEquals(
        List.of(EstadoCamion.DESHABILITADO, EstadoCamion.DISPONIBLE),
        historial.stream().map(CambioEstadoCamion::estadoNuevo).toList());
  }

  @Test
  void elHistorialDeLaEntregaCreceSinPerderLosCambiosAnteriores() {
    Entrega entrega = entregas.save(EntregaMother.pendiente());
    entrega.iniciarRuta("Chofer");
    Entrega enTraslado = entregas.save(entrega);
    enTraslado.negarEntrega("Entidad", "No había nadie", true);
    entregas.save(enTraslado);

    List<CambioEstadoEntrega> historial =
        entregas.findById(entrega.getId()).orElseThrow().getHistorialEstado();

    assertEquals(
        List.of(EstadoEntrega.EN_TRASLADO, EstadoEntrega.NO_RECIBIDA),
        historial.stream().map(CambioEstadoEntrega::estadoNuevo).toList());
  }

  @Test
  void unCamionEnUnaRutaEnTraslado_seLeeConElIdDeEsaRuta() {
    Camion camion = camiones.save(CamionMother.disponible());
    Ruta ruta = rutaEnTrasladoCon(camion.getId(), UUID.randomUUID());
    camion.asignarARuta(ruta.getId());
    camiones.save(camion);

    assertEquals(ruta.getId(), camiones.findById(camion.getId()).orElseThrow().getRutaId());
  }

  @Test
  void unChoferEnUnaRutaEnTraslado_seLeeConElIdDeEsaRuta() {
    Chofer chofer = choferes.save(ChoferMother.disponible());
    Ruta ruta = rutaEnTrasladoCon(UUID.randomUUID(), chofer.getId());
    chofer.asignarARuta(ruta.getId());
    choferes.save(chofer);

    assertEquals(ruta.getId(), choferes.findById(chofer.getId()).orElseThrow().getRutaId());
  }

  @Test
  void dosEntregasDeLaMismaDonacion_violanElIndiceUnico() {
    UUID idDonacion = UUID.randomUUID();
    entregas.save(entregaDeLaDonacion(idDonacion));
    Entrega repetida = entregaDeLaDonacion(idDonacion);

    assertThrows(DuplicateKeyException.class, () -> entregas.save(repetida));
  }

  @Test
  void dosCamionesConLaMismaPatente_violanElIndiceUnico() {
    camiones.save(new Camion("AA111AA", 20f, 5000f, 3f));
    Camion repetido = new Camion("AA111AA", 20f, 5000f, 3f);

    assertThrows(DuplicateKeyException.class, () -> camiones.save(repetido));
  }

  @Test
  void guardarUnaCopiaDesactualizada_lanzaOptimisticLockingFailure() {
    Camion camion = camiones.save(CamionMother.disponible());
    Camion copiaVigente = camiones.findById(camion.getId()).orElseThrow();
    Camion copiaDesactualizada = camiones.findById(camion.getId()).orElseThrow();
    copiaVigente.deshabilitar();
    camiones.save(copiaVigente);
    copiaDesactualizada.deshabilitar();

    assertThrows(OptimisticLockingFailureException.class, () -> camiones.save(copiaDesactualizada));
  }

  @Test
  void cadaTransicionRegistradaAgregaUnDocumentoDeAuditoria() {
    Entrega entrega = entregas.save(EntregaMother.pendiente());

    transiciones.registrar(new NoRecepcion(entrega, "Chofer", "Ausente", true));
    transiciones.registrar(new RevisionEntrega(entrega, "Supervisor"));

    assertEquals(2, mongoTemplate.count(new Query(), SolicitudTransicionEntregaDocument.class));
  }

  @Test
  void cadaEventoRegistradoAgregaUnDocumentoDeAuditoria() {
    Entrega entrega = entregas.save(EntregaMother.pendiente());
    UUID idRuta = UUID.randomUUID();

    eventos.registrarEntregaExitosa(
        new EntregaConfirmada(entrega.getId(), entrega.getIdDonacion(), idRuta));
    eventos.registrarEntregaFallida(
        new EntregaFallida(entrega.getId(), entrega.getIdDonacion(), "No atendieron", true));

    assertEquals(2, mongoTemplate.count(new Query(), EventoEntregaDocument.class));
  }

  private Ruta rutaCon(UUID idCamion, UUID idChofer, Entrega... entregasDeLaRuta) {
    Ruta ruta = new Ruta(FECHA_RUTA, idChofer, idCamion);
    for (Entrega entrega : entregasDeLaRuta) {
      ruta.agregarEntrega(entrega.getId());
    }
    return ruta;
  }

  private Ruta rutaEnTrasladoCon(UUID idCamion, UUID idChofer) {
    Ruta ruta = rutaCon(idCamion, idChofer, entregas.save(EntregaMother.pendiente()));
    ruta.iniciarRuta();
    return rutas.save(ruta);
  }

  private static Entrega entregaDeLaDonacion(UUID idDonacion) {
    return new Entrega(idDonacion, UUID.randomUUID(), EntregaMother.direccion(), 10f, 2f);
  }
}
