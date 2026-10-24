package grupo5.logistica.infrastructure.persistencia.mongo.mappers; // NOSONAR

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import grupo5.logistica.infrastructure.persistencia.mongo.documents.CambioEstadoSubdocument;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.CamionDocument;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.ChoferDocument;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.DireccionSubdocument;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.EntregaDocument;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.FotoRecepcionSubdocument;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.ParadaSubdocument;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.RutaDocument;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.SolicitudPlanificacionDocument;
import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.camiones.EstadoCamion;
import grupo5.logistica.models.entities.choferes.Chofer;
import grupo5.logistica.models.entities.choferes.EstadoChofer;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.entregas.EstadoEntrega;
import grupo5.logistica.models.entities.rutas.Ruta;
import grupo5.logistica.models.entities.rutas.direccion.CoordenadasGeograficas;
import grupo5.logistica.models.entities.rutas.direccion.Direccion;
import grupo5.logistica.models.entities.rutas.direccion.Localidad;
import grupo5.logistica.models.entities.rutas.direccion.Pais;
import grupo5.logistica.models.entities.rutas.direccion.Provincia;
import grupo5.logistica.models.entities.solicitudes.EstadoSolicitud;
import grupo5.logistica.models.entities.solicitudes.SolicitudPlanificacion;
import grupo5.logistica.testutils.EntregaMother;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;

class PersistenciaMongoMappersTest {

  private final CamionMongoMapper camionMapper = new CamionMongoMapper();
  private final ChoferMongoMapper choferMapper = new ChoferMongoMapper();
  private final RutaMongoMapper rutaMapper = new RutaMongoMapper();
  private final EntregaMongoMapper entregaMapper = new EntregaMongoMapper(Clock.systemUTC());
  private final SolicitudPlanificacionMongoMapper solicitudMapper =
      new SolicitudPlanificacionMongoMapper();

  @Test
  void camionMapper_deberiaMapearIdaYVueltaYSincronizarHistorialAppendOnly() {
    assertNull(camionMapper.toDocument(null));
    assertNull(camionMapper.toDomain(null, null));

    Camion camion = new Camion("AB123CD", 45.0f, 1500.0f, 3.0f);
    camion.deshabilitar();
    CamionDocument doc = camionMapper.toDocument(camion);
    assertNull(doc.getVersion()); // nuevo documento en inserción tiene version null

    doc.setVersion(0L); // simulamos persistencia en Mongo
    CambioEstadoSubdocument cambioInicial = doc.getHistorialEstado().getFirst();
    UUID rutaId = UUID.randomUUID();

    Camion reconstituido = camionMapper.toDomain(doc, rutaId);
    assertEquals(camion.getId(), reconstituido.getId());
    assertEquals(0L, reconstituido.getVersion());
    assertEquals(rutaId, reconstituido.getRutaId());
    assertEquals(1, reconstituido.getHistorialEstado().size());

    reconstituido.habilitar();
    CamionDocument actualizado = camionMapper.toDocument(reconstituido, doc);
    assertEquals(EstadoCamion.DISPONIBLE, actualizado.getEstado());
    assertEquals(2, actualizado.getHistorialEstado().size());
    assertEquals(cambioInicial, actualizado.getHistorialEstado().getFirst());

    doc.setVersion(2L);
    assertThrows(
        OptimisticLockingFailureException.class, () -> camionMapper.toDocument(reconstituido, doc));
  }

  @Test
  void choferMapper_deberiaMapearIdaYVueltaYSincronizarHistorialAppendOnly() {
    assertNull(choferMapper.toDocument(null));
    assertNull(choferMapper.toDomain(null, null));

    Chofer chofer = new Chofer("Carlos", "Diaz", "LIC-100", "1133445566");
    chofer.cambiarEstado(EstadoChofer.DESHABILITADO);
    ChoferDocument doc = choferMapper.toDocument(chofer);
    assertNull(doc.getVersion());

    doc.setVersion(0L);
    CambioEstadoSubdocument cambioInicial = doc.getHistorialEstado().getFirst();
    UUID rutaId = UUID.randomUUID();

    Chofer reconstituido = choferMapper.toDomain(doc, rutaId);
    assertEquals(chofer.getId(), reconstituido.getId());
    assertEquals(0L, reconstituido.getVersion());
    assertEquals(rutaId, reconstituido.getRutaId());

    reconstituido.cambiarEstado(EstadoChofer.DISPONIBLE);
    ChoferDocument actualizado = choferMapper.toDocument(reconstituido, doc);
    assertEquals(2, actualizado.getHistorialEstado().size());
    assertEquals(cambioInicial, actualizado.getHistorialEstado().getFirst());

    doc.setVersion(3L);
    assertThrows(
        OptimisticLockingFailureException.class, () -> choferMapper.toDocument(reconstituido, doc));
  }

