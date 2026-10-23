package grupo5.logistica.infrastructure.persistencia.mongo.mappers; // NOSONAR

import grupo5.logistica.infrastructure.persistencia.mongo.documents.CambioEstadoSubdocument;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.CamionDocument;
import grupo5.logistica.models.entities.camiones.CambioEstadoCamion;
import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.camiones.EstadoCamion;
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
public class CamionMongoMapper {

  public CamionDocument toDocument(Camion domain) {
    return toDocument(domain, null);
  }

  public CamionDocument toDocument(Camion domain, CamionDocument existing) {
    if (domain == null) {
      return null;
    }
    if (existing != null
        && domain.getVersion() != null
        && !Objects.equals(domain.getVersion(), existing.getVersion())) {
      throw new ObjectOptimisticLockingFailureException(CamionDocument.class, domain.getId());
    }

    CamionDocument doc = existing != null ? existing : new CamionDocument();
    doc.setId(domain.getId());
    doc.setPatente(domain.getPatente());
    doc.setCapacidadVolumen(aDouble(domain.getCapacidadVolumen()));
    doc.setCapacidadPeso(aDouble(domain.getCapacidadKG()));
    doc.setAltura(aDouble(domain.getAltura()));
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

  public Camion toDomain(CamionDocument doc) {
    return toDomain(doc, null);
  }

  public Camion toDomain(CamionDocument doc, UUID rutaActivaId) {
    if (doc == null) {
      return null;
    }

    return new Camion(
        doc.getId(),
        rutaActivaId,
        doc.getPatente(),
        aFloat(doc.getCapacidadVolumen()),
        aFloat(doc.getCapacidadPeso()),
        aFloat(doc.getAltura()),
        doc.getEstado(),
        mapearHistorialDominio(doc),
        doc.getVersion());
  }

  private static List<CambioEstadoCamion> mapearHistorialDominio(CamionDocument doc) {
    if (doc.getHistorialEstado() == null) {
      return List.of();
    }
    return doc.getHistorialEstado().stream()
        .map(
            c ->
                new CambioEstadoCamion(
                    aEstadoCamion(c.getEstadoAnterior()),
                    aEstadoCamion(c.getEstadoNuevo()),
                    aLocalDateTimeUtc(c.getTimestamp())))
        .toList();
  }

  private static EstadoCamion aEstadoCamion(String estado) {
    return estado != null ? EstadoCamion.valueOf(estado) : null;
  }

  private static void sincronizarHistorial(Camion domain, CamionDocument doc) {
    List<CambioEstadoSubdocument> historialDoc = doc.getHistorialEstado();
    if (historialDoc == null) {
      historialDoc = new ArrayList<>();
      doc.setHistorialEstado(historialDoc);
    }

    List<CambioEstadoCamion> historialDominio = domain.getHistorialEstado();
    if (historialDominio != null) {
      for (int i = historialDoc.size(); i < historialDominio.size(); i++) {
        CambioEstadoCamion cambio = historialDominio.get(i);
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

  private static Double aDouble(Float valor) {
    return valor != null ? valor.doubleValue() : null;
  }

  private static Float aFloat(Double valor) {
    return valor != null ? valor.floatValue() : null;
  }

  private static Instant aInstantUtc(LocalDateTime fechaHora) {
    return fechaHora != null ? fechaHora.toInstant(ZoneOffset.UTC) : null;
  }

  private static LocalDateTime aLocalDateTimeUtc(Instant instante) {
    return instante != null ? LocalDateTime.ofInstant(instante, ZoneOffset.UTC) : null;
  }
}
