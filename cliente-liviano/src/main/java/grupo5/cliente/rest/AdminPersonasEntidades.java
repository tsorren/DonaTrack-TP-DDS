package grupo5.cliente.rest;

import grupo5.cliente.cuentas.SolicitudesDeEntidad;
import grupo5.cliente.cuentas.SolicitudesDeEntidad.Pendiente;
import grupo5.cliente.rest.DonacionesApi.Direccion;
import grupo5.cliente.rest.DonacionesApi.Medio;
import grupo5.cliente.rest.DonacionesApi.Persona;
import grupo5.cliente.rest.DonacionesApi.Subcategoria;
import grupo5.cliente.vistas.AdminEntidadesVista;
import grupo5.cliente.vistas.AdminEntidadesVista.Entidad;
import grupo5.cliente.vistas.AdminEntidadesVista.Necesidad;
import grupo5.cliente.vistas.Tramo;
import java.text.Collator;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Las entidades del admin con lo que da Donaciones, de a una página (R11-01): la página pedida de
 * las registradas (activas y dadas de baja), buscadas por nombre en el servidor, con sus
 * necesidades vigentes; en la página 1, antes, las que piden sumarse (las solicitudes de Keycloak)
 * con los datos de su persona jurídica, leídas por id. Cuántas activas hay es un número del
 * servicio, no el largo de la página. Lo que nadie guarda (la misión, la fecha de alta y de baja,
 * las entregas confirmadas, el motivo de la baja, la constancia) queda null.
 *
 * <p>Desde R11-43 vuelven los filtros Estado, Necesidades y Tipo: los de las registradas los
 * resuelve Donaciones ({@code estado=ACTIVA|BAJA}, {@code conNecesidades}, {@code rubro}); las que
 * piden sumarse siguen saliendo de Keycloak y se filtran acá (no tienen necesidades; su tipo es el
 * rubro de su jurídica). Con "Pendientes" no se piden registradas; con "Activas" o "Inactivas", no
 * van las que piden sumarse. Las cifras de cada estado son dos números del servicio ({@code
 * size=1}) y las solicitudes: no dependen de los filtros.
 */
final class AdminPersonasEntidades {

  private static final String ACTIVA = "activa";
  private static final String SIZE = "size";
  private static final String ESTADO = "estado";
  private static final String ACTIVA_EN_DONACIONES = "ACTIVA";
  private static final String BAJA = "BAJA";
  private static final String PENDIENTES = "pendientes";

  /** De a 20 entidades por página, como los donantes (ficha, decisiones de R11). */
  static final int POR_PAGINA = 20;

  /** El tamaño de página más grande que acepta Donaciones: las personas se piden de a este lote. */
  private static final int TOPE = DonacionesApi.TOPE;

  private final DonacionesApi donaciones;
  private final ObjectProvider<SolicitudesDeEntidad> solicitudes;

  AdminPersonasEntidades(
      DonacionesApi donaciones, ObjectProvider<SolicitudesDeEntidad> solicitudes) {
    this.donaciones = donaciones;
    this.solicitudes = solicitudes;
  }

