package grupo5.logistica.infrastructure.persistencia.mongo.mappers; // NOSONAR

import static grupo5.logistica.infrastructure.persistencia.mongo.mappers.FechasUtc.aLocalDateTime;
import static grupo5.logistica.infrastructure.persistencia.mongo.mappers.HistorialEstadoMongo.aDominio;
import static grupo5.logistica.infrastructure.persistencia.mongo.mappers.HistorialEstadoMongo.cambio;
import static grupo5.logistica.infrastructure.persistencia.mongo.mappers.HistorialEstadoMongo.conCambiosNuevos;
import static grupo5.logistica.infrastructure.persistencia.mongo.mappers.HistorialEstadoMongo.estado;

import grupo5.logistica.infrastructure.persistencia.mongo.documents.CambioEstadoSubdocument;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.ChoferDocument;
import grupo5.logistica.models.entities.choferes.CambioEstadoChofer;
import grupo5.logistica.models.entities.choferes.Chofer;
import grupo5.logistica.models.entities.choferes.EstadoChofer;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@SuppressWarnings("java:S120")
@Component
public class ChoferMongoMapper {

  public ChoferDocument toDocument(Chofer domain) {
    return toDocument(domain, null);
  }

  public ChoferDocument toDocument(Chofer domain, ChoferDocument existing) {
    if (domain == null) {
      return null;
    }
    if (existing != null) {
      ControlDeVersion.verificar(
          domain.getVersion(), existing.getVersion(), ChoferDocument.class, domain.getId());
    }

    ChoferDocument doc = existing != null ? existing : new ChoferDocument();
    doc.setId(domain.getId());
    doc.setNombre(domain.getNombre());
    doc.setApellido(domain.getApellido());
    doc.setLicencia(domain.getLicencia());
    doc.setTelefono(domain.getTelefonoContacto());
    doc.setEstado(domain.getEstado());

    if (existing == null) {
      doc.setVersion(ControlDeVersion.versionInicial(domain.getVersion()));
    }

    sincronizarHistorial(domain, doc);
    return doc;
  }

  public Chofer toDomain(ChoferDocument doc) {
    return toDomain(doc, null);
  }

  public Chofer toDomain(ChoferDocument doc, UUID rutaActivaId) {
    if (doc == null) {
      return null;
    }

    return new Chofer(
        doc.getId(),
        doc.getNombre(),
        doc.getApellido(),
        doc.getLicencia(),
        doc.getTelefono(),
        doc.getEstado(),
        rutaActivaId,
        mapearHistorialDominio(doc),
        doc.getVersion());
  }

  private static List<CambioEstadoChofer> mapearHistorialDominio(ChoferDocument doc) {
    return aDominio(doc.getHistorialEstado(), ChoferMongoMapper::aCambio);
  }

  private static CambioEstadoChofer aCambio(CambioEstadoSubdocument c) {
    EstadoChofer anterior = estado(EstadoChofer.class, c.getEstadoAnterior());
    EstadoChofer nuevo = estado(EstadoChofer.class, c.getEstadoNuevo());
    return new CambioEstadoChofer(anterior, nuevo, aLocalDateTime(c.getTimestamp()));
  }

  private static void sincronizarHistorial(Chofer domain, ChoferDocument doc) {
    List<CambioEstadoSubdocument> guardado = doc.getHistorialEstado();
    List<CambioEstadoChofer> cambios = domain.getHistorialEstados();
    doc.setHistorialEstado(conCambiosNuevos(guardado, cambios, ChoferMongoMapper::aSubdocumento));
  }

  private static CambioEstadoSubdocument aSubdocumento(CambioEstadoChofer c) {
    return cambio(c.estadoAnterior(), c.estadoNuevo(), c.timestamp());
  }
}
