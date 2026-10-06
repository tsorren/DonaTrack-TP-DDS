package grupo5.logistica.infrastructure.persistencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import grupo5.logistica.infrastructure.persistencia.entities.CamionEntity;
import grupo5.logistica.infrastructure.persistencia.entities.ChoferEntity;
import grupo5.logistica.infrastructure.persistencia.entities.DireccionEntity;
import grupo5.logistica.infrastructure.persistencia.entities.EntregaEntity;
import grupo5.logistica.infrastructure.persistencia.entities.EventoEntregaEntity;
import grupo5.logistica.infrastructure.persistencia.entities.LocalidadEntity;
import grupo5.logistica.infrastructure.persistencia.entities.PaisEntity;
import grupo5.logistica.infrastructure.persistencia.entities.ProvinciaEntity;
import grupo5.logistica.infrastructure.persistencia.entities.RutaEntity;
import grupo5.logistica.infrastructure.persistencia.entities.SolicitudPlanificacionEntity;
import grupo5.logistica.infrastructure.persistencia.entities.SolicitudTransicionEntregaEntity;
import grupo5.logistica.infrastructure.persistencia.entities.TipoEventoEntrega;
import grupo5.logistica.infrastructure.persistencia.entities.TipoTransicionEntrega;
import grupo5.logistica.infrastructure.persistencia.mappers.CamionPersistenciaMapper;
import grupo5.logistica.infrastructure.persistencia.mappers.ChoferPersistenciaMapper;
import grupo5.logistica.infrastructure.persistencia.mappers.EntregaPersistenciaMapper;
import grupo5.logistica.infrastructure.persistencia.mappers.RutaPersistenciaMapper;
import grupo5.logistica.infrastructure.persistencia.mappers.SolicitudPlanificacionPersistenciaMapper;
import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.camiones.EstadoCamion;
import grupo5.logistica.models.entities.choferes.Chofer;
import grupo5.logistica.models.entities.choferes.EstadoChofer;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.entregas.EstadoEntrega;
import grupo5.logistica.models.entities.rutas.Ruta;
import grupo5.logistica.models.entities.rutas.direccion.Direccion;
import grupo5.logistica.models.entities.rutas.direccion.Localidad;
import grupo5.logistica.models.entities.rutas.direccion.Pais;
import grupo5.logistica.models.entities.rutas.direccion.Provincia;
import grupo5.logistica.models.entities.solicitudes.EstadoSolicitud;
import grupo5.logistica.models.entities.solicitudes.SolicitudPlanificacion;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

class PersistenciaMappersTest {

  private final CamionPersistenciaMapper camionMapper = new CamionPersistenciaMapper();
  private final ChoferPersistenciaMapper choferMapper = new ChoferPersistenciaMapper();
  private final RutaPersistenciaMapper rutaMapper = new RutaPersistenciaMapper();
  private final EntregaPersistenciaMapper entregaMapper = new EntregaPersistenciaMapper();
  private final SolicitudPlanificacionPersistenciaMapper solicitudMapper =
      new SolicitudPlanificacionPersistenciaMapper();

  @Test
  void camionMapper_deberiaMapearIdaYVueltaYSincronizarHistorialAppendOnly() {
    assertNull(camionMapper.toEntity(null));
    assertNull(camionMapper.toDomain(null, null));

    Camion camion = new Camion("AB123CD", 45.0f, 1500.0f, 3.0f);
    camion.deshabilitar();
    CamionEntity entity = camionMapper.toEntity(camion);
    entity.setVersion(0L);

    UUID idCambioInicial = entity.getHistorialEstado().getFirst().getIdCambioEstadoCamion();
    UUID rutaId = UUID.randomUUID();

    Camion reconstituido = camionMapper.toDomain(entity, rutaId);
    assertEquals(camion.getId(), reconstituido.getId());
    assertEquals(0L, reconstituido.getVersion());
    assertEquals(rutaId, reconstituido.getRutaId());
    assertEquals(1, reconstituido.getHistorialEstado().size());

    reconstituido.habilitar();
    CamionEntity actualizada = camionMapper.toEntity(reconstituido, entity);
    assertEquals(EstadoCamion.DISPONIBLE, actualizada.getEstadoCamion());
    assertEquals(2, actualizada.getHistorialEstado().size());
    assertEquals(
        idCambioInicial, actualizada.getHistorialEstado().getFirst().getIdCambioEstadoCamion());

    entity.setVersion(2L);
    assertThrows(
        ObjectOptimisticLockingFailureException.class,
        () -> camionMapper.toEntity(reconstituido, entity));
  }