  AdminEntidadesVista entidades(
      String al, String buscar, AdminEntidadesVista.Filtro filtro, int pagina) {
    String nombre = buscar == null ? "" : buscar.strip();
    Collator orden = Collator.getInstance(Comun.ES_AR);
    List<Pendiente> todasLasPendientes = pendientesDeKeycloak();
    // Las que piden sumarse no tienen necesidades ni van con "Activas" o "Inactivas"
    boolean conPendientes =
        (filtro.ver().isEmpty() || PENDIENTES.equals(filtro.ver()))
            && !"con".equals(filtro.pedidos());
    List<Pendiente> buscadas = conPendientes ? buscadas(todasLasPendientes, nombre) : List.of();
    if (PENDIENTES.equals(filtro.ver())) {
      // Solo las que piden sumarse, de a una página: la lista es la que dio Keycloak
      Tramo<Entidad> tramo =
          Tramo.de(delTipo(pendientes(buscadas, orden), filtro), pagina, POR_PAGINA);
      return new AdminEntidadesVista(
          cuantas(ACTIVA_EN_DONACIONES),
          al,
          tramo.filas(),
          new AdminEntidadesVista.EnPaginas(
              nombre, tramo.total(), tramo, cuantas(BAJA), todasLasPendientes.size()));
    }
    Tramo<DonacionesApi.Entidad> tramo =
        Paginas.pedir(
            pagina, POR_PAGINA, numero -> donaciones.entidades(filtros(nombre, filtro, numero)));
    Map<UUID, List<Necesidad>> necesidades =
        tramo.filas().isEmpty() ? Map.of() : necesidadesVigentes();
    List<Entidad> registradas =
        tramo.filas().stream()
            .map(e -> registrada(e, necesidades.getOrDefault(e.id(), List.of())))
            .sorted(Comparator.comparing(Entidad::nombre, orden))
            .toList();
    // Sus datos (y su tipo) se leen solo si se muestran o si hay que filtrarlas por tipo
    boolean leerPendientes = tramo.pagina() == 1 || !filtro.tipo().isEmpty();
    List<Entidad> pendientes =
        leerPendientes ? delTipo(pendientes(buscadas, orden), filtro) : List.of();
    long cuantasPendientes = leerPendientes ? pendientes.size() : buscadas.size();
    List<Entidad> todas = new ArrayList<>(tramo.pagina() == 1 ? pendientes : List.of());
    todas.addAll(registradas);
    Tramo<Entidad> paginada =
        new Tramo<>(registradas, tramo.total(), tramo.pagina(), tramo.paginas());
    return new AdminEntidadesVista(
        cuantas(ACTIVA_EN_DONACIONES),
        al,
        todas,
        new AdminEntidadesVista.EnPaginas(
            nombre,
            tramo.total() + cuantasPendientes,
            paginada,
            cuantas(BAJA),
            todasLasPendientes.size()));
  }

  /** Las del rubro del filtro Tipo; sin él, todas. */
  private static List<Entidad> delTipo(List<Entidad> entidades, AdminEntidadesVista.Filtro filtro) {
    return filtro.tipo().isEmpty()
        ? entidades
        : entidades.stream().filter(e -> filtro.tipo().equals(e.tipo())).toList();
  }

  private static MultiValueMap<String, String> filtros(
      String nombre, AdminEntidadesVista.Filtro filtro, int pagina) {
    MultiValueMap<String, String> filtros = new LinkedMultiValueMap<>();
    if (!nombre.isEmpty()) {
      filtros.add("nombre", nombre);
    }
    switch (filtro.ver()) {
      case "activas" -> filtros.add(ESTADO, ACTIVA_EN_DONACIONES);
      case "inactivas" -> filtros.add(ESTADO, BAJA);
      default -> {
        // Todas: activas y dadas de baja
      }
    }
    if (!filtro.pedidos().isEmpty()) {
      filtros.add("conNecesidades", String.valueOf("con".equals(filtro.pedidos())));
    }
    if (!filtro.tipo().isEmpty()) {
      filtros.add("rubro", filtro.tipo());
    }
    filtros.add("page", String.valueOf(pagina));
    filtros.add(SIZE, String.valueOf(POR_PAGINA));
    return filtros;
  }

  /**
   * Cuántas registradas hay en {@code estado} ({@code ACTIVA} o {@code BAJA}): el total de una
   * página de una, sin traer la lista ni contar sobre una página (R11-43).
   */
  private long cuantas(String estado) {
    MultiValueMap<String, String> filtros = new LinkedMultiValueMap<>();
    filtros.add(ESTADO, estado);
    filtros.add(SIZE, "1");
    ResponseEntity<List<DonacionesApi.Entidad>> respuesta = donaciones.entidades(filtros);
    return Paginas.total(respuesta, List.of(), 1, 1);
  }

