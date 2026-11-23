package grupo5.cliente.rest;

import static grupo5.cliente.rest.Comun.ARGENTINA;
import static grupo5.cliente.rest.Comun.ASIGNADA;
import static grupo5.cliente.rest.Comun.DIA;
import static grupo5.cliente.rest.Comun.DIA_Y_ANIO;
import static grupo5.cliente.rest.Comun.DIA_Y_HORA;
import static grupo5.cliente.rest.Comun.ES_AR;
import static grupo5.cliente.rest.Comun.HORA;
import static grupo5.cliente.rest.Comun.LISTA;
import static grupo5.cliente.rest.Comun.PUERTA;
import static grupo5.cliente.rest.Comun.cantidad;
import static grupo5.cliente.rest.Comun.categoria;
import static grupo5.cliente.rest.Comun.desde;
import static grupo5.cliente.rest.Comun.icono;
import static grupo5.cliente.rest.Comun.normalizado;

import grupo5.cliente.puertos.EntidadConfirmarPuerto;
import grupo5.cliente.rest.DonacionesApi.Categoria;
import grupo5.cliente.rest.LogisticaApi.Seguimiento;
import grupo5.cliente.vistas.EntidadEntregaVista;
import java.text.NumberFormat;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
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
 *
 * <p>Desde R11-43 no se leen todas las asignadas ni todas las entregas: el acta pide a Donaciones
 * solo la donación de su clave ({@code id}, con la entidad) y, para ofrecer la que sigue, una
 * página de 2 de las que están en la puerta o en traslado ({@code orden=ATENCION}, la puerta
 * primero); de Logística, solo las entregas de esas donaciones. Confirmar busca la entrega por la
 * donación ({@code idDonacion}).
 *
 * <p>Recepción confirmada (ORG-12) arma la entrega ya confirmada con lo que Logística guarda de
 * ella ({@code GET /api/entregas/{id}}): el remito, la primera foto (solo si es una dirección web),
 * cuándo pasó a ENTREGADA y el problema que se contó. Quién la recibió no se guarda (HG-03): la
 * pantalla dice la organización. Cuándo se registró, se asignó y se preparó sale del historial de
 * la donación, que ya llega con ella.
 */
// @Primary solo para el análisis estático (S9352): la propiedad ya deja un único adaptador activo.
@Component
@Primary
@ConditionalOnProperty(name = "cliente.datos.entidad", havingValue = "rest")
class EntidadConfirmarRest implements EntidadConfirmarPuerto {

  private static final String EN_TRASLADO = "EN_TRASLADO";
  private static final String EN_PUERTA = "EN_PUERTA";
  private static final List<String> SE_CONFIRMAN = List.of(EN_PUERTA, EN_TRASLADO);
  private static final String ENTREGADA = "ENTREGADA";

  /**
   * La confirmada, en Donaciones: ENTREGADA o, mientras Donaciones no se enteró del cambio de
   * Logística, todavía en la puerta o en traslado.
   */
  private static final List<String> CONFIRMADA = List.of(ENTREGADA, EN_PUERTA, EN_TRASLADO);

  private static final DateTimeFormatter CUANDO =
      DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy, H:mm", ES_AR);

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
  public List<EntidadEntregaVista> entregas(UUID entidad, String pedida) {
    UUID clave = comoId(pedida);
    if (clave == null) {
      return List.of();
    }
    Map<UUID, DonacionesApi.Donacion> asignadas = new LinkedHashMap<>();
    asignadas(entidad, clave, SE_CONFIRMAN, 1).forEach(d -> asignadas.put(d.id(), d));
    if (asignadas.isEmpty()) {
      // La pedida no espera la confirmación o no es de la entidad: el acta dice que no la encontró
      return List.of();
    }
    otraQueEspera(entidad, clave).ifPresent(d -> asignadas.put(d.id(), d));
    MultiValueMap<String, String> filtros =
        EntidadSeguimientoRest.deLasDonaciones(
            asignadas.keySet().stream().map(UUID::toString).toList());
    SE_CONFIRMAN.forEach(e -> filtros.add("estado", e));
    return logistica.seguimientoDeEntidad(entidad, filtros).stream()
        .filter(s -> SE_CONFIRMAN.contains(s.estadoEntrega()))
        .filter(s -> s.idEntrega() != null && asignadas.containsKey(s.idDonacion()))
        .sorted(Comparator.comparingInt(s -> SE_CONFIRMAN.indexOf(s.estadoEntrega())))
        .map(s -> entrega(s, asignadas.get(s.idDonacion()), Asentado.NADA))
        .toList();
  }

