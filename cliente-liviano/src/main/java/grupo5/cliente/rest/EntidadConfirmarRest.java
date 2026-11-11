package grupo5.cliente.rest;

import static grupo5.cliente.rest.Comun.ARGENTINA;
import static grupo5.cliente.rest.Comun.DIA;
import static grupo5.cliente.rest.Comun.DIA_Y_ANIO;
import static grupo5.cliente.rest.Comun.DIA_Y_HORA;
import static grupo5.cliente.rest.Comun.HORA;
import static grupo5.cliente.rest.Comun.PUERTA;
import static grupo5.cliente.rest.Comun.cantidad;
import static grupo5.cliente.rest.Comun.categoria;
import static grupo5.cliente.rest.Comun.desde;
import static grupo5.cliente.rest.Comun.icono;

import grupo5.cliente.puertos.EntidadConfirmarPuerto;
import grupo5.cliente.rest.DonacionesApi.Categoria;
import grupo5.cliente.rest.LogisticaApi.Seguimiento;
import grupo5.cliente.vistas.EntidadEntregaVista;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

/**
 * Confirmar recepción con Logística y Donaciones. Esperan la confirmación las entregas de la
 * entidad que llegaron a la puerta ({@code EN_PUERTA}, HB-22), primero, con la hora de llegada que
 * da el historial de la donación; después, las que siguen en traslado, que también se pueden
 * confirmar (un proveedor que no informa la llegada), con cuándo salieron. La clave es la donación,
 * como en Donaciones recibidas, para que sus enlaces lleguen al acta. Qué llevan sale de la
 * donación asignada ({@code idDonacion}) y la necesidad que cubre, de {@code GET
 * /api/necesidades/{id}}. Cuánto lleva recibido esa necesidad y la categoría del ranking de quien
 * ocultó su nombre no se piden. Confirmar sube las fotos ({@code POST /api/entregas/{id}/fotos}) y
 * pasa la entrega a ENTREGADA con el remito y, si lo hay, el reporte del problema.
 */
// @Primary solo para el análisis estático (S9352): la propiedad ya deja un único adaptador activo.
@Component
@Primary
@ConditionalOnProperty(name = "cliente.datos.entidad", havingValue = "rest")
class EntidadConfirmarRest implements EntidadConfirmarPuerto {

  private static final String EN_TRASLADO = "EN_TRASLADO";
  private static final String EN_PUERTA = "EN_PUERTA";
  private static final List<String> SE_CONFIRMAN = List.of(EN_PUERTA, EN_TRASLADO);

  private final LogisticaApi logistica;
  private final DonacionesApi donaciones;
  private final Clock reloj;

  EntidadConfirmarRest(LogisticaApi logistica, DonacionesApi donaciones, Clock reloj) {
    this.logistica = logistica;
    this.donaciones = donaciones;
    this.reloj = reloj;
  }

  @Override
  public String hoy() {
    return LocalDate.now(reloj.withZone(ARGENTINA)).format(DIA_Y_ANIO);
  }

  @Override
  public List<EntidadEntregaVista> entregas(UUID entidad) {
    List<Seguimiento> enLaPuerta =
        porConfirmar(entidad)
            .sorted(Comparator.comparingInt(s -> SE_CONFIRMAN.indexOf(s.estadoEntrega())))
            .toList();
    if (enLaPuerta.isEmpty()) {
      return List.of();
    }
    Map<UUID, DonacionesApi.Donacion> asignadas =
        donaciones.asignadas(entidad).stream()
            .filter(d -> d.id() != null)
            .collect(
                Collectors.toMap(DonacionesApi.Donacion::id, Function.identity(), (a, b) -> a));
    return enLaPuerta.stream()
        .filter(s -> asignadas.containsKey(s.idDonacion()))
        .map(s -> entrega(s, asignadas.get(s.idDonacion())))
        .toList();
  }

  private Stream<Seguimiento> porConfirmar(UUID entidad) {
    return logistica.seguimientoDeEntidad(entidad).stream()
        .filter(s -> SE_CONFIRMAN.contains(s.estadoEntrega()))
        .filter(s -> s.idEntrega() != null && s.idDonacion() != null);
  }

