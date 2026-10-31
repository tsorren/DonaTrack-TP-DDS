package grupo5.logistica.infrastructure.persistencia.mongo.mappers; // NOSONAR

import static grupo5.logistica.infrastructure.persistencia.mongo.mappers.FechasUtc.aInstant;
import static grupo5.logistica.infrastructure.persistencia.mongo.mappers.FechasUtc.aLocalDateTime;
import static grupo5.logistica.infrastructure.persistencia.mongo.mappers.HistorialEstadoMongo.aDominio;
import static grupo5.logistica.infrastructure.persistencia.mongo.mappers.HistorialEstadoMongo.cambio;
import static grupo5.logistica.infrastructure.persistencia.mongo.mappers.HistorialEstadoMongo.conCambiosNuevos;
import static grupo5.logistica.infrastructure.persistencia.mongo.mappers.HistorialEstadoMongo.estado;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import grupo5.logistica.infrastructure.persistencia.mongo.documents.CambioEstadoSubdocument;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.ParadaSubdocument;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.RutaDocument;
import grupo5.logistica.models.entities.rutas.CambioEstadoRuta;
import grupo5.logistica.models.entities.rutas.EstadoRuta;
import grupo5.logistica.models.entities.rutas.Ruta;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
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
    if (existing != null) {
      ControlDeVersion.verificar(
          domain.getVersion(), existing.getVersion(), RutaDocument.class, domain.getId());
    }

    RutaDocument doc = existing != null ? existing : new RutaDocument();
    doc.setId(domain.getId());
    doc.setIdChofer(domain.getChoferId());
    doc.setIdCamion(domain.getCamionId());
    doc.setFecha(domain.getFecha());
    doc.setEstado(domain.getEstado());
    doc.setHoraInicioReal(aInstant(domain.getHoraInicioReal()));
    doc.setHoraFinReal(aInstant(domain.getHoraFinReal()));

    if (existing == null) {
      doc.setVersion(ControlDeVersion.versionInicial(domain.getVersion()));
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
        aLocalDateTime(doc.getHoraInicioReal()),
        aLocalDateTime(doc.getHoraFinReal()),
        doc.getVersion());
  }

  private static List<CambioEstadoRuta> mapearHistorialDominio(RutaDocument doc) {
    return aDominio(doc.getHistorialEstado(), RutaMongoMapper::aCambio);
  }

  private static CambioEstadoRuta aCambio(CambioEstadoSubdocument c) {
    EstadoRuta anterior = estado(EstadoRuta.class, c.getEstadoAnterior());
    EstadoRuta nuevo = estado(EstadoRuta.class, c.getEstadoNuevo());
    return new CambioEstadoRuta(anterior, nuevo, aLocalDateTime(c.getTimestamp()));
  }

  private static void sincronizarHistorial(Ruta domain, RutaDocument doc) {
    List<CambioEstadoSubdocument> guardado = doc.getHistorialEstado();
    List<CambioEstadoRuta> cambios = domain.getHistorialEstado();
    doc.setHistorialEstado(conCambiosNuevos(guardado, cambios, RutaMongoMapper::aSubdocumento));
  }

  private static CambioEstadoSubdocument aSubdocumento(CambioEstadoRuta c) {
    return cambio(c.estadoAnterior(), c.estadoNuevo(), c.timestamp());
  }

  private static List<UUID> idsEntregasEnOrden(RutaDocument doc) {
    Comparator<ParadaSubdocument> porOrden = Comparator.comparing(ParadaSubdocument::getOrden);
    List<ParadaSubdocument> paradas = doc.getParadas();
    return paradas.stream().sorted(porOrden).map(ParadaSubdocument::getIdEntrega).toList();
  }

  /**
   * Conserva las paradas ya completadas; las nuevas llevan solo el id y las completa el adaptador.
   */
  private static void sincronizarParadas(Ruta domain, RutaDocument doc) {
    Map<UUID, ParadaSubdocument> existentes = porIdEntrega(doc.getParadas());
    List<UUID> idsEntregas = domain.getEntregaIds();
    List<ParadaSubdocument> paradas = new ArrayList<>();
    for (int i = 0; i < idsEntregas.size(); i++) {
      paradas.add(paradaEnOrden(i + 1, idsEntregas.get(i), existentes));
    }
    doc.setParadas(paradas);
  }

  /** Si el documento trae una entrega repetida, se queda con la primera parada en vez de fallar. */
  private static Map<UUID, ParadaSubdocument> porIdEntrega(List<ParadaSubdocument> paradas) {
    Function<ParadaSubdocument, UUID> idEntrega = ParadaSubdocument::getIdEntrega;
    return paradas.stream().collect(toMap(idEntrega, identity(), (primera, repetida) -> primera));
  }

  private static ParadaSubdocument paradaEnOrden(
      int orden, UUID idEntrega, Map<UUID, ParadaSubdocument> existentes) {
    ParadaSubdocument nueva = ParadaSubdocument.builder().idEntrega(idEntrega).build();
    ParadaSubdocument parada = existentes.getOrDefault(idEntrega, nueva);
    parada.setOrden(orden);
    return parada;
  }
}
