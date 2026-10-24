package grupo5.logistica.infrastructure.persistencia.mongo.mappers; // NOSONAR

import grupo5.logistica.infrastructure.persistencia.mongo.documents.CamionDocument;
import grupo5.logistica.models.entities.camiones.CambioEstadoCamion;
import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.camiones.EstadoCamion;
import java.util.List;
import java.util.UUID;
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
    if (existing != null) {
      ControlDeVersion.verificar(
          domain.getVersion(), existing.getVersion(), CamionDocument.class, domain.getId());
    }

    CamionDocument doc = existing != null ? existing : new CamionDocument();
    doc.setId(domain.getId());
    doc.setPatente(domain.getPatente());
    doc.setCapacidadVolumen(aDouble(domain.getCapacidadVolumen()));
    doc.setCapacidadPeso(aDouble(domain.getCapacidadKG()));
    doc.setAltura(aDouble(domain.getAltura()));
    doc.setEstado(domain.getEstado());

    if (existing == null) {
      doc.setVersion(ControlDeVersion.versionInicial(domain.getVersion()));
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
    return HistorialEstadoMongo.aDominio(
        doc.getHistorialEstado(),
        c ->
            new CambioEstadoCamion(
                HistorialEstadoMongo.estado(EstadoCamion.class, c.getEstadoAnterior()),
                HistorialEstadoMongo.estado(EstadoCamion.class, c.getEstadoNuevo()),
                FechasUtc.aLocalDateTime(c.getTimestamp())));
  }

  private static void sincronizarHistorial(Camion domain, CamionDocument doc) {
    doc.setHistorialEstado(
        HistorialEstadoMongo.conCambiosNuevos(
            doc.getHistorialEstado(),
            domain.getHistorialEstado(),
            c -> HistorialEstadoMongo.cambio(c.estadoAnterior(), c.estadoNuevo(), c.timestamp())));
  }

  private static Double aDouble(Float valor) {
    return valor != null ? valor.doubleValue() : null;
  }

  private static Float aFloat(Double valor) {
    return valor != null ? valor.floatValue() : null;
  }
}