  private EntidadEntregaVista entrega(Seguimiento s, DonacionesApi.Donacion d) {
    Categoria categoria = categoria(d);
    // La unidad sola, la de Comun: "2 kg" sin el número; sin categoría, vacía.
    String unidad = cantidad(2, categoria).substring(1).strip();
    String cuanto = cantidad(d.cantidad(), categoria);
    String descripcion = Objects.requireNonNullElse(d.descripcion(), "Donación");
    String salio =
        s.horaSalida() == null ? null : "Salió del depósito el " + dia(s.horaSalida()) + ".";
    // La hora de llegada es la del cambio a EN_PUERTA en el historial de la donación (hora local)
    LocalDateTime llego = EN_PUERTA.equals(s.estadoEntrega()) ? desde(d, PUERTA) : null;
    String frase =
        llego == null
            ? salio
            : "Llegó a tu puerta el " + llego.format(DIA) + " a las " + llego.format(HORA) + ".";
    DonacionesApi.QuienDono quien = d.quienDono();
    return new EntidadEntregaVista(
        s.idDonacion().toString(),
        descripcion.toLowerCase(Locale.ROOT),
        descripcion + ", " + cuanto,
        cuanto,
        cuando(s.horaSalida()),
        llego == null ? null : llego.format(DIA_Y_HORA),
        frase,
        frase,
        null,
        categoria == null ? null : icono(categoria.nombre()),
        categoria == null ? null : categoria.nombre(),
        pedido(d.necesidadId()),
        null,
        null,
        quien == null || quien.nombreVisible() == null || quien.nombreVisible().isBlank()
            ? null
            : quien.nombreVisible().strip(),
        null,
        quien != null && "JURIDICA".equals(quien.tipo()) ? "empresa" : null,
        null,
        null,
        d.cantidad(),
        unidad.isEmpty() ? "unidades" : unidad,
        "kg".equals(unidad) || "l".equals(unidad),
        null,
        null);
  }

  /**
   * "Lentejas secas, 40 kg, hasta el 31 de octubre.": el nombre es el de la subcategoría, como en
   * Necesidades (la descripción es el motivo); sin necesidad, null.
   */
  private String pedido(UUID necesidadId) {
    if (necesidadId == null) {
      return null;
    }
    DonacionesApi.Necesidad n = donaciones.necesidad(necesidadId);
    if (n == null) {
      return null;
    }
    String nombre =
        donaciones.subcategorias().stream()
            .filter(s -> s.id() != null && s.id().equals(n.idSubcategoria()))
            .map(DonacionesApi.Subcategoria::nombre)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(n.descripcion());
    if (nombre == null) {
      return null;
    }
    Categoria conUnidad = new Categoria(null, null, n.unidad(), null, null, null);
    String cuanto =
        n.cantidadNecesitada() == null ? "" : ", " + cantidad(n.cantidadNecesitada(), conUnidad);
    LocalDate hasta = n.fechaLimite() != null ? n.fechaLimite() : n.fechaFin();
    return nombre + cuanto + (hasta == null ? "" : ", hasta el " + hasta.format(DIA)) + ".";
  }

  private static String cuando(Instant momento) {
    return momento == null ? null : momento.atZone(ARGENTINA).format(DIA_Y_HORA);
  }

  /** "8 de noviembre a las 18:20". */
  private static String dia(Instant momento) {
    ZonedDateTime aca = momento.atZone(ARGENTINA);
    return aca.format(DIA) + " a las " + aca.format(HORA);
  }

  @Override
  public void confirmar(UUID entidad, String clave, Acta acta) {
    // La clave es la donación; una clave que ya es la entrega (enlaces viejos) se usa tal cual
    UUID entrega =
        porConfirmar(entidad)
            .filter(s -> s.idDonacion().toString().equals(clave))
            .map(Seguimiento::idEntrega)
            .findFirst()
            .orElseGet(() -> UUID.fromString(clave));
    MultiValueMap<String, Object> partes = new LinkedMultiValueMap<>();
    acta.fotos().stream()
        .filter(f -> f != null && !f.isEmpty())
        .map(MultipartFile::getResource)
        .forEach(foto -> partes.add("foto", foto));
    List<String> fotos =
        logistica.subirFotos(entrega, partes).stream()
            .map(LogisticaApi.FotoSubida::url)
            .filter(Objects::nonNull)
            .toList();
    LogisticaApi.Reporte reporte =
        acta.motivo() == null
            ? null
            : new LogisticaApi.Reporte(
                acta.motivo().toUpperCase(Locale.ROOT),
                acta.detalle() == null || acta.detalle().isBlank() ? null : acta.detalle().strip(),
                "incompleta".equals(acta.motivo()) ? acta.cantidadRecibida() : null);
    logistica.cambiarEstado(
        entrega,
        new LogisticaApi.CambioDeEntrega(
            "ENTREGADA",
            entidad.toString(),
            null,
            null,
            fotos,
            fotos.isEmpty() ? null : fotos.getFirst(),
            acta.remito().strip(),
            reporte));
  }
}
