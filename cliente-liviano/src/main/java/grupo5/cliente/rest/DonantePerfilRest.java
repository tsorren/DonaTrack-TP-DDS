package grupo5.cliente.rest;

import static grupo5.cliente.rest.Comun.DIA_Y_ANIO;

import grupo5.cliente.puertos.DonantePerfilPuerto;
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
    boolean empresa = p.tipo() == null || JURIDICA.equals(p.tipo());
    String tipo = empresa ? "Empresa donante" : "Persona donante";
    List<DonacionesApi.Medio> medios =
        p.mediosDeContacto() == null ? List.of() : p.mediosDeContacto();
    String correo = medio(medios, "CORREO");
    return new DonantePerfilVista(
        nombre(p),
        correo == null ? tipo : tipo + " · " + correo,
        anonimo(empresa, categoria(donante)),
        Boolean.FALSE.equals(d.mostrarNombre()),
        p.rubro(),
        p.documento(),
        dir.calle(),
        dir.altura() == null ? null : dir.altura().toString(),
        piso(dir),
        dir.codigoPostal(),
        dir.localidad(),
        dir.provincia(),
        correo,
        medio(medios, "WHATSAPP"),
        medio(medios, "TELEFONO"),
        legales(donante));
  }

  /** Lo que ve la entidad sin el nombre: "Empresa donante · Sostenedor" o "Donante Sostenedor". */
  private static String anonimo(boolean empresa, String categoria) {
    if (empresa) {
      return categoria == null ? "Empresa donante" : "Empresa donante · " + categoria;
    }
    return categoria == null ? "Donante" : "Donante " + categoria;
  }

  @Override
  public void mostrarNombre(UUID donante, boolean mostrar) {
    api.cambiarPreferencias(donante, new DonacionesApi.Preferencias(mostrar));
  }

  private static String nombre(DonacionesApi.Persona p) {
    return JURIDICA.equals(p.tipo())
        ? p.razonSocial()
        : Stream.of(p.nombre(), p.apellido())
            .filter(Objects::nonNull)
            .collect(Collectors.joining(" "));
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
