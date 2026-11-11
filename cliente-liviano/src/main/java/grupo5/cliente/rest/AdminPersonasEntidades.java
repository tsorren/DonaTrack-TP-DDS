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
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Las entidades del admin con lo que da Donaciones: todas las entidades (activas y dadas de baja),
 * sus necesidades vigentes y, de las que piden sumarse (las solicitudes de Keycloak), los datos de
 * su persona jurídica. Lo que nadie guarda (la misión, la fecha de alta y de baja, las entregas
 * confirmadas, el motivo de la baja, la constancia) queda null.
 */
final class AdminPersonasEntidades {

  private static final String ACTIVA = "activa";
  private static final String JURIDICA = "JURIDICA";

  private final DonacionesApi donaciones;
  private final ObjectProvider<SolicitudesDeEntidad> solicitudes;

  AdminPersonasEntidades(
      DonacionesApi donaciones, ObjectProvider<SolicitudesDeEntidad> solicitudes) {
    this.donaciones = donaciones;
    this.solicitudes = solicitudes;
  }

  AdminEntidadesVista entidades(String al) {
    List<DonacionesApi.Entidad> registradas =
        Objects.requireNonNullElse(
            donaciones.entidades(new LinkedMultiValueMap<>()).getBody(), List.of());
    Map<UUID, List<Necesidad>> necesidades = necesidadesVigentes();
    List<Entidad> todas = new ArrayList<>();
    registradas.forEach(e -> todas.add(registrada(e, necesidades.getOrDefault(e.id(), List.of()))));
    SolicitudesDeEntidad cuentas = solicitudes.getIfAvailable();
    List<Pendiente> pendientes = cuentas == null ? List.of() : cuentas.pendientes();
    if (!pendientes.isEmpty()) {
      Map<UUID, Persona> juridicas =
          donaciones.personas(JURIDICA).stream()
              .filter(p -> p.id() != null)
              .collect(Collectors.toMap(Persona::id, Function.identity(), (a, b) -> a));
      pendientes.forEach(p -> todas.add(pendiente(p, juridicas.get(p.personaId()))));
    }
    Collator orden = Collator.getInstance(Comun.ES_AR);
    todas.sort(Comparator.comparing(Entidad::nombre, orden));
    long activas = todas.stream().filter(e -> ACTIVA.equals(e.estado())).count();
    return new AdminEntidadesVista(activas, al, todas);
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
