package grupo5.cliente.rest;

import static grupo5.cliente.rest.Comun.DIA_Y_ANIO;

import grupo5.cliente.puertos.DonantePerfilPuerto;
import grupo5.cliente.puertos.DonantePerfilPuerto.Tipo;
import grupo5.cliente.vistas.CuentaVista;
import grupo5.cliente.vistas.DonantePerfilVista;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

/**
 * Mi perfil del donante desde Donaciones (el donante con su persona, {@code mostrarNombre} y la
 * constancia de los términos) y la categoría de Incentivos, que es lo que ve la entidad si el
 * nombre está oculto. Sin Incentivos, lo que ve la entidad queda sin la categoría.
 */
// @Primary solo para el análisis estático (S9352): la propiedad ya deja un único adaptador activo.
@Component
@Primary
@ConditionalOnProperty(name = "cliente.datos.donante", havingValue = "rest")
class DonantePerfilRest implements DonantePerfilPuerto {

  private static final String JURIDICA = "JURIDICA";
  private static final String HUMANA = "HUMANA";

  private static final DonacionesApi.Persona SIN_PERSONA =
      new DonacionesApi.Persona(
          null, null, null, null, null, null, null, null, null, null, null, null);

  private static final DonacionesApi.Direccion SIN_DIRECCION =
      new DonacionesApi.Direccion(null, null, null, null, null, null, null, null);

  private final DonacionesApi api;
  private final IncentivosApi incentivos;

  DonantePerfilRest(DonacionesApi api, IncentivosApi incentivos) {
    this.api = api;
    this.incentivos = incentivos;
  }

  @Override
  public DonantePerfilVista perfil(UUID donante) {
    DonacionesApi.Donante d = api.donante(donante);
    // Sin persona o sin dirección, los campos quedan vacíos: así no hay un null por campo.
    DonacionesApi.Persona p = d.persona() == null ? SIN_PERSONA : d.persona();
    DonacionesApi.Direccion dir = p.direccion() == null ? SIN_DIRECCION : p.direccion();
    // Sin tipo no es empresa (R11-06): solo JURIDICA lleva razón social y CUIT.
    boolean empresa = tipo(p) == Tipo.EMPRESA;
    String tipo = comoDonante(p);
    List<DonacionesApi.Medio> medios =
        p.mediosDeContacto() == null ? List.of() : p.mediosDeContacto();
    String correo = medio(medios, "CORREO");
    return new DonantePerfilVista(
        nombre(p),
        correo == null ? tipo : tipo + " · " + correo,
        anonimo(empresa ? tipo : null, categoria(donante)),
        Boolean.FALSE.equals(d.mostrarNombre()),
        empresa ? p.rubro() : null,
        empresa ? p.documento() : null,
        dir.calle(),
        dir.altura() == null ? null : dir.altura().toString(),
        piso(dir),
        dir.codigoPostal(),
        dir.localidad(),
        dir.provincia(),
        correo,
        medio(medios, "WHATSAPP"),
        medio(medios, "TELEFONO"),
        legales(donante),
        empresa ? null : p.nombre(),
        empresa ? null : p.apellido(),
        empresa ? null : documento(p));
  }

  /** El nombre y el tipo de donante con un solo pedido a Donaciones (R11-44). */
  @Override
  public CuentaVista cuenta(UUID donante) {
    DonacionesApi.Donante d = api.donante(donante);
    DonacionesApi.Persona p = d == null || d.persona() == null ? SIN_PERSONA : d.persona();
    return new CuentaVista(nombre(p), comoDonante(p));
  }

  /**
   * Qué tipo de donante es, en palabras (R11-45): la persona jurídica dice su tipo jurídico y nunca
   * "organización", que es quien recibe. Sin tipo jurídico, empresa, como hasta ahora.
   */
  private static String comoDonante(DonacionesApi.Persona p) {
    return switch (tipo(p)) {
      case PERSONA -> "Persona donante";
      case SIN_TIPO -> "Donante";
      case EMPRESA ->
          switch (p.tipoJuridico() == null ? "" : p.tipoJuridico()) {
            case "ONG" -> "ONG donante";
            case "INSTITUCION" -> "Institución donante";
            case "GUBERNAMENTAL" -> "Donante gubernamental";
            default -> "Empresa donante";
          };
    };
  }