  @Test
  void choferMapper_deberiaMapearIdaYVueltaYSincronizarHistorialAppendOnly() {
    assertNull(choferMapper.toEntity(null));
    assertNull(choferMapper.toDomain(null, null));

    Chofer chofer = new Chofer("Carlos", "Diaz", "LIC-100", "1133445566");
    chofer.cambiarEstado(EstadoChofer.DESHABILITADO);
    ChoferEntity entity = choferMapper.toEntity(chofer);
    entity.setVersion(0L);

    UUID idCambioInicial = entity.getHistorialEstados().getFirst().getIdCambioEstadoChofer();
    UUID rutaId = UUID.randomUUID();

    Chofer reconstituido = choferMapper.toDomain(entity, rutaId);
    assertEquals(chofer.getId(), reconstituido.getId());
    assertEquals(0L, reconstituido.getVersion());
    assertEquals(rutaId, reconstituido.getRutaId());

    reconstituido.cambiarEstado(EstadoChofer.DISPONIBLE);
    ChoferEntity actualizada = choferMapper.toEntity(reconstituido, entity);
    assertEquals(2, actualizada.getHistorialEstados().size());
    assertEquals(
        idCambioInicial, actualizada.getHistorialEstados().getFirst().getIdCambioEstadoChofer());

    entity.setVersion(3L);
    assertThrows(
        ObjectOptimisticLockingFailureException.class,
        () -> choferMapper.toEntity(reconstituido, entity));
  }

  @Test
  void rutaMapper_deberiaMapearIdaYVueltaYSincronizarHistorialAppendOnly() {
    assertNull(rutaMapper.toEntity(null));
    assertNull(rutaMapper.toDomain(null));

    UUID entregaId = UUID.randomUUID();
    Ruta ruta = new Ruta(LocalDate.of(2026, 10, 10), UUID.randomUUID(), UUID.randomUUID());
    ruta.agregarEntrega(entregaId);
    ruta.iniciarRuta();

    RutaEntity entity = rutaMapper.toEntity(ruta);
    entity.setVersion(0L);
    UUID idCambioInicial = entity.getHistorialEstado().getFirst().getIdCambioEstadoRuta();

    Ruta reconstituida = rutaMapper.toDomain(entity);
    assertEquals(ruta.getId(), reconstituida.getId());
    assertEquals(List.of(entregaId), reconstituida.getEntregaIds());
    assertEquals(0L, reconstituida.getVersion());

    reconstituida.completarRuta();
    RutaEntity actualizada = rutaMapper.toEntity(reconstituida, entity);
    assertEquals(2, actualizada.getHistorialEstado().size());
    assertEquals(
        idCambioInicial, actualizada.getHistorialEstado().getFirst().getIdCambioEstadoRuta());

    entity.setVersion(5L);
    assertThrows(
        ObjectOptimisticLockingFailureException.class,
        () -> rutaMapper.toEntity(reconstituida, entity));
  }

  @Test
  void entregaMapper_deberiaMapearIdaYVueltaReutilizandoDireccionExistente() {
    assertNull(entregaMapper.toEntity(null));
    assertNull(entregaMapper.toDomain(null));

    PaisEntity paisEntity = new PaisEntity(UUID.randomUUID(), "Argentina");
    ProvinciaEntity provinciaEntity =
        new ProvinciaEntity(UUID.randomUUID(), paisEntity, "Buenos Aires");
    LocalidadEntity localidadEntity =
        new LocalidadEntity(UUID.randomUUID(), provinciaEntity, "CABA");

    Localidad caba = new Localidad("CABA", new Provincia("Buenos Aires", new Pais("Argentina")));
    Direccion dir = new Direccion("Av. Cordoba", 2100, 2, "C", "1120", caba);
    DireccionEntity dirEntity = entregaMapper.construirDireccionNueva(dir, localidadEntity);

    Entrega entrega = new Entrega(UUID.randomUUID(), UUID.randomUUID(), dir, 12.5f, 1.1f);
    entrega.iniciarRuta("Chofer");

    EntregaEntity entity = entregaMapper.toEntity(entrega, null, dirEntity);
    entity.setVersion(0L);
    UUID idDireccion = entity.getDireccion().getIdDireccion();
    UUID idCambioInicial = entity.getHistorialEstado().getFirst().getIdCambioEstadoEntrega();

    Entrega reconstituida = entregaMapper.toDomain(entity);
    assertEquals(entrega.getId(), reconstituida.getId());
    assertEquals("Av. Cordoba", reconstituida.getDestino().calle());
    assertEquals(0L, reconstituida.getVersion());

    reconstituida.negarEntrega("Chofer", "Ausente", true);
    EntregaEntity actualizada = entregaMapper.toEntity(reconstituida, entity, null);
    assertEquals(EstadoEntrega.NO_RECIBIDA, actualizada.getEstado());
    assertEquals(2, actualizada.getHistorialEstado().size());
    assertEquals(
        idCambioInicial, actualizada.getHistorialEstado().getFirst().getIdCambioEstadoEntrega());
    assertEquals(idDireccion, actualizada.getDireccion().getIdDireccion());

    entity.setVersion(9L);
    assertThrows(
        ObjectOptimisticLockingFailureException.class,
        () -> entregaMapper.toEntity(reconstituida, entity, null));
  }

