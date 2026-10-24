package grupo5.logistica.infrastructure.persistencia.mongo.mappers; // NOSONAR

import grupo5.logistica.infrastructure.persistencia.mongo.documents.ChoferDocument;
import grupo5.logistica.models.entities.choferes.CambioEstadoChofer;
import grupo5.logistica.models.entities.choferes.Chofer;
import grupo5.logistica.models.entities.choferes.EstadoChofer;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
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
    if (existing != null
        && domain.getVersion() != null
        && !Objects.equals(domain.getVersion(), existing.getVersion())) {
      throw new ObjectOptimisticLockingFailureException(ChoferDocument.class, domain.getId());
    }

    ChoferDocument doc = existing != null ? existing : new ChoferDocument();
    doc.setId(domain.getId());
    doc.setNombre(domain.getNombre());
    doc.setApellido(domain.getApellido());
    doc.setLicencia(domain.getLicencia());
    doc.setTelefono(domain.getTelefonoContacto());
    doc.setEstado(domain.getEstado());

    if (existing == null) {
      if (domain.getVersion() != null && domain.getVersion() > 0L) {
        doc.setVersion(domain.getVersion());
      } else {
        doc.setVersion(null);
      }
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
    return HistorialEstadoMongo.aDominio(
        doc.getHistorialEstado(),
        c ->
            new CambioEstadoChofer(
                HistorialEstadoMongo.estado(EstadoChofer.class, c.getEstadoAnterior()),
                HistorialEstadoMongo.estado(EstadoChofer.class, c.getEstadoNuevo()),
                FechasUtc.aLocalDateTime(c.getTimestamp())));
  }

  private static void sincronizarHistorial(Chofer domain, ChoferDocument doc) {
    doc.setHistorialEstado(
        HistorialEstadoMongo.conCambiosNuevos(
            doc.getHistorialEstado(),
            domain.getHistorialEstados(),
            c -> HistorialEstadoMongo.cambio(c.estadoAnterior(), c.estadoNuevo(), c.timestamp())));
  }
}
