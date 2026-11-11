package grupo5.cliente.rest;

import grupo5.cliente.puertos.AdminPersonasPuerto;
import grupo5.cliente.puertos.PersonaNueva;
import grupo5.cliente.rest.DonacionesApi.Archivo;
import grupo5.cliente.rest.DonacionesApi.Persona;
import grupo5.cliente.vistas.AdminDonantesVista;
import grupo5.cliente.vistas.AdminDonantesVista.Donante;
import grupo5.cliente.vistas.AdminDonantesVista.FilaConError;
import grupo5.cliente.vistas.AdminDonantesVista.Importacion;
import java.text.NumberFormat;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Los donantes y las entidades del admin desde Donaciones (con el token del admin). Lo que
 * Donaciones no guarda (la fecha de alta, qué dona, la fecha de fin de una importación) queda null
 * y la pantalla no lo muestra.
 */
// @Primary solo para el análisis estático (S9352): la propiedad ya deja un único adaptador activo.
@Component
@Primary
@ConditionalOnProperty(name = "cliente.datos.admin-personas", havingValue = "rest")
class AdminPersonasRest implements AdminPersonasPuerto {

  private static final DateTimeFormatter AL =
      DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy, HH:mm", Comun.ES_AR);
  private static final String JURIDICA = "JURIDICA";
  private static final int FILAS_A_LA_VISTA = 20;

  private final DonacionesApi donaciones;
  private final Clock reloj;

  AdminPersonasRest(DonacionesApi donaciones, Clock reloj) {
    this.donaciones = donaciones;
    this.reloj = reloj;
  }

  private String ahora() {
    return AL.format(LocalDateTime.now(reloj.withZone(Comun.ARGENTINA)));
  }

  private static String numero(Integer valor) {
    return NumberFormat.getIntegerInstance(Comun.ES_AR).format(valor == null ? 0 : valor);
  }

  // --- Donantes --------------------------------------------------------------------------------

  @Override
  public AdminDonantesVista donantes() {
    List<Donante> todos =
        donaciones.donantes(null).stream().map(AdminPersonasRest::donante).toList();
    List<String> nuevos = todos.stream().map(Donante::clave).toList();
    List<String> porNombre =
        todos.stream()
            .sorted(Comparator.comparing(Donante::nombre, String.CASE_INSENSITIVE_ORDER))
            .map(Donante::clave)
            .toList();
    long activos = todos.stream().filter(d -> "activo".equals(d.estado())).count();
    return new AdminDonantesVista(
        activos, ahora(), todos, nuevos, nuevos.reversed(), porNombre, "la lista");
  }

  private static Donante donante(DonacionesApi.Donante donante) {
    Persona persona = donante.persona();
    String[] tipo = tipo(persona);
    String estado = Boolean.FALSE.equals(donante.activo()) ? "baja" : "activo";
    return new Donante(
        String.valueOf(donante.idDonante()),
        nombre(persona),
        tipo[0],
        tipo[1],
        documento(persona),
        null,
        estado,
        null,
        null);
  }

  static String nombre(Persona persona) {
    if (persona == null) {
      return "";
    }
    if (JURIDICA.equals(persona.tipo())) {
      return persona.razonSocial() == null ? "" : persona.razonSocial();
    }
    String completo =
        (persona.nombre() == null ? "" : persona.nombre())
            + " "
            + (persona.apellido() == null ? "" : persona.apellido());
    return completo.trim();
  }

  /** El tipo del filtro y su texto: una persona, una empresa o cualquier otra organización. */
  private static String[] tipo(Persona persona) {
    if (persona == null || !JURIDICA.equals(persona.tipo())) {
      return new String[] {"persona", "Persona"};
    }
    return "EMPRESA".equals(persona.tipoJuridico())
        ? new String[] {"empresa", "Empresa"}
        : new String[] {"organizacion", "Organización"};
  }

  private static String documento(Persona persona) {
    if (persona == null || persona.documento() == null) {
      return "";
    }
    return persona.tipoDocumento() == null
        ? persona.documento()
        : persona.tipoDocumento() + " " + persona.documento();
  }

  @Override
  public void registrarDonante(PersonaNueva persona) {
    Persona creada = donaciones.crearPersona(persona);
    donaciones.registrarDonante(new DonacionesApi.DonanteNuevo(creada.id()));
  }

  // --- Importar donantes -----------------------------------------------------------------------

  @Override
  public UUID importar(MultipartFile archivo) {
    MultiValueMap<String, Object> partes = new LinkedMultiValueMap<>();
    partes.add("archivo", archivo.getResource());
    return donaciones.importarDonantes(partes).id();
  }

  @Override
  public Importacion importacion(UUID id, String nombre) {
    if (id == null) {
      return null;
    }
    Archivo avance = donaciones.archivo(id);
    String accion =
        switch (avance.estado() == null ? "" : avance.estado()) {
          case "PROCESADO", "PROCESADO_CON_ERRORES" -> "importado";
          case "ERROR" -> "importar-error";
          default -> "importando";
        };
    List<FilaConError> filas =
        "importado".equals(accion) && avance.conError() != null && avance.conError() > 0
            ? donaciones.filasConError(id, 0, FILAS_A_LA_VISTA).stream()
                .map(f -> new FilaConError("Fila " + numero(f.linea()), f.motivo()))
                .toList()
            : List.of();
    String resultado =
        numero(avance.procesadas())
            + " filas leídas: "
            + numero(avance.creadas())
            + " donantes nuevos, "
            + numero(avance.actualizadas())
            + " actualizados y "
            + numero(avance.conError())
            + " filas con error.";
    String ver =
        UriComponentsBuilder.fromPath("/admin-donantes-importado")
            .queryParam("archivo", id)
            .queryParamIfPresent("nombre", Optional.ofNullable(nombre))
            .encode()
            .toUriString();
    return new Importacion(
        accion,
        nombre == null ? "El archivo" : nombre,
        avance.totalFilas() == null ? null : numero(avance.totalFilas()),
        numero(avance.procesadas()),
        null,
        resultado,
        numero(avance.conError()),
        filas,
        ver);
  }
}