  @Override
  public List<EntidadEntregaVista> confirmada(UUID entidad, String pedida) {
    UUID clave = comoId(pedida);
    if (clave == null) {
      return List.of();
    }
    DonacionesApi.Donacion donacion =
        asignadas(entidad, clave, CONFIRMADA, 1).stream().findFirst().orElse(null);
    if (donacion == null) {
      // No es de la entidad o no llegó: la pantalla dice que no la encontró
      return List.of();
    }
    DonacionesApi.Donacion otra = otraQueEspera(entidad, clave).orElse(null);
    List<String> pedidas = new ArrayList<>(List.of(clave.toString()));
    if (otra != null) {
      pedidas.add(otra.id().toString());
    }
    List<Seguimiento> seguimientos =
        logistica.seguimientoDeEntidad(entidad, EntidadSeguimientoRest.deLasDonaciones(pedidas));
    Seguimiento entregada =
        seguimientos.stream()
            .filter(s -> s.idEntrega() != null && clave.equals(s.idDonacion()))
            .filter(s -> ENTREGADA.equals(s.estadoEntrega()))
            .findFirst()
            .orElse(null);
    if (entregada == null) {
      // Todavía no está confirmada: no hay nada asentado para mostrar
      return List.of();
    }
    List<EntidadEntregaVista> vistas = new ArrayList<>();
    vistas.add(
        entrega(entregada, donacion, asentado(logistica.entrega(entregada.idEntrega()), donacion)));
    if (otra != null) {
      seguimientos.stream()
          .filter(s -> s.idEntrega() != null && otra.id().equals(s.idDonacion()))
          .filter(s -> SE_CONFIRMAN.contains(s.estadoEntrega()))
          .findFirst()
          .ifPresent(s -> vistas.add(entrega(s, otra, Asentado.NADA)));
    }
    return vistas;
  }

  /** La primera que espera en el orden de atención y no es {@code clave}, para ofrecerla. */
  private Optional<DonacionesApi.Donacion> otraQueEspera(UUID entidad, UUID clave) {
    return asignadas(entidad, null, SE_CONFIRMAN, 2).stream()
        .filter(d -> !clave.equals(d.id()))
        .findFirst();
  }

  /**
   * Lo que quedó asentado en Logística: el remito, la primera foto si es una dirección web (va a un
   * src), cuándo pasó a ENTREGADA y, del problema, cuánto llegó y el detalle.
   */
  private static Asentado asentado(LogisticaApi.Entrega e, DonacionesApi.Donacion d) {
    if (e == null) {
      return Asentado.NADA;
    }
    List<String> fotos = e.fotosRecepcion() == null ? List.of() : e.fotosRecepcion();
    String foto = fotos.isEmpty() ? e.fotoRecepcionUrl() : fotos.getFirst();
    boolean web = foto != null && (foto.startsWith("https://") || foto.startsWith("http://"));
    Instant confirmo =
        e.historialEstado() == null
            ? null
            : e.historialEstado().stream()
                .filter(c -> ENTREGADA.equals(c.estadoNuevo()) && c.timestamp() != null)
                .map(LogisticaApi.CambioDeEstado::timestamp)
                .max(Comparator.naturalOrder())
                .orElse(null);
    LogisticaApi.Reporte reporte = e.reporte();
    String llegoMenos =
        reporte == null || reporte.cantidadRecibida() == null
            ? null
            : NumberFormat.getNumberInstance(ES_AR).format(reporte.cantidadRecibida())
                + " de "
                + cantidad(d.cantidad(), categoria(d));
    String detalle =
        reporte == null || reporte.detalle() == null || reporte.detalle().isBlank()
            ? null
            : reporte.detalle().strip();
    String remito =
        e.numeroRemito() == null || e.numeroRemito().isBlank() ? null : e.numeroRemito().strip();
    return new Asentado(
        remito,
        web ? foto : null,
        confirmo == null ? null : confirmo.atZone(ARGENTINA).format(CUANDO),
        llegoMenos,
        detalle);
  }

  /** Lo asentado de una entrega confirmada; en una que espera, nada. */
  private record Asentado(
      String remito, String foto, String confirmo, String llegoMenos, String detalle) {
    static final Asentado NADA = new Asentado(null, null, null, null, null);
  }

