package grupo5.logistica.infrastructure.persistencia.mongo.documents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import grupo5.logistica.infrastructure.persistencia.entities.TipoEventoEntrega;
import grupo5.logistica.infrastructure.persistencia.entities.TipoTransicionEntrega;
import grupo5.logistica.models.entities.camiones.EstadoCamion;
import grupo5.logistica.models.entities.choferes.EstadoChofer;
import grupo5.logistica.models.entities.entregas.EstadoEntrega;
import grupo5.logistica.models.entities.rutas.EstadoRuta;
import grupo5.logistica.models.entities.solicitudes.EstadoSolicitud;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.mapping.Document;

class MongoDocumentsMappingTest {

  @Test
  void camionDocument_debeEstarMapeadoCorrectamente() {
    Document docAnnotation = CamionDocument.class.getAnnotation(Document.class);
    assertNotNull(docAnnotation);
    assertEquals("camiones", docAnnotation.collection());

    CamionDocument doc =
        CamionDocument.builder()
            .id(UUID.randomUUID())
            .patente("AB123CD")
            .capacidadVolumen(30.0)
            .capacidadPeso(5000.0)
            .altura(3.5)
            .estadoCamion(EstadoCamion.DISPONIBLE)
            .version(0L)
            .build();

    assertNotNull(doc.getId());
    assertEquals("AB123CD", doc.getPatente());
    assertEquals(0L, doc.getVersion());
    assertNotNull(doc.getHistorialEstado());
  }

  @Test
  void choferDocument_debeEstarMapeadoCorrectamente() {
    Document docAnnotation = ChoferDocument.class.getAnnotation(Document.class);
    assertNotNull(docAnnotation);
    assertEquals("choferes", docAnnotation.collection());

    ChoferDocument doc =
        ChoferDocument.builder()
            .id(UUID.randomUUID())
            .nombre("Carlos")
            .apellido("Pérez")
            .licencia("LIC-12345")
            .telefono("1122334455")
            .estadoChofer(EstadoChofer.DISPONIBLE)
            .version(0L)
            .build();

    assertNotNull(doc.getId());
    assertEquals("Carlos", doc.getNombre());
    assertNotNull(doc.getHistorialEstados());
  }

  @Test
  void rutaDocument_debeEstarMapeadoCorrectamente() {
    Document docAnnotation = RutaDocument.class.getAnnotation(Document.class);
    assertNotNull(docAnnotation);
    assertEquals("rutas", docAnnotation.collection());

    RutaDocument doc =
        RutaDocument.builder()
            .id(UUID.randomUUID())
            .idChofer(UUID.randomUUID())
            .idCamion(UUID.randomUUID())
            .fecha(LocalDate.now())
            .estado(EstadoRuta.PENDIENTE)
            .version(0L)
            .build();

    assertNotNull(doc.getId());
    assertEquals(EstadoRuta.PENDIENTE, doc.getEstado());
    assertNotNull(doc.getEntregaIds());
    assertNotNull(doc.getHistorialEstado());
  }

  @Test
  void entregaDocument_debeEstarMapeadoCorrectamente() {
    Document docAnnotation = EntregaDocument.class.getAnnotation(Document.class);
    assertNotNull(docAnnotation);
    assertEquals("entregas", docAnnotation.collection());

    DireccionSubdocument direccion =
        DireccionSubdocument.builder()
            .calle("Av. Corrientes")
            .altura(1234)
            .codigoPostal("C1043AAZ")
            .localidad("San Nicolás")
            .provincia("Buenos Aires")
            .pais("Argentina")
            .build();

    EntregaDocument doc =
        EntregaDocument.builder()
            .id(UUID.randomUUID())
            .idDonacion(UUID.randomUUID())
            .idBeneficiario(UUID.randomUUID())
            .estado(EstadoEntrega.PENDIENTE)
            .volumenTotalM3(2.5)
            .pesoTotalKg(150.0)
            .destino(direccion)
            .version(0L)
            .build();

    assertNotNull(doc.getId());
    assertEquals("Av. Corrientes", doc.getDestino().getCalle());
    assertEquals(EstadoEntrega.PENDIENTE, doc.getEstado());
  }

  @Test
  void solicitudPlanificacionDocument_debeEstarMapeadoCorrectamente() {
    Document docAnnotation = SolicitudPlanificacionDocument.class.getAnnotation(Document.class);
    assertNotNull(docAnnotation);
    assertEquals("solicitudes_planificacion", docAnnotation.collection());

    SolicitudPlanificacionDocument doc =
        SolicitudPlanificacionDocument.builder()
            .id(UUID.randomUUID())
            .fecha(LocalDate.now())
            .estado(EstadoSolicitud.PENDIENTE)
            .cantidadDonaciones(10)
            .callbackUrl("http://localhost:8083/callback")
            .build();

    assertNotNull(doc.getId());
    assertEquals(0, doc.getIntentosFallidos());
    assertNotNull(doc.getRutasGeneradas());
  }

  @Test
  void solicitudTransicionEntregaDocument_debeEstarMapeadoCorrectamente() {
    Document docAnnotation = SolicitudTransicionEntregaDocument.class.getAnnotation(Document.class);
    assertNotNull(docAnnotation);
    assertEquals("solicitudes_transicion_entrega", docAnnotation.collection());

    SolicitudTransicionEntregaDocument doc =
        SolicitudTransicionEntregaDocument.builder()
            .id(UUID.randomUUID())
            .idEntrega(UUID.randomUUID())
            .tipoTransicion(TipoTransicionEntrega.CONFIRMAR_ENTREGA)
            .actor("OPERADOR_1")
            .ocurrioEn(Instant.now())
            .build();

    assertNotNull(doc.getId());
    assertEquals(TipoTransicionEntrega.CONFIRMAR_ENTREGA, doc.getTipoTransicion());
  }

  @Test
  void eventoEntregaDocument_debeEstarMapeadoCorrectamente() {
    Document docAnnotation = EventoEntregaDocument.class.getAnnotation(Document.class);
    assertNotNull(docAnnotation);
    assertEquals("eventos_entrega", docAnnotation.collection());

    EventoEntregaDocument doc =
        EventoEntregaDocument.builder()
            .id(UUID.randomUUID())
            .idEntrega(UUID.randomUUID())
            .idDonacion(UUID.randomUUID())
            .tipo(TipoEventoEntrega.ENTREGA_EXITOSA)
            .ocurrioEn(Instant.now())
            .build();

    assertNotNull(doc.getId());
    assertEquals(TipoEventoEntrega.ENTREGA_EXITOSA, doc.getTipo());
  }
}