  @Test
  void rutaMapper_deberiaMapearIdaYVueltaYSincronizarHistorialAppendOnly() {
    assertNull(rutaMapper.toDocument(null));
    assertNull(rutaMapper.toDomain(null));

    UUID entregaId = UUID.randomUUID();
    Ruta ruta = new Ruta(LocalDate.of(2026, 10, 10), UUID.randomUUID(), UUID.randomUUID());
    ruta.agregarEntrega(entregaId);
    ruta.iniciarRuta();

    RutaDocument doc = rutaMapper.toDocument(ruta);
    assertNull(doc.getVersion());
    doc.setVersion(0L);

    CambioEstadoSubdocument cambioInicial = doc.getHistorialEstado().getFirst();

    Ruta reconstituida = rutaMapper.toDomain(doc);
    assertEquals(ruta.getId(), reconstituida.getId());
    assertEquals(List.of(entregaId), reconstituida.getEntregaIds());
    assertEquals(0L, reconstituida.getVersion());

    reconstituida.completarRuta();
    RutaDocument actualizado = rutaMapper.toDocument(reconstituida, doc);
    assertEquals(2, actualizado.getHistorialEstado().size());
    assertEquals(cambioInicial, actualizado.getHistorialEstado().getFirst());

    doc.setVersion(5L);
    assertThrows(
        OptimisticLockingFailureException.class, () -> rutaMapper.toDocument(reconstituida, doc));
  }

  @Test
  void entregaMapper_deberiaMapearIdaYVueltaPreservandoDestinoInmutable() {
    assertNull(entregaMapper.toDocument(null));
    assertNull(entregaMapper.toDomain(null));

    Localidad caba = new Localidad("CABA", new Provincia("Buenos Aires", new Pais("Argentina")));
    Direccion dir = new Direccion("Av. Cordoba", 2100, 2, "C", "1120", caba);

    Entrega entrega = new Entrega(UUID.randomUUID(), UUID.randomUUID(), dir, 12.5f, 1.1f);
    entrega.iniciarRuta("Chofer");

    EntregaDocument doc = entregaMapper.toDocument(entrega);
    assertNull(doc.getVersion());
    doc.setVersion(0L);

    DireccionSubdocument destinoInicial = doc.getDestino();
    assertNotNull(destinoInicial);
    assertEquals("Av. Cordoba", destinoInicial.getCalle());
    assertEquals("Argentina", destinoInicial.getPais());
    assertEquals("Buenos Aires", destinoInicial.getProvincia());
    assertEquals("CABA", destinoInicial.getLocalidad());

    CambioEstadoSubdocument cambioInicial = doc.getHistorialEstado().getFirst();

    Entrega reconstituida = entregaMapper.toDomain(doc);
    assertEquals(entrega.getId(), reconstituida.getId());
    assertEquals("Av. Cordoba", reconstituida.getDestino().calle());
    assertEquals(0L, reconstituida.getVersion());

    reconstituida.negarEntrega("Chofer", "Ausente", true);
    EntregaDocument actualizado = entregaMapper.toDocument(reconstituida, doc);
    assertEquals(EstadoEntrega.NO_RECIBIDA, actualizado.getEstado());
    assertEquals(2, actualizado.getHistorialEstado().size());
    assertEquals(cambioInicial, actualizado.getHistorialEstado().getFirst());
    assertEquals(destinoInicial, actualizado.getDestino()); // Preserva destino inmutable

    doc.setVersion(9L);
    assertThrows(
        OptimisticLockingFailureException.class,
        () -> entregaMapper.toDocument(reconstituida, doc));
  }

  @Test
  void entregaMapper_deberiaGuardarLasCoordenadasEnOrdenLongitudLatitud() {
    Entrega entrega = entregaConfirmadaConCoordenadas(new CoordenadasGeograficas(-34.6, -58.4));

    EntregaDocument doc = entregaMapper.toDocument(entrega);

    assertEquals(-58.4, doc.getDestino().getUbicacion().getX());
    assertEquals(-34.6, doc.getDestino().getUbicacion().getY());
  }

  @Test
  void entregaMapper_deberiaRecuperarCoordenadasYTodasLasFotos() {
    CoordenadasGeograficas coordenadas = new CoordenadasGeograficas(-34.6, -58.4);
    EntregaDocument doc = entregaMapper.toDocument(entregaConfirmadaConCoordenadas(coordenadas));

    Entrega reconstituida = entregaMapper.toDomain(doc);

    assertEquals(coordenadas, reconstituida.getDestino().coordenadas());
    assertEquals(List.of("f1", "f2", "f3"), reconstituida.getFotosRecepcion());
  }