  private static UUID comoId(String clave) {
    if (clave == null || clave.isBlank()) {
      return null;
    }
    try {
      return UUID.fromString(clave.strip());
    } catch (IllegalArgumentException noEsUnId) {
      return null;
    }
  }

  /**
   * Las asignadas de la entidad en {@code estados}, de a una página chica de Donaciones (R11-43):
   * con {@code id}, solo esa (si es de la entidad y está en uno de esos estados); sin {@code id},
   * las primeras {@code cuantas} en el orden de atención, la puerta primero. Ningún tope depende de
   * cuántas haya.
   */
  private List<DonacionesApi.Donacion> asignadas(
      UUID entidad, UUID id, List<String> estados, int cuantas) {
    MultiValueMap<String, String> filtros = new LinkedMultiValueMap<>();
    filtros.add("entidadId", entidad.toString());
    if (id != null) {
      filtros.add("id", id.toString());
    }
    estados.forEach(e -> filtros.add("estado", e));
    filtros.add("orden", "ATENCION");
    filtros.add("size", String.valueOf(cuantas));
    List<DonacionesApi.Donacion> pagina = donaciones.pagina(filtros).getBody();
    return (pagina == null ? List.<DonacionesApi.Donacion>of() : pagina)
        .stream().filter(d -> d.id() != null).toList();
  }

  private EntidadEntregaVista entrega(Seguimiento s, DonacionesApi.Donacion d, Asentado a) {
    Categoria categoria = categoria(d);
    // La unidad sola, la de Comun: "2 kg" sin el número; sin categoría, vacía.
    String unidad = cantidad(2, categoria).substring(1).strip();
    String cuanto = cantidad(d.cantidad(), categoria);
    String descripcion = Objects.requireNonNullElse(d.descripcion(), "Donación");
    String salio =
        s.horaSalida() == null ? null : "Salió del depósito el " + dia(s.horaSalida()) + ".";
    // La hora de llegada es la del cambio a EN_PUERTA en el historial de la donación (hora local);
    // una entrega en traslado todavía no llegó
    LocalDateTime llego = EN_TRASLADO.equals(s.estadoEntrega()) ? null : desde(d, PUERTA);
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
        a.remito(),
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
        a.foto(),
        a.confirmo(),
        d.cantidad(),
        unidad.isEmpty() ? "unidades" : unidad,
        "kg".equals(unidad) || "l".equals(unidad),
        a.llegoMenos(),
        a.detalle(),
        d.fechaRegistro() == null ? null : d.fechaRegistro().format(DIA_Y_HORA),
        paso(d, ASIGNADA),
        paso(d, LISTA));
  }

  /**
   * Cuándo la donación pasó a {@code estado} por última vez (hora local), del historial; si nunca
   * pasó, null: a diferencia de {@code Comun.desde}, no cae en la fecha de registro.
   */
  private static String paso(DonacionesApi.Donacion d, String estado) {
    return d.historial() == null
        ? null
        : d.historial().stream()
            .filter(c -> c.timestamp() != null && normalizado(c.estadoNuevo()).equals(estado))
            .map(DonacionesApi.Cambio::timestamp)
            .max(Comparator.naturalOrder())
            .map(t -> t.format(DIA_Y_HORA))
            .orElse(null);
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
    // En cualquier estado: una segunda confirmación recibe el 400 de Logística, no un 404
    // Solo la entrega de esa donación (R11-43); una clave que no es un id no llega a Logística
    UUID pedida = UUID.fromString(clave);
    UUID entrega =
        logistica
            .seguimientoDeEntidad(
                entidad, EntidadSeguimientoRest.deLasDonaciones(List.of(pedida.toString())))
            .stream()
            .filter(s -> s.idEntrega() != null && pedida.equals(s.idDonacion()))
            .min(Comparator.comparing(s -> "CANCELADA".equals(s.estadoEntrega())))
            .map(Seguimiento::idEntrega)
            .orElse(pedida);
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
    LogisticaApi.Reporte reporte = reporte(acta);
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

  private static LogisticaApi.Reporte reporte(Acta acta) {
    if (acta.motivo() == null) {
      return null;
    }
    String detalle =
        acta.detalle() == null || acta.detalle().isBlank() ? null : acta.detalle().strip();
    Double cantidad = "incompleta".equals(acta.motivo()) ? acta.cantidadRecibida() : null;
    return new LogisticaApi.Reporte(acta.motivo().toUpperCase(Locale.ROOT), detalle, cantidad);
  }
}
