package grupo5.logistica.infrastructure.persistencia.mongo.mappers; // NOSONAR

import grupo5.logistica.infrastructure.persistencia.mongo.documents.CambioEstadoSubdocument;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.ChoferDocument;
import grupo5.logistica.models.entities.choferes.CambioEstadoChofer;
import grupo5.logistica.models.entities.choferes.Chofer;
import grupo5.logistica.models.entities.choferes.EstadoChofer;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
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
    doc.setEstadoChofer(domain.getEstado());

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
        doc.getEstadoChofer(),
        rutaActivaId,
        mapearHistorialDominio(doc),
        doc.getVersion());
  }

  private static List<CambioEstadoChofer> mapearHistorialDominio(ChoferDocument doc) {
    if (doc.getHistorialEstados() == null) {
      return List.of();
    }
    return doc.getHistorialEstados().stream()
        .map(
            c ->
                new CambioEstadoChofer(
                    aEstadoChofer(c.getEstadoAnterior()),
                    aEstadoChofer(c.getEstadoNuevo()),
                    aLocalDateTimeUtc(c.getTimestamp())))
        .toList();
  }

  private static EstadoChofer aEstadoChofer(String estado) {
    return estado != null ? EstadoChofer.valueOf(estado) : null;
  }

  private static void sincronizarHistorial(Chofer domain, ChoferDocument doc) {
    List<CambioEstadoSubdocument> historialDoc = doc.getHistorialEstados();
    if (historialDoc == null) {
      historialDoc = new ArrayList<>();
      doc.setHistorialEstados(historialDoc);
    }

    List<CambioEstadoChofer> historialDominio = domain.getHistorialEstados();
    if (historialDominio != null) {
      for (int i = historialDoc.size(); i < historialDominio.size(); i++) {
        CambioEstadoChofer cambio = historialDominio.get(i);
        CambioEstadoSubdocument nuevo =
            CambioEstadoSubdocument.builder()
                .estadoAnterior(
                    cambio.estadoAnterior() != null ? cambio.estadoAnterior().name() : null)
                .estadoNuevo(cambio.estadoNuevo() != null ? cambio.estadoNuevo().name() : null)
                .timestamp(aInstantUtc(cambio.timestamp()))
                .build();
        historialDoc.add(nuevo);
      }
    }
  }

  private static Instant aInstantUtc(LocalDateTime fechaHora) {
    return fechaHora != null ? fechaHora.toInstant(ZoneOffset.UTC) : null;
  }

  private static LocalDateTime aLocalDateTimeUtc(Instant instante) {
    return instante != null ? LocalDateTime.ofInstant(instante, ZoneOffset.UTC) : null;
  }
}
