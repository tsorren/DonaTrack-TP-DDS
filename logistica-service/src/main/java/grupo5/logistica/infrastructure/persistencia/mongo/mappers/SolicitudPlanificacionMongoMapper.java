package grupo5.logistica.infrastructure.persistencia.mongo.mappers; // NOSONAR

import grupo5.logistica.infrastructure.persistencia.mongo.documents.SolicitudPlanificacionDocument;
import grupo5.logistica.models.entities.solicitudes.SolicitudPlanificacion;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
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
    if (existing != null) {
      ControlDeVersion.verificar(
          domain.getVersion(),
          existing.getVersion(),
          SolicitudPlanificacionDocument.class,
          domain.getId());
    }

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
    if (existing == null) {
      doc.setVersion(ControlDeVersion.versionInicial(domain.getVersion()));
    }

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

  /** Quita las rutas que el dominio ya no tiene, conserva el orden del resto y suma las nuevas. */
  private static void sincronizarRutasGeneradas(
      SolicitudPlanificacion domain, SolicitudPlanificacionDocument doc) {
    List<UUID> delDominio = Objects.requireNonNullElse(domain.getRutasGeneradas(), List.of());
    Set<UUID> rutas =
        new LinkedHashSet<>(Objects.requireNonNullElse(doc.getRutasGeneradas(), Set.of()));
    rutas.retainAll(delDominio);
    rutas.addAll(delDominio);
    doc.setRutasGeneradas(rutas);
  }
}
