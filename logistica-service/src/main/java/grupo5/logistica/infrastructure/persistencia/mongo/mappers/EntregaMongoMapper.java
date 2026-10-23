package grupo5.logistica.infrastructure.persistencia.mongo.mappers; // NOSONAR

import grupo5.logistica.infrastructure.persistencia.mongo.documents.CambioEstadoSubdocument;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.DireccionSubdocument;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.EntregaDocument;
import grupo5.logistica.models.entities.entregas.CambioEstadoEntrega;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.entregas.EstadoEntrega;
import grupo5.logistica.models.entities.rutas.direccion.CoordenadasGeograficas;
import grupo5.logistica.models.entities.rutas.direccion.Direccion;
import grupo5.logistica.models.entities.rutas.direccion.Localidad;
import grupo5.logistica.models.entities.rutas.direccion.Pais;
import grupo5.logistica.models.entities.rutas.direccion.Provincia;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

@SuppressWarnings("java:S120")
@Component
public class EntregaMongoMapper {

  public EntregaDocument toDocument(Entrega domain) {
    return toDocument(domain, null);
  }

  public EntregaDocument toDocument(Entrega domain, EntregaDocument existing) {
    if (domain == null) {
      return null;
    }
    if (existing != null
        && domain.getVersion() != null
        && !Objects.equals(domain.getVersion(), existing.getVersion())) {
      throw new ObjectOptimisticLockingFailureException(EntregaDocument.class, domain.getId());
    }

    EntregaDocument doc = existing != null ? existing : new EntregaDocument();
    doc.setId(domain.getId());
    doc.setIdRuta(domain.getIdRuta());
    doc.setIdDonacion(domain.getIdDonacion());
    doc.setIdBeneficiario(domain.getIdBeneficiaria());
    doc.setEstado(domain.getEstadoActual());
    doc.setHoraArribo(aInstantUtc(domain.getHoraArribo()));
    doc.setHoraSalida(aInstantUtc(domain.getHoraSalida()));
    doc.setFotosRecepcion(new ArrayList<>(domain.getFotosRecepcion()));
    doc.setVolumenTotalM3((double) domain.getVolumenTotalM3());
    doc.setPesoTotalKg((double) domain.getPesoTotalKG());

    if (existing == null || existing.getDestino() == null) {
      doc.setDestino(mapearDireccionADocument(domain.getDestino()));
    } else if (domain.getDestino() != null) {
      doc.getDestino().setUbicacion(aGeoJson(domain.getDestino().coordenadas()));
    }

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

  public DireccionSubdocument mapearDireccionADocument(Direccion destino) {
    if (destino == null) {
      return null;
    }
    String localidadNombre = destino.localidad() != null ? destino.localidad().nombre() : null;
    String provinciaNombre = null;
    if (destino.localidad() != null && destino.localidad().provincia() != null) {
      provinciaNombre = destino.localidad().provincia().nombre();
    }
    String paisNombre = null;
    if (destino.localidad() != null
        && destino.localidad().provincia() != null
        && destino.localidad().provincia().pais() != null) {
      paisNombre = destino.localidad().provincia().pais().nombre();
    }

    return DireccionSubdocument.builder()
        .calle(destino.calle())
        .altura(destino.altura())
        .piso(destino.piso() != null ? destino.piso().shortValue() : null)
        .departamento(destino.departamento())
        .codigoPostal(destino.codigoPostal())
        .localidad(localidadNombre)
        .provincia(provinciaNombre)
        .pais(paisNombre)
        .ubicacion(aGeoJson(destino.coordenadas()))
        .build();
  }

  public Direccion mapearDireccionADominio(DireccionSubdocument doc) {
    if (doc == null) {
      return null;
    }
    Pais pais = new Pais(doc.getPais() != null ? doc.getPais() : "");
    Provincia provincia = new Provincia(doc.getProvincia() != null ? doc.getProvincia() : "", pais);
    Localidad localidad =
        new Localidad(doc.getLocalidad() != null ? doc.getLocalidad() : "", provincia);

    return new Direccion(
        doc.getCalle(),
        doc.getAltura(),
        doc.getPiso() != null ? doc.getPiso().intValue() : null,
        doc.getDepartamento(),
        doc.getCodigoPostal(),
        localidad,
        aCoordenadas(doc.getUbicacion()));
  }

  public Entrega toDomain(EntregaDocument doc) {
    if (doc == null) {
      return null;
    }

    return new Entrega(
        doc.getId(),
        doc.getIdRuta(),
        doc.getIdDonacion(),
        doc.getIdBeneficiario(),
        mapearDireccionADominio(doc.getDestino()),
        doc.getEstado(),
        mapearHistorialDominio(doc),
        aLocalDateTimeUtc(doc.getHoraArribo()),
        aLocalDateTimeUtc(doc.getHoraSalida()),
        doc.getFotosRecepcion(),
        doc.getPesoTotalKg() != null ? doc.getPesoTotalKg().floatValue() : 0f,
        doc.getVolumenTotalM3() != null ? doc.getVolumenTotalM3().floatValue() : 0f,
        doc.getVersion());
  }

  private static List<CambioEstadoEntrega> mapearHistorialDominio(EntregaDocument doc) {
    if (doc.getHistorialEstado() == null) {
      return List.of();
    }
    return doc.getHistorialEstado().stream()
        .map(
            c ->
                new CambioEstadoEntrega(
                    aEstadoEntrega(c.getEstadoAnterior()),
                    aEstadoEntrega(c.getEstadoNuevo()),
                    aLocalDateTimeUtc(c.getTimestamp()),
                    c.getActor()))
        .toList();
  }

  private static EstadoEntrega aEstadoEntrega(String estado) {
    return estado != null ? EstadoEntrega.valueOf(estado) : null;
  }

  private static void sincronizarHistorial(Entrega domain, EntregaDocument doc) {
    List<CambioEstadoSubdocument> historialDoc = doc.getHistorialEstado();
    if (historialDoc == null) {
      historialDoc = new ArrayList<>();
      doc.setHistorialEstado(historialDoc);
    }

    List<CambioEstadoEntrega> historialDominio = domain.getHistorialEstado();
    if (historialDominio != null) {
      for (int i = historialDoc.size(); i < historialDominio.size(); i++) {
        CambioEstadoEntrega cambio = historialDominio.get(i);
        CambioEstadoSubdocument nuevo =
            CambioEstadoSubdocument.builder()
                .estadoAnterior(
                    cambio.estadoAnterior() != null ? cambio.estadoAnterior().name() : null)
                .estadoNuevo(cambio.estadoNuevo() != null ? cambio.estadoNuevo().name() : null)
                .timestamp(aInstantUtc(cambio.timeStamp()))
                .actor(cambio.actor())
                .build();
        historialDoc.add(nuevo);
      }
    }
  }

  /** GeoJSON usa el orden [longitud, latitud], al revés que el dominio. */
  private static GeoJsonPoint aGeoJson(CoordenadasGeograficas coordenadas) {
    return coordenadas != null
        ? new GeoJsonPoint(coordenadas.longitud(), coordenadas.latitud())
        : null;
  }

  private static CoordenadasGeograficas aCoordenadas(GeoJsonPoint punto) {
    return punto != null ? new CoordenadasGeograficas(punto.getY(), punto.getX()) : null;
  }

  private static Instant aInstantUtc(LocalDateTime fechaHora) {
    return fechaHora != null ? fechaHora.toInstant(ZoneOffset.UTC) : null;
  }

  private static LocalDateTime aLocalDateTimeUtc(Instant instante) {
    return instante != null ? LocalDateTime.ofInstant(instante, ZoneOffset.UTC) : null;
  }
}