  @Test
  void solicitudPlanificacionMapper_deberiaMapearIdaYVueltaYValidarVersion() {
    assertNull(solicitudMapper.toEntity(null));
    assertNull(solicitudMapper.toDomain(null));

    SolicitudPlanificacion solicitud =
        new SolicitudPlanificacion(
            UUID.randomUUID(), LocalDate.of(2026, 10, 11), 4, "http://localhost:8083/callback");

    SolicitudPlanificacionEntity entity = solicitudMapper.toEntity(solicitud);
    entity.setVersion(0L);

    SolicitudPlanificacion reconstituida = solicitudMapper.toDomain(entity);
    assertEquals(EstadoSolicitud.PENDIENTE, reconstituida.getEstado());
    assertEquals(0L, reconstituida.getVersion());

    reconstituida.marcarError("Timeout");
    SolicitudPlanificacionEntity actualizada = solicitudMapper.toEntity(reconstituida, entity);
    assertEquals(EstadoSolicitud.ERROR, actualizada.getEstado());
    assertEquals("Timeout", actualizada.getMotivoError());

    entity.setVersion(4L);
    assertThrows(
        ObjectOptimisticLockingFailureException.class,
        () -> solicitudMapper.toEntity(reconstituida, entity));
  }

  @Test
  void entidadesAuditoriaYGeografia_deberianExponerGettersYSetters() {
    PaisEntity pais = new PaisEntity();
    UUID idPais = UUID.randomUUID();
    pais.setIdPais(idPais);
    pais.setNombre("Uruguay");
    assertEquals(idPais, pais.getIdPais());
    assertEquals("Uruguay", pais.getNombre());

    ProvinciaEntity provincia = new ProvinciaEntity();
    UUID idProv = UUID.randomUUID();
    provincia.setIdProvincia(idProv);
    provincia.setPais(pais);
    provincia.setNombre("Montevideo");
    assertEquals(idProv, provincia.getIdProvincia());
    assertEquals(pais, provincia.getPais());
    assertEquals("Montevideo", provincia.getNombre());

    LocalidadEntity localidad = new LocalidadEntity();
    UUID idLoc = UUID.randomUUID();
    localidad.setIdLocalidad(idLoc);
    localidad.setProvincia(provincia);
    localidad.setNombre("Centro");
    assertEquals(idLoc, localidad.getIdLocalidad());
    assertEquals(provincia, localidad.getProvincia());
    assertEquals("Centro", localidad.getNombre());

    SolicitudTransicionEntregaEntity transicion = new SolicitudTransicionEntregaEntity();
    UUID idSol = UUID.randomUUID();
    UUID idEnt = UUID.randomUUID();
    Instant ahora = Instant.now();
    transicion.setIdSolicitud(idSol);
    transicion.setIdEntrega(idEnt);
    transicion.setActor("Chofer");
    transicion.setOcurrioEn(ahora);
    transicion.setTipoTransicion(TipoTransicionEntrega.NO_RECEPCION);
    transicion.setFotoRecepcionUrl("https://foto");
    transicion.setJustificacion("Cerrado");
    transicion.setReplanificable(true);
    assertEquals(idSol, transicion.getIdSolicitud());
    assertEquals(idEnt, transicion.getIdEntrega());
    assertEquals("Chofer", transicion.getActor());
    assertEquals(ahora, transicion.getOcurrioEn());
    assertEquals(TipoTransicionEntrega.NO_RECEPCION, transicion.getTipoTransicion());
    assertEquals("https://foto", transicion.getFotoRecepcionUrl());
    assertEquals("Cerrado", transicion.getJustificacion());
    assertEquals(Boolean.TRUE, transicion.getReplanificable());

    EventoEntregaEntity evento = new EventoEntregaEntity();
    UUID idEv = UUID.randomUUID();
    UUID idDon = UUID.randomUUID();
    UUID idRuta = UUID.randomUUID();
    evento.setIdEventoEntrega(idEv);
    evento.setIdEntrega(idEnt);
    evento.setIdDonacion(idDon);
    evento.setIdRuta(idRuta);
    evento.setOcurrioEn(ahora);
    evento.setTipo(TipoEventoEntrega.ENTREGA_FALLIDA);
    evento.setJustificacion("Rechazado");
    evento.setReplanificable(false);
    assertEquals(idEv, evento.getIdEventoEntrega());
    assertEquals(idEnt, evento.getIdEntrega());
    assertEquals(idDon, evento.getIdDonacion());
    assertEquals(idRuta, evento.getIdRuta());
    assertEquals(ahora, evento.getOcurrioEn());
    assertEquals(TipoEventoEntrega.ENTREGA_FALLIDA, evento.getTipo());
    assertEquals("Rechazado", evento.getJustificacion());
    assertEquals(Boolean.FALSE, evento.getReplanificable());
    assertNotNull(ahora);
  }
}
