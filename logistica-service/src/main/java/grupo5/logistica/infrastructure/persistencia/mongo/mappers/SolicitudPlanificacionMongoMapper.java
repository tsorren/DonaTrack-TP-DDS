package grupo5.logistica.infrastructure.persistencia.mongo.mappers; // NOSONAR

import grupo5.logistica.infrastructure.persistencia.mongo.documents.SolicitudPlanificacionDocument;
import grupo5.logistica.models.entities.solicitudes.SolicitudPlanificacion;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

@SuppressWarnings("java:S120")
@Component
public class SolicitudPlanificacionMongoMapper {

  public SolicitudPlanificacionDocument toDocument(SolicitudPlanificacion domain) {
    return toDocument(domain, null);
  }

  public SolicitudPlanificacionDocument toDocument(
      SolicitudPlanificacion domain, SolicitudPlanificacionDocument existing) {
    if (domain == null) {
      return null;
    }
    validarVersion(domain, existing);

    SolicitudPlanificacionDocument doc =
        existing != null ? existing : new SolicitudPlanificacionDocument();
    doc.setId(domain.getId());
    doc.setFecha(domain.getFecha());
    doc.setEstado(domain.getEstado());
    doc.setCantidadDonaciones(domain.getCantidadDonaciones());
    doc.setCallbackUrl(domain.getCallbackUrl());
    doc.setIntentosFallidos(domain.getIntentosFallidos());
    doc.setMotivoError(domain.getMotivoError());

    sincronizarRutasGeneradas(domain, doc);
    configurarVersionInicial(domain, doc, existing);

    return doc;
  }

  public SolicitudPlanificacion toDomain(SolicitudPlanificacionDocument doc) {
    if (doc == null) {
      return null;
    }

    return new SolicitudPlanificacion(
        doc.getId(),
        doc.getFecha(),
        doc.getEstado(),
        doc.getCantidadDonaciones(),
        doc.getCallbackUrl(),
        doc.getRutasGeneradas() != null ? new ArrayList<>(doc.getRutasGeneradas()) : List.of(),
        doc.getIntentosFallidos(),
        doc.getMotivoError(),
        doc.getVersion());
  }

  private static void validarVersion(
      SolicitudPlanificacion domain, SolicitudPlanificacionDocument existing) {
    if (existing != null
        && domain.getVersion() != null
        && !Objects.equals(domain.getVersion(), existing.getVersion())) {
      throw new ObjectOptimisticLockingFailureException(
          SolicitudPlanificacionDocument.class, domain.getId());
    }
  }

  private static void sincronizarRutasGeneradas(
      SolicitudPlanificacion domain, SolicitudPlanificacionDocument doc) {
    List<UUID> nuevasRutas = domain.getRutasGeneradas();
    if (doc.getRutasGeneradas() == null) {
      doc.setRutasGeneradas(
          nuevasRutas != null ? new LinkedHashSet<>(nuevasRutas) : new LinkedHashSet<>());
      return;
    }
    if (nuevasRutas != null) {
      doc.getRutasGeneradas().retainAll(nuevasRutas);
      doc.getRutasGeneradas().addAll(nuevasRutas);
    } else {
      doc.getRutasGeneradas().clear();
    }
  }

  private static void configurarVersionInicial(
      SolicitudPlanificacion domain,
      SolicitudPlanificacionDocument doc,
      SolicitudPlanificacionDocument existing) {
    if (existing != null) {
      return;
    }
    if (domain.getVersion() != null && domain.getVersion() > 0L) {
      doc.setVersion(domain.getVersion());
    } else {
      doc.setVersion(null);
    }
  }
}