  @Override
  public Tipo tipo(UUID donante) {
    DonacionesApi.Donante d = api.donante(donante);
    return tipo(d == null || d.persona() == null ? SIN_PERSONA : d.persona());
  }

  private static Tipo tipo(DonacionesApi.Persona p) {
    if (JURIDICA.equals(p.tipo())) {
      return Tipo.EMPRESA;
    }
    return HUMANA.equals(p.tipo()) ? Tipo.PERSONA : Tipo.SIN_TIPO;
  }

  /** "DNI 12.345.678"; un número con otros signos, como viene. */
  private static String documento(DonacionesApi.Persona p) {
    if (p.documento() == null || p.documento().isBlank()) {
      return null;
    }
    String numero =
        p.documento().matches("\\d{4,}")
            ? String.format(Comun.ES_AR, "%,d", Long.parseLong(p.documento()))
            : p.documento();
    return p.tipoDocumento() == null || p.tipoDocumento().isBlank()
        ? numero
        : p.tipoDocumento() + " " + numero;
  }

  /**
   * Lo que ve la organización sin el nombre: "Empresa donante · Sostenedor" (o el tipo jurídico que
   * tenga: "ONG donante · Sostenedor") o "Donante Sostenedor".
   */
  private static String anonimo(String juridica, String categoria) {
    if (juridica != null) {
      return categoria == null ? juridica : juridica + " · " + categoria;
    }
    return categoria == null ? "Donante" : "Donante " + categoria;
  }

  @Override
  public void mostrarNombre(UUID donante, boolean mostrar) {
    api.cambiarPreferencias(donante, new DonacionesApi.Preferencias(mostrar));
  }

  /** La razón social de la empresa; si no, nombre y apellido o, sin ninguno, "Tu cuenta". */
  private static String nombre(DonacionesApi.Persona p) {
    if (JURIDICA.equals(p.tipo())) {
      return p.razonSocial();
    }
    String nombre =
        Stream.of(p.nombre(), p.apellido())
            .filter(v -> v != null && !v.isBlank())
            .collect(Collectors.joining(" "));
    return nombre.isEmpty() ? "Tu cuenta" : nombre;
  }

  /** "SOSTENEDOR" se lee "Sostenedor"; sin Incentivos, null. */
  private String categoria(UUID donante) {
    try {
      IncentivosApi.PerfilDeDonante perfil = incentivos.donante(donante);
      String categoria = perfil == null ? null : perfil.categoria();
      return categoria == null || categoria.isBlank()
          ? null
          : categoria.charAt(0) + categoria.substring(1).toLowerCase(Locale.ROOT);
    } catch (RestClientException sinIncentivos) {
      return null;
    }
  }

  /** El correo, o el teléfono con su código de área, del primer medio de ese tipo. */
  private static String medio(List<DonacionesApi.Medio> medios, String tipo) {
    return medios.stream()
        .filter(m -> tipo.equals(m.tipo()))
        .map(
            m ->
                "CORREO".equals(tipo)
                    ? m.direccionCorreo()
                    : Stream.of(m.codigoArea(), m.numero())
                        .filter(Objects::nonNull)
                        .collect(Collectors.joining(" ")))
        .filter(v -> v != null && !v.isBlank())
        .findFirst()
        .orElse(null);
  }

  /** "3.º B"; sin piso, solo el departamento. */
  private static String piso(DonacionesApi.Direccion dir) {
    String piso = dir.piso() == null ? null : dir.piso() + ".º";
    String texto =
        Stream.of(piso, dir.departamento())
            .filter(v -> v != null && !v.isBlank())
            .collect(Collectors.joining(" "));
    return texto.isEmpty() ? null : texto;
  }

  /** La constancia; sin la versión aceptada, lo dice. */
  private String legales(UUID donante) {
    DonacionesApi.Legales legales = api.legales(donante);
    if (legales == null || legales.versionAceptada() == null || legales.aceptadaEn() == null) {
      return "Todavía no aceptaste los términos y la política de privacidad.";
    }
    return "Aceptaste la versión "
        + legales.versionAceptada()
        + " el "
        + legales.aceptadaEn().format(DIA_Y_ANIO)
        + ".";
  }
}