  @Test
  void entregaMapper_deberiaActualizarLasCoordenadasSobreUnDocumentoExistente() {
    EntregaDocument doc =
        entregaMapper.toDocument(
            entregaConfirmadaConCoordenadas(new CoordenadasGeograficas(-34.6, -58.4)));
    Entrega reconstituida = entregaMapper.toDomain(doc);
    reconstituida.getDestino().actualizarCoordenadas(new CoordenadasGeograficas(-31.4, -64.2));

    EntregaDocument actualizado = entregaMapper.toDocument(reconstituida, doc);

    assertEquals(-64.2, actualizado.getDestino().getUbicacion().getX());
    assertEquals(reconstituida.getDestino().calle(), actualizado.getDestino().getCalle());
  }

  @Test
  void entregaMapper_conFotosRepetidasEnElDocumento_conservaLaHoraDeLaPrimera() {
    Instant primeraSubida = Instant.parse("2026-10-23T10:00:00Z");
    EntregaDocument doc =
        entregaMapper.toDocument(
            entregaConfirmadaConCoordenadas(new CoordenadasGeograficas(-34.6, -58.4)));
    doc.setFotosRecepcion(
        List.of(
            fotoSubidaEn("f1", primeraSubida), fotoSubidaEn("f1", primeraSubida.plusSeconds(60))));
    Entrega reconstituida = entregaMapper.toDomain(doc);

    EntregaDocument actualizado = entregaMapper.toDocument(reconstituida, doc);

    assertEquals(primeraSubida, actualizado.getFotosRecepcion().getFirst().getSubidaEn());
  }

  @Test
  void rutaMapper_conParadasRepetidasEnElDocumento_conservaLaPrimera() {
    UUID entregaId = UUID.randomUUID();
    UUID donacionDeLaPrimera = UUID.randomUUID();
    Ruta ruta = new Ruta(LocalDate.of(2026, 10, 10), UUID.randomUUID(), UUID.randomUUID());
    ruta.agregarEntrega(entregaId);
    RutaDocument doc = rutaMapper.toDocument(ruta);
    doc.setParadas(
        List.of(
            paradaDe(1, entregaId, donacionDeLaPrimera),
            paradaDe(2, entregaId, UUID.randomUUID())));
    Ruta reconstituida = rutaMapper.toDomain(doc);

    RutaDocument actualizado = rutaMapper.toDocument(reconstituida, doc);

    assertEquals(donacionDeLaPrimera, actualizado.getParadas().getFirst().getIdDonacion());
  }

  private static FotoRecepcionSubdocument fotoSubidaEn(String clave, Instant subidaEn) {
    return FotoRecepcionSubdocument.builder().clave(clave).subidaEn(subidaEn).build();
  }

  private static ParadaSubdocument paradaDe(int orden, UUID idEntrega, UUID idDonacion) {
    return ParadaSubdocument.builder()
        .orden(orden)
        .idEntrega(idEntrega)
        .idDonacion(idDonacion)
        .build();
  }

  private static Entrega entregaConfirmadaConCoordenadas(CoordenadasGeograficas coordenadas) {
    Entrega entrega = EntregaMother.pendienteEn(coordenadas);
    entrega.iniciarRuta("Chofer");
    entrega.confirmarEntrega("Entidad", List.of("f1", "f2", "f3"));
    return entrega;
  }

  @Test
  void solicitudPlanificacionMapper_deberiaMapearIdaYVueltaYValidarVersion() {
    assertNull(solicitudMapper.toDocument(null));
    assertNull(solicitudMapper.toDomain(null));

    SolicitudPlanificacion solicitud =
        new SolicitudPlanificacion(
            UUID.randomUUID(), LocalDate.of(2026, 10, 11), 4, "http://localhost:8083/callback");

    SolicitudPlanificacionDocument doc = solicitudMapper.toDocument(solicitud);
    assertNull(doc.getVersion());
    doc.setVersion(0L);

    SolicitudPlanificacion reconstituida = solicitudMapper.toDomain(doc);
    assertEquals(EstadoSolicitud.PENDIENTE, reconstituida.getEstado());
    assertEquals(0L, reconstituida.getVersion());

    reconstituida.marcarError("Timeout");
    SolicitudPlanificacionDocument actualizado = solicitudMapper.toDocument(reconstituida, doc);
    assertEquals(EstadoSolicitud.ERROR, actualizado.getEstado());
    assertEquals("Timeout", actualizado.getMotivoError());

    doc.setVersion(4L);
    assertThrows(
        OptimisticLockingFailureException.class,
        () -> solicitudMapper.toDocument(reconstituida, doc));
  }
}
