package grupo5.cliente.rest;

import static grupo5.cliente.rest.Comun.DIA_Y_ANIO;

import grupo5.cliente.puertos.EntidadPerfilPuerto;
import grupo5.cliente.rest.DonacionesApi.Medio;
import grupo5.cliente.rest.DonacionesApi.Persona;
import grupo5.cliente.vistas.CuentaVista;
import grupo5.cliente.vistas.EntidadPerfilVista;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Mi perfil de la entidad desde Donaciones: {@code GET /api/entidades/{id}} (la entidad sobre sí) y
 * la constancia de {@code GET /api/personas/{id}/legales}. "Qué es" es el rubro; el tipo jurídico
 * EMPRESA no tiene opción en la pantalla.
 */
// @Primary solo para el análisis estático (S9352): la propiedad ya deja un único adaptador activo.
@Component
@Primary
@ConditionalOnProperty(name = "cliente.datos.entidad", havingValue = "rest")
class EntidadPerfilRest implements EntidadPerfilPuerto {

  private static final Map<String, String> TIPOS =
      Map.of("ONG", "asociacion", "INSTITUCION", "institucion", "GUBERNAMENTAL", "organismo");
  private static final String CORREO = "CORREO";

  private final DonacionesApi api;

  EntidadPerfilRest(DonacionesApi api) {
    this.api = api;
  }

  @Override
  public EntidadPerfilVista perfil(UUID entidad) {
    DonacionesApi.Entidad datos = api.entidad(entidad);
    Persona juridica = datos == null || datos.juridica() == null ? vacia() : datos.juridica();
    Persona representante =
        juridica.representantes() == null || juridica.representantes().isEmpty()
            ? vacia()
            : juridica.representantes().getFirst();
    String nombreRepresentante = unidos(" ", representante.nombre(), representante.apellido());
    String correoRepresentante = medio(representante, CORREO);
    Medio predeterminado =
        medios(juridica).stream()
            .filter(m -> Boolean.TRUE.equals(m.esPredeterminado()))
            .findFirst()
            .orElse(null);
    return new EntidadPerfilVista(
        juridica.razonSocial(),
        unidos(" · ", juridica.rubro(), unidos(", ", representante.nombre(), correoRepresentante)),
        juridica.rubro(),
        juridica.tipoJuridico() == null ? null : TIPOS.get(juridica.tipoJuridico()),
        direccion(juridica.direccion()),
        nombreRepresentante,
        correoRepresentante,
        medio(juridica, CORREO),
        medio(juridica, "WHATSAPP"),
        medio(juridica, "TELEFONO"),
        predeterminado == null || predeterminado.tipo() == null
            ? null
            : predeterminado.tipo().toLowerCase(Locale.ROOT),
        legales(api.legales(entidad)));
  }

  /** La razón social con un solo pedido a Donaciones, sin la constancia (R11-44). */
  @Override
  public CuentaVista cuenta(UUID entidad) {
    DonacionesApi.Entidad datos = api.entidad(entidad);
    return new CuentaVista(
        datos == null || datos.juridica() == null ? null : datos.juridica().razonSocial(),
        ORGANIZACION);
  }

  private static Persona vacia() {
    return new Persona(null, null, null, null, null, null, null, null, null, null, null, null);
  }

  private static List<Medio> medios(Persona persona) {
    return persona.mediosDeContacto() == null ? List.of() : persona.mediosDeContacto();
  }

  /** El primero de ese tipo: el correo, o el teléfono con su código de área. */
  private static String medio(Persona persona, String tipo) {
    return medios(persona).stream()
        .filter(m -> tipo.equals(m.tipo()))
        .map(
            m ->
                CORREO.equals(tipo) ? m.direccionCorreo() : unidos(" ", m.codigoArea(), m.numero()))
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
  }

  private static String direccion(DonacionesApi.Direccion d) {
    return d == null
        ? null
        : unidos(
            ", ",
            unidos(" ", d.calle(), d.altura() == null ? null : d.altura().toString()),
            d.localidad(),
            d.provincia());
  }

  private static String legales(DonacionesApi.Legales legales) {
    if (legales == null || legales.versionAceptada() == null || legales.aceptadaEn() == null) {
      return "Todavía no aceptaste los términos y la política de privacidad.";
    }
    return "Aceptaste la versión %d el %s."
        .formatted(legales.versionAceptada(), legales.aceptadaEn().format(DIA_Y_ANIO));
  }

  /** Las partes que hay, unidas; sin ninguna, null. */
  private static String unidos(String separador, String... partes) {
    String unido =
        Stream.of(partes)
            .filter(p -> p != null && !p.isBlank())
            .collect(Collectors.joining(separador));
    return unido.isEmpty() ? null : unido;
  }
}