  /** Las que piden sumarse y cumplen la búsqueda, con los datos de su jurídica, por nombre. */
  private List<Entidad> pendientes(List<Pendiente> pendientes, Collator orden) {
    if (pendientes.isEmpty()) {
      return List.of();
    }
    Map<UUID, Persona> juridicas = personas(pendientes);
    return pendientes.stream()
        .map(p -> pendiente(p, juridicas.get(p.personaId())))
        .sorted(Comparator.comparing(Entidad::nombre, orden))
        .toList();
  }

  private List<Pendiente> pendientesDeKeycloak() {
    SolicitudesDeEntidad cuentas = solicitudes.getIfAvailable();
    return cuentas == null ? List.of() : cuentas.pendientes();
  }

  /** Las solicitudes cuyo nombre contiene {@code nombre}, sin distinguir mayúsculas ni tildes. */
  private static List<Pendiente> buscadas(List<Pendiente> pendientes, String nombre) {
    String buscado = sinTildes(nombre);
    return pendientes.stream()
        .filter(p -> buscado.isEmpty() || sinTildes(p.razonSocial()).contains(buscado))
        .toList();
  }

  private static String sinTildes(String texto) {
    return Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toLowerCase(Comun.ES_AR);
  }

  /** Los nombres de las entidades que piden sumarse, como los traen sus solicitudes. */
  List<String> pendientes() {
    return pendientesDeKeycloak().stream().map(Pendiente::razonSocial).toList();
  }

  /**
   * Solo las personas de las solicitudes, por su identificador (R11-01), de a {@link #TOPE}: con el
   * CSV hay miles de jurídicas donantes y no se traen todas para cruzarlas.
   */
  private Map<UUID, Persona> personas(List<Pendiente> pendientes) {
    List<String> ids =
        pendientes.stream()
            .map(Pendiente::personaId)
            .filter(Objects::nonNull)
            .map(UUID::toString)
            .distinct()
            .toList();
    Map<UUID, Persona> juridicas = new HashMap<>();
    for (int desde = 0; desde < ids.size(); desde += TOPE) {
      MultiValueMap<String, String> lote = new LinkedMultiValueMap<>();
      ids.subList(desde, Math.min(ids.size(), desde + TOPE)).forEach(id -> lote.add("id", id));
      lote.add(SIZE, String.valueOf(TOPE));
      Objects.requireNonNullElse(donaciones.personas(lote).getBody(), List.<Persona>of()).stream()
          .filter(p -> p.id() != null)
          .forEach(p -> juridicas.putIfAbsent(p.id(), p));
    }
    return juridicas;
  }

  /** Aprueba la solicitud de la cuenta {@code clave} y devuelve el nombre de la entidad. */
  String aprobar(String clave) {
    SolicitudesDeEntidad cuentas = solicitudes.getObject();
    String nombre =
        cuentas.pendientes().stream()
            .filter(p -> p.cuentaId().equals(clave))
            .map(Pendiente::razonSocial)
            .findFirst()
            .orElseThrow();
    cuentas.aprobar(clave);
    return nombre;
  }

  private Map<UUID, List<Necesidad>> necesidadesVigentes() {
    MultiValueMap<String, String> filtros = new LinkedMultiValueMap<>();
    filtros.add("estado", "VIGENTE");
    List<DonacionesApi.Necesidad> vigentes = donaciones.necesidades(filtros);
    if (vigentes.isEmpty()) {
      return Map.of();
    }
    Map<UUID, String> categorias =
        donaciones.subcategorias().stream()
            .filter(s -> s.id() != null && s.categoria() != null && s.categoria().nombre() != null)
            .collect(Collectors.toMap(Subcategoria::id, s -> s.categoria().nombre(), (a, b) -> a));
    return vigentes.stream()
        .filter(n -> n.idEntidad() != null)
        .collect(
            Collectors.groupingBy(
                DonacionesApi.Necesidad::idEntidad,
                Collectors.mapping(n -> necesidad(n, categorias), Collectors.toList())));
  }

