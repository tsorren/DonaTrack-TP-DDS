package grupo5.logistica.infrastructure.persistencia.mongo.mappers; // NOSONAR

import grupo5.logistica.infrastructure.persistencia.mongo.documents.CambioEstadoSubdocument;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.ParadaSubdocument;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.RutaDocument;
import grupo5.logistica.models.entities.rutas.CambioEstadoRuta;
import grupo5.logistica.models.entities.rutas.EstadoRuta;
import grupo5.logistica.models.entities.rutas.Ruta;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

@SuppressWarnings("java:S120")
@Component
public class RutaMongoMapper {

  public RutaDocument toDocument(Ruta domain) {
    return toDocument(domain, null);
  }

  public RutaDocument toDocument(Ruta domain, RutaDocument existing) {
    if (domain == null) {
      return null;
    }
    if (existing != null
        && domain.getVersion() != null
        && !Objects.equals(domain.getVersion(), existing.getVersion())) {
      throw new ObjectOptimisticLockingFailureException(RutaDocument.class, domain.getId());
    }

    RutaDocument doc = existing != null ? existing : new RutaDocument();
    doc.setId(domain.getId());
    doc.setIdChofer(domain.getChoferId());
    doc.setIdCamion(domain.getCamionId());
    doc.setFecha(domain.getFecha());
    doc.setEstado(domain.getEstado());
    doc.setHoraInicioReal(aInstantUtc(domain.getHoraInicioReal()));
    doc.setHoraFinReal(aInstantUtc(domain.getHoraFinReal()));

    if (existing == null) {
      if (domain.getVersion() != null && domain.getVersion() > 0L) {
        doc.setVersion(domain.getVersion());
      } else {
        doc.setVersion(null);
      }
    }

    sincronizarHistorial(domain, doc);
    sincronizarParadas(domain, doc);
    return doc;
  }

  public Ruta toDomain(RutaDocument doc) {
    if (doc == null) {
      return null;
    }

    return new Ruta(
        doc.getId(),
        doc.getFecha(),
        idsEntregasEnOrden(doc),
        doc.getIdChofer(),
        doc.getIdCamion(),
        doc.getEstado(),
        mapearHistorialDominio(doc),
        aLocalDateTimeUtc(doc.getHoraInicioReal()),
        aLocalDateTimeUtc(doc.getHoraFinReal()),
        doc.getVersion());
  }

  private static List<CambioEstadoRuta> mapearHistorialDominio(RutaDocument doc) {
    if (doc.getHistorialEstado() == null) {
      return List.of();
    }
    return doc.getHistorialEstado().stream()
        .map(
            c ->
                new CambioEstadoRuta(
                    aEstadoRuta(c.getEstadoAnterior()),
                    aEstadoRuta(c.getEstadoNuevo()),
                    aLocalDateTimeUtc(c.getTimestamp())))
        .toList();
  }

  private static EstadoRuta aEstadoRuta(String estado) {
    return estado != null ? EstadoRuta.valueOf(estado) : null;
  }

  private static void sincronizarHistorial(Ruta domain, RutaDocument doc) {
    List<CambioEstadoSubdocument> historialDoc = doc.getHistorialEstado();
    if (historialDoc == null) {
      historialDoc = new ArrayList<>();
      doc.setHistorialEstado(historialDoc);
    }

    List<CambioEstadoRuta> historialDominio = domain.getHistorialEstado();
    if (historialDominio != null) {
      for (int i = historialDoc.size(); i < historialDominio.size(); i++) {
        CambioEstadoRuta cambio = historialDominio.get(i);
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

  private static List<UUID> idsEntregasEnOrden(RutaDocument doc) {
    return doc.getParadas().stream()
        .sorted(Comparator.comparing(ParadaSubdocument::getOrden))
        .map(ParadaSubdocument::getIdEntrega)
        .toList();
  }

  /**
   * Conserva las paradas ya completadas; las nuevas llevan solo el id y las completa el adaptador.
   */
  private static void sincronizarParadas(Ruta domain, RutaDocument doc) {
    Map<UUID, ParadaSubdocument> existentes =
        doc.getParadas().stream()
            .collect(Collectors.toMap(ParadaSubdocument::getIdEntrega, Function.identity()));
    List<UUID> idsEntregas = domain.getEntregaIds();
    List<ParadaSubdocument> paradas = new ArrayList<>();
    for (int i = 0; i < idsEntregas.size(); i++) {
      UUID idEntrega = idsEntregas.get(i);
      ParadaSubdocument parada =
          existentes.getOrDefault(
              idEntrega, ParadaSubdocument.builder().idEntrega(idEntrega).build());
      parada.setOrden(i + 1);
      paradas.add(parada);
    }
    doc.setParadas(paradas);
  }

  private static Instant aInstantUtc(LocalDateTime fechaHora) {
    return fechaHora != null ? fechaHora.toInstant(ZoneOffset.UTC) : null;
  }

  private static LocalDateTime aLocalDateTimeUtc(Instant instante) {
    return instante != null ? LocalDateTime.ofInstant(instante, ZoneOffset.UTC) : null;
  }
}
