package grupo5.logistica.infrastructure.persistencia.mongo.mappers; // NOSONAR

import grupo5.logistica.infrastructure.persistencia.mongo.documents.AsignacionSubdocument;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.DireccionSubdocument;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.EntregaDocument;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.FotoRecepcionSubdocument;
import grupo5.logistica.models.entities.entregas.CambioEstadoEntrega;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.entregas.EstadoEntrega;
import grupo5.logistica.models.entities.rutas.direccion.CoordenadasGeograficas;
import grupo5.logistica.models.entities.rutas.direccion.Direccion;
import grupo5.logistica.models.entities.rutas.direccion.Localidad;
import grupo5.logistica.models.entities.rutas.direccion.Pais;
import grupo5.logistica.models.entities.rutas.direccion.Provincia;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

@SuppressWarnings("java:S120")
@Component
public class EntregaMongoMapper {

  private final Clock reloj;

  public EntregaMongoMapper(Clock reloj) {
    this.reloj = reloj;
  }

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
    doc.setIdDonacion(domain.getIdDonacion());
    doc.setIdBeneficiaria(domain.getIdBeneficiaria());
    doc.setEstado(domain.getEstadoActual());
    doc.setHoraArribo(FechasUtc.aInstant(domain.getHoraArribo()));
    doc.setHoraSalida(FechasUtc.aInstant(domain.getHoraSalida()));
    sincronizarFotos(domain, doc);
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

    sincronizarAsignacion(domain, doc);
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
        idRutaAsignada(doc),
        doc.getIdDonacion(),
        doc.getIdBeneficiaria(),
        mapearDireccionADominio(doc.getDestino()),
        doc.getEstado(),
        mapearHistorialDominio(doc),
        FechasUtc.aLocalDateTime(doc.getHoraArribo()),
        FechasUtc.aLocalDateTime(doc.getHoraSalida()),
        doc.getFotosRecepcion().stream().map(FotoRecepcionSubdocument::getClave).toList(),
        doc.getPesoTotalKg() != null ? doc.getPesoTotalKg().floatValue() : 0f,
        doc.getVolumenTotalM3() != null ? doc.getVolumenTotalM3().floatValue() : 0f,
        doc.getVersion());
  }

  private static List<CambioEstadoEntrega> mapearHistorialDominio(EntregaDocument doc) {
    return HistorialEstadoMongo.aDominio(
        doc.getHistorialEstado(),
        c ->
            new CambioEstadoEntrega(
                HistorialEstadoMongo.estado(EstadoEntrega.class, c.getEstadoAnterior()),
                HistorialEstadoMongo.estado(EstadoEntrega.class, c.getEstadoNuevo()),
                FechasUtc.aLocalDateTime(c.getTimestamp()),
                c.getActor()));
  }

  private static void sincronizarHistorial(Entrega domain, EntregaDocument doc) {
    doc.setHistorialEstado(
        HistorialEstadoMongo.conCambiosNuevos(
            doc.getHistorialEstado(),
            domain.getHistorialEstado(),
            c ->
                HistorialEstadoMongo.cambio(
                    c.estadoAnterior(), c.estadoNuevo(), c.timeStamp(), c.actor())));
  }

  /** Las fotos ya guardadas conservan su {@code subidaEn}; las nuevas toman la hora actual. */
  private void sincronizarFotos(Entrega domain, EntregaDocument doc) {
    Map<String, FotoRecepcionSubdocument> guardadas =
        doc.getFotosRecepcion().stream()
            .collect(
                Collectors.toMap(
                    FotoRecepcionSubdocument::getClave,
                    Function.identity(),
                    (primera, repetida) -> primera));
    Instant ahora = reloj.instant();
    doc.setFotosRecepcion(
        domain.getFotosRecepcion().stream()
            .map(
                clave ->
                    guardadas.getOrDefault(
                        clave,
                        FotoRecepcionSubdocument.builder().clave(clave).subidaEn(ahora).build()))
            .collect(Collectors.toCollection(ArrayList::new)));
  }

  /** Conserva la asignación mientras siga siendo a la misma ruta; el adaptador la completa. */
  private static void sincronizarAsignacion(Entrega domain, EntregaDocument doc) {
    UUID idRuta = domain.getIdRuta();
    if (!Objects.equals(idRuta, idRutaAsignada(doc))) {
      doc.setAsignacion(
          idRuta != null ? AsignacionSubdocument.builder().idRuta(idRuta).build() : null);
    }
  }

  private static UUID idRutaAsignada(EntregaDocument doc) {
    return doc.getAsignacion() != null ? doc.getAsignacion().getIdRuta() : null;
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
}