  private static Necesidad necesidad(DonacionesApi.Necesidad n, Map<UUID, String> categorias) {
    String categoria = categorias.get(n.idSubcategoria());
    String unidad = n.unidad();
    String cuanto =
        Comun.cantidad(
            n.cantidadNecesitada() == null ? 0 : n.cantidadNecesitada(),
            new DonacionesApi.Categoria(null, null, unidad, null, null, null));
    String hasta = n.fechaLimite() == null ? "" : ", hasta el " + Comun.DIA.format(n.fechaLimite());
    return new Necesidad(
        n.descripcion() == null ? "" : n.descripcion(), icono(categoria), "Pide " + cuanto + hasta);
  }

  /** El nombre del ícono sin el prefijo del sprite ({@code alimentos}), o null si no hay. */
  private static String icono(String categoria) {
    String icono = categoria == null ? null : Comun.icono(categoria);
    return icono == null ? null : icono.substring("cat-".length());
  }

  private static Entidad registrada(DonacionesApi.Entidad e, List<Necesidad> necesidades) {
    Persona juridica = e.juridica();
    boolean activa = !Boolean.FALSE.equals(e.activo());
    return new Entidad(
        String.valueOf(e.id()),
        activa ? ACTIVA : "inactiva",
        AdminPersonasRest.nombre(juridica),
        tipo(juridica),
        necesidades.isEmpty() ? null : necesidades.get(0).cat(),
        localidad(juridica),
        responsable(juridica),
        correo(juridica),
        null,
        activa ? necesidades : List.of(),
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  private static Entidad pendiente(Pendiente p, Persona juridica) {
    return new Entidad(
        p.cuentaId(),
        "pendiente",
        p.razonSocial(),
        tipo(juridica),
        null,
        localidad(juridica),
        responsable(juridica),
        p.correo(),
        null,
        List.of(),
        null,
        null,
        null,
        null,
        null,
        null,
        juridica == null ? null : juridica.razonSocial(),
        juridica == null ? null : juridica.documento(),
        direccion(juridica),
        null,
        null,
        null,
        null);
  }

  /** El rubro, si lo cargó; si no, el tipo de organización del contrato. */
  private static String tipo(Persona juridica) {
    if (juridica != null && juridica.rubro() != null && !juridica.rubro().isBlank()) {
      return juridica.rubro();
    }
    String tipo = juridica == null ? null : juridica.tipoJuridico();
    return switch (tipo == null ? "" : tipo) {
      case "ONG" -> "ONG";
      case "INSTITUCION" -> "Institución";
      case "GUBERNAMENTAL" -> "Gubernamental";
      default -> "Organización";
    };
  }

  private static String localidad(Persona persona) {
    Direccion d = persona == null ? null : persona.direccion();
    return d == null || d.localidad() == null ? "" : d.localidad();
  }

  private static String direccion(Persona persona) {
    Direccion d = persona == null ? null : persona.direccion();
    if (d == null || d.calle() == null) {
      return null;
    }
    String calle = d.altura() == null ? d.calle() : d.calle() + " " + d.altura();
    return Stream.of(calle, d.localidad(), d.provincia())
        .filter(parte -> parte != null && !parte.isBlank())
        .collect(Collectors.joining(", "));
  }

  private static String responsable(Persona juridica) {
    if (juridica == null || juridica.representantes() == null) {
      return null;
    }
    return juridica.representantes().stream()
        .map(AdminPersonasRest::nombre)
        .filter(nombre -> !nombre.isBlank())
        .findFirst()
        .orElse(null);
  }

  private static String correo(Persona persona) {
    if (persona == null || persona.mediosDeContacto() == null) {
      return null;
    }
    return persona.mediosDeContacto().stream()
        .filter(m -> "CORREO".equals(m.tipo()) && m.direccionCorreo() != null)
        .map(Medio::direccionCorreo)
        .findFirst()
        .orElse(null);
  }
}
