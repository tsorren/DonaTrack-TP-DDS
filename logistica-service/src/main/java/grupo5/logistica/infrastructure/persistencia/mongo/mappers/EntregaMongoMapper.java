package grupo5.logistica.infrastructure.persistencia.mongo.mappers; // NOSONAR

import static grupo5.logistica.infrastructure.persistencia.mongo.mappers.FechasUtc.aInstant;
import static grupo5.logistica.infrastructure.persistencia.mongo.mappers.FechasUtc.aLocalDateTime;
import static grupo5.logistica.infrastructure.persistencia.mongo.mappers.HistorialEstadoMongo.aDominio;
import static grupo5.logistica.infrastructure.persistencia.mongo.mappers.HistorialEstadoMongo.cambio;
import static grupo5.logistica.infrastructure.persistencia.mongo.mappers.HistorialEstadoMongo.conCambiosNuevos;
import static grupo5.logistica.infrastructure.persistencia.mongo.mappers.HistorialEstadoMongo.estado;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import grupo5.logistica.infrastructure.persistencia.mongo.documents.AsignacionSubdocument;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.CambioEstadoSubdocument;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
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
    if (existing != null) {
      ControlDeVersion.verificar(
          domain.getVersion(), existing.getVersion(), EntregaDocument.class, domain.getId());
    }

    EntregaDocument doc = existing != null ? existing : new EntregaDocument();
    doc.setId(domain.getId());
    doc.setIdDonacion(domain.getIdDonacion());
    doc.setIdBeneficiaria(domain.getIdBeneficiaria());
    doc.setEstado(domain.getEstadoActual());
    doc.setHoraArribo(aInstant(domain.getHoraArribo()));
    doc.setHoraSalida(aInstant(domain.getHoraSalida()));
    sincronizarFotos(domain, doc);
    doc.setVolumenTotalM3((double) domain.getVolumenTotalM3());
    doc.setPesoTotalKg((double) domain.getPesoTotalKG());

    sincronizarDestino(domain, doc);

    if (existing == null) {
      doc.setVersion(ControlDeVersion.versionInicial(domain.getVersion()));
    }

    sincronizarAsignacion(domain, doc);
    sincronizarHistorial(domain, doc);
    return doc;
  }

  private static DireccionSubdocument mapearDireccionADocument(Direccion destino) {
    if (destino == null) {
      return null;
    }
    Optional<Localidad> localidad = Optional.ofNullable(destino.localidad());
    Optional<Provincia> provincia = localidad.map(Localidad::provincia);

    return DireccionSubdocument.builder()
        .calle(destino.calle())
        .altura(destino.altura())
        .piso(destino.piso() != null ? destino.piso().shortValue() : null)
        .departamento(destino.departamento())
        .codigoPostal(destino.codigoPostal())
        .localidad(localidad.map(Localidad::nombre).orElse(null))
        .provincia(provincia.map(Provincia::nombre).orElse(null))
        .pais(provincia.map(Provincia::pais).map(Pais::nombre).orElse(null))
        .ubicacion(aGeoJson(destino.coordenadas()))
        .build();
  }

  private static Direccion mapearDireccionADominio(DireccionSubdocument doc) {
    if (doc == null) {
      return null;
    }
    Pais pais = new Pais(nombreOVacio(doc.getPais()));
    Provincia provincia = new Provincia(nombreOVacio(doc.getProvincia()), pais);
    Localidad localidad = new Localidad(nombreOVacio(doc.getLocalidad()), provincia);

    return new Direccion(
        doc.getCalle(),
        doc.getAltura(),
        doc.getPiso() != null ? doc.getPiso().intValue() : null,
        doc.getDepartamento(),
        doc.getCodigoPostal(),
        localidad,
        aCoordenadas(doc.getUbicacion()));
  }

  private static String nombreOVacio(String nombre) {
    return Objects.requireNonNullElse(nombre, "");
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
        aLocalDateTime(doc.getHoraArribo()),
        aLocalDateTime(doc.getHoraSalida()),
        doc.getFotosRecepcion().stream().map(FotoRecepcionSubdocument::getClave).toList(),
        doc.getPesoTotalKg() != null ? doc.getPesoTotalKg().floatValue() : 0f,
        doc.getVolumenTotalM3() != null ? doc.getVolumenTotalM3().floatValue() : 0f,
        doc.getVersion());
  }

  private static List<CambioEstadoEntrega> mapearHistorialDominio(EntregaDocument doc) {
    return aDominio(doc.getHistorialEstado(), EntregaMongoMapper::aCambio);
  }

  private static CambioEstadoEntrega aCambio(CambioEstadoSubdocument c) {
    EstadoEntrega anterior = estado(EstadoEntrega.class, c.getEstadoAnterior());
    EstadoEntrega nuevo = estado(EstadoEntrega.class, c.getEstadoNuevo());
    LocalDateTime cuando = aLocalDateTime(c.getTimestamp());
    return new CambioEstadoEntrega(anterior, nuevo, cuando, c.getActor());
  }

  private static void sincronizarHistorial(Entrega domain, EntregaDocument doc) {
    List<CambioEstadoSubdocument> guardado = doc.getHistorialEstado();
    List<CambioEstadoEntrega> cambios = domain.getHistorialEstado();
    doc.setHistorialEstado(conCambiosNuevos(guardado, cambios, EntregaMongoMapper::aSubdocumento));
  }

  private static CambioEstadoSubdocument aSubdocumento(CambioEstadoEntrega c) {
    return cambio(c.estadoAnterior(), c.estadoNuevo(), c.timeStamp(), c.actor());
  }

  /** Las fotos ya guardadas conservan su {@code subidaEn}; las nuevas toman la hora actual. */
  private void sincronizarFotos(Entrega domain, EntregaDocument doc) {
    Map<String, FotoRecepcionSubdocument> guardadas = porClave(doc.getFotosRecepcion());
    Instant ahora = reloj.instant();
    List<FotoRecepcionSubdocument> fotos = new ArrayList<>();
    for (String clave : domain.getFotosRecepcion()) {
      fotos.add(guardadas.getOrDefault(clave, new FotoRecepcionSubdocument(clave, ahora)));
    }
    doc.setFotosRecepcion(fotos);
  }

  /** Si el documento trae una foto repetida, se queda con la primera en vez de fallar. */
  private static Map<String, FotoRecepcionSubdocument> porClave(
      List<FotoRecepcionSubdocument> fotos) {
    Function<FotoRecepcionSubdocument, String> clave = FotoRecepcionSubdocument::getClave;
    return fotos.stream().collect(toMap(clave, identity(), (primera, repetida) -> primera));
  }

  /** El destino guardado no se reemplaza; solo se actualiza su ubicación. */
  private static void sincronizarDestino(Entrega domain, EntregaDocument doc) {
    if (doc.getDestino() == null) {
      doc.setDestino(mapearDireccionADocument(domain.getDestino()));
    } else if (domain.getDestino() != null) {
      doc.getDestino().setUbicacion(aGeoJson(domain.getDestino().coordenadas()));
    }
  }

  /** Conserva la asignación mientras siga siendo a la misma ruta; el adaptador la completa. */
  private static void sincronizarAsignacion(Entrega domain, EntregaDocument doc) {
    UUID idRuta = domain.getIdRuta();
    if (!Objects.equals(idRuta, idRutaAsignada(doc))) {
      doc.setAsignacion(asignacionSinCompletar(idRuta));
    }
  }

  private static AsignacionSubdocument asignacionSinCompletar(UUID idRuta) {
    return idRuta != null ? AsignacionSubdocument.builder().idRuta(idRuta).build() : null;
  }

  private static UUID idRutaAsignada(EntregaDocument doc) {
    return doc.getAsignacion() != null ? doc.getAsignacion().getIdRuta() : null;
  }

  /** GeoJSON usa el orden [longitud, latitud], al revés que el dominio. */
  private static GeoJsonPoint aGeoJson(CoordenadasGeograficas coordenadas) {
    if (coordenadas == null) {
      return null;
    }
    return new GeoJsonPoint(coordenadas.longitud(), coordenadas.latitud());
  }

  private static CoordenadasGeograficas aCoordenadas(GeoJsonPoint punto) {
    return punto != null ? new CoordenadasGeograficas(punto.getY(), punto.getX()) : null;
  }
}
