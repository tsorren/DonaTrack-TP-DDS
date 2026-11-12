package grupo5.cliente.rest;

import grupo5.cliente.cuentas.SolicitudesDeEntidad;
import grupo5.cliente.puertos.AdminPersonasPuerto;
import grupo5.cliente.puertos.DescargaDeFilas;
import grupo5.cliente.puertos.PersonaNueva;
import grupo5.cliente.rest.DonacionesApi.Archivo;
import grupo5.cliente.rest.DonacionesApi.Persona;
import grupo5.cliente.vistas.AdminDonantesVista;
import grupo5.cliente.vistas.AdminDonantesVista.Contacto;
import grupo5.cliente.vistas.AdminDonantesVista.Donante;
import grupo5.cliente.vistas.AdminDonantesVista.FilaConError;
import grupo5.cliente.vistas.AdminDonantesVista.Importacion;
import grupo5.cliente.vistas.AdminDonantesVista.Opcion;
import grupo5.cliente.vistas.AdminDonantesVista.Opciones;
import grupo5.cliente.vistas.AdminDonantesVista.Reciente;
import grupo5.cliente.vistas.AdminDonantesVista.SusDonaciones;
import grupo5.cliente.vistas.AdminDonantesVista.Ultimo;
import grupo5.cliente.vistas.AdminEntidadesVista;
import grupo5.cliente.vistas.AdminImportacionesVista;
import grupo5.cliente.vistas.AdminInicioVista;
import grupo5.cliente.vistas.ConsultaDonantes;
import grupo5.cliente.vistas.ConsultaImportaciones;
import grupo5.cliente.vistas.Tramo;
import java.text.Collator;
import java.text.NumberFormat;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
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

  /** El tipo del renglón de una persona jurídica que no es empresa (sin texto en pantalla). */
  private static final String JURIDICA_NO_EMPRESA = "organizacion";

  private static final DateTimeFormatter HORA =
      DateTimeFormatter.ofPattern("HH:mm:ss", Comun.ES_AR);
  private static final String IMPORTANDO = "importando";
  private static final String IMPORTADO = "importado";
  private static final String ACTIVO = "activo";
  private static final String NOMBRE = "nombre";
  private static final String PERSONA = "persona";

  /** De a 20 donantes y de a 20 filas con error por página (ficha, decisiones de R11). */
  static final int POR_PAGINA = 20;

  private static final int FILAS_POR_PAGINA = 20;

  /** De a 3 (R11-34): las donaciones más recientes de la ficha del donante. */
  private static final int RECIENTES = 3;

  private static final int ULTIMOS = 5;
  private static final String MOTIVO_INESPERADO =
      "No pudimos cargar esta fila por un problema nuestro. Probá subirla de nuevo.";
  private static final Map<String, String> MOTIVOS =
      Map.of(
          "FORMATO_INVALIDO", "La fila no tiene las mismas columnas que el encabezado.",
          "DATOS_INVALIDOS",
              "Hay un dato que no pudimos leer: el tipo de persona, el documento o la fecha.",
          "DONANTE_DADO_DE_BAJA", "Es de un donante dado de baja: no lo cambiamos.",
          "PERSONA_DUPLICADA_EN_CONFLICTO", "El correo ya es de otro donante, con otro documento.",
          "ERROR_INESPERADO", MOTIVO_INESPERADO);

  private final DonacionesApi donaciones;
  private final Clock reloj;
  private final AdminPersonasEntidades entidades;
  private final AdminPersonasInicio inicio;
  private final AdminPersonasImportaciones importaciones;

  /**
   * Las solicitudes de entidad las da Keycloak, fuera de la maqueta; sin ellas, no hay pendientes.
   */
  AdminPersonasRest(
      DonacionesApi donaciones,
      LogisticaApi logistica,
      Clock reloj,
      ObjectProvider<SolicitudesDeEntidad> solicitudes) {
    this.donaciones = donaciones;
    this.reloj = reloj;
    this.entidades = new AdminPersonasEntidades(donaciones, solicitudes);
    this.inicio = new AdminPersonasInicio(donaciones, logistica);
    this.importaciones = new AdminPersonasImportaciones(donaciones);
  }

  @Override
  public AdminInicioVista inicio() {
    LocalDateTime ahora = LocalDateTime.now(reloj.withZone(Comun.ARGENTINA));
    String al = AL.format(ahora);
    return inicio.inicio(ahora, al, entidades.pendientes());
  }

  @Override
  public AdminEntidadesVista entidades(
      String buscar, AdminEntidadesVista.Filtro filtro, int pagina) {
    return entidades.entidades(ahora(), buscar, filtro, pagina);
  }

  @Override
  public String aprobarEntidad(String clave) {
    return entidades.aprobar(clave);
  }

  private String ahora() {
    return AL.format(LocalDateTime.now(reloj.withZone(Comun.ARGENTINA)));
  }

  private static String numero(Integer valor) {
    return NumberFormat.getIntegerInstance(Comun.ES_AR).format(valor == null ? 0 : valor);
  }

  // --- Donantes --------------------------------------------------------------------------------

  @Override
  public AdminDonantesVista donantes(ConsultaDonantes pedida) {
    Opciones opciones = opciones();
    ConsultaDonantes consulta =
        new ConsultaDonantes(
            pedida.buscar(),
            Opciones.elegido(pedida.tipo(), opciones.tipos()),
            Opciones.elegido(pedida.dona(), opciones.categorias()),
            Opciones.elegido(pedida.estado(), opciones.estados()),
            Opciones.elegido(pedida.orden(), opciones.ordenes()),
            pedida.pagina(),
            Opciones.elegido(pedida.alta(), opciones.altas()));
    LocalDate hoy = LocalDate.now(reloj.withZone(Comun.ARGENTINA));
    Tramo<DonacionesApi.Donante> pagina =
        Paginas.pedir(
            consulta.pagina(),
            POR_PAGINA,
            numero -> donaciones.donantes(filtros(consulta, hoy, numero, POR_PAGINA)));
    Tramo<Donante> donantes =
        new Tramo<>(
            pagina.filas().stream().map(AdminPersonasRest::donante).toList(),
            pagina.total(),
            pagina.pagina(),
            pagina.paginas());
    return new AdminDonantesVista(activos(), ahora(), donantes, consulta, opciones, null);
  }

  /** Cuántos donantes activos hay: el total de una página de uno, sin traer la lista. */
  private long activos() {
    MultiValueMap<String, String> filtros = new LinkedMultiValueMap<>();
    filtros.add(ACTIVO, "true");
    filtros.add("size", "1");
    ResponseEntity<List<DonacionesApi.Donante>> respuesta = donaciones.donantes(filtros);
    return Paginas.total(respuesta, List.of(), 1, 1);
  }

  /**
   * Los filtros que Donaciones resuelve (R11-01): el tipo de persona o el tipo jurídico, el estado
   * (activo o dado de baja), la categoría de lo que donó, el período de alta (R11-34) y el orden
   * por alta o por nombre. "Sin activar su cuenta" no lo guarda Donaciones, y qué dona cada uno no
   * viene en la fila: no se ofrecen.
   */
  private Opciones opciones() {
    List<Opcion> categorias = new ArrayList<>();
    categorias.add(new Opcion("", "Todas las categorías"));
    Collator castellano = Collator.getInstance(Comun.ES_AR);
    donaciones.categorias().stream()
        .filter(c -> c.id() != null && c.nombre() != null)
        .sorted(Comparator.comparing(DonacionesApi.Categoria::nombre, castellano))
        .forEach(c -> categorias.add(new Opcion(c.id().toString(), c.nombre())));
    return new Opciones(
        List.of(
            new Opcion("", "Todos los tipos"),
            new Opcion(PERSONA, "Personas"),
            new Opcion("empresa", "Empresas"),
            new Opcion("ong", "ONG"),
            new Opcion("institucion", "Instituciones"),
            new Opcion("gubernamental", "Gubernamentales")),
        categorias,
        List.of(
            new Opcion("", "Todos los estados"),
            new Opcion(ACTIVO, "Activos"),
            new Opcion("baja", "Dados de baja")),
        List.of(
            new Opcion("", "Más nuevos primero"),
            new Opcion("antiguo", "Más antiguos primero"),
            new Opcion(NOMBRE, "Por nombre, de la A a la Z")),
        false,
        true,
        Opciones.periodos());
  }

  private static MultiValueMap<String, String> filtros(
      ConsultaDonantes consulta, LocalDate hoy, int pagina, int tamanio) {
    MultiValueMap<String, String> filtros = new LinkedMultiValueMap<>();
    if (!consulta.buscar().isEmpty()) {
      filtros.add("q", consulta.buscar());
    }
    switch (consulta.tipo()) {
      case PERSONA -> filtros.add("tipo", "HUMANA");
      case "" -> {
        // Todos los tipos.
      }
      default -> filtros.add("tipoJuridico", consulta.tipo().toUpperCase(Locale.ROOT));
    }
    if (!consulta.estado().isEmpty()) {
      filtros.add(ACTIVO, String.valueOf(ACTIVO.equals(consulta.estado())));
    }
    if (!consulta.dona().isEmpty()) {
      filtros.add("categoriaId", consulta.dona());
    }
    LocalDate[] alta = consulta.periodoDeAlta(hoy);
    if (alta[0] != null) {
      filtros.add("altaDesde", alta[0].toString());
      filtros.add("altaHasta", alta[1].toString());
    }
    filtros.add(
        "orden",
        switch (consulta.orden()) {
          case "antiguo" -> "antiguos";
          case NOMBRE -> NOMBRE;
          default -> "nuevos";
        });
    filtros.add("page", String.valueOf(pagina));
    filtros.add("size", String.valueOf(tamanio));
    return filtros;
  }

  private static Donante donante(DonacionesApi.Donante donante) {
    Persona persona = donante.persona();
    String[] tipo = tipo(persona);
    String estado = Boolean.FALSE.equals(donante.activo()) ? "baja" : ACTIVO;
    return new Donante(
        String.valueOf(donante.idDonante()),
        nombre(persona),
        tipo[0],
        tipo[1],
        documento(persona),
        donante.fechaAlta() == null ? null : Comun.DIA_Y_ANIO.format(donante.fechaAlta()),
        estado,
        null,
        null,
        contacto(persona, donante.mostrarNombre()));
  }

  /**
   * El contacto de la ficha (R11-34): el correo y el teléfono que tenga (el predeterminado
   * primero), la dirección y si pidió que su nombre no se muestre a las organizaciones.
   */
  private static Contacto contacto(Persona persona, Boolean mostrarNombre) {
    List<DonacionesApi.Medio> medios =
        persona == null || persona.mediosDeContacto() == null
            ? List.of()
            : persona.mediosDeContacto().stream()
                .filter(Objects::nonNull)
                .sorted(
                    Comparator.comparing(
                        (DonacionesApi.Medio m) -> !Boolean.TRUE.equals(m.esPredeterminado())))
                .toList();
    String correo =
        medios.stream()
            .filter(m -> "CORREO".equals(m.tipo()))
            .map(DonacionesApi.Medio::direccionCorreo)
            .filter(c -> c != null && !c.isBlank())
            .findFirst()
            .orElse(null);
    String telefono =
        medios.stream()
            .filter(m -> !"CORREO".equals(m.tipo()))
            .map(AdminPersonasRest::telefono)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    return new Contacto(
        correo,
        telefono,
        direccion(persona == null ? null : persona.direccion()),
        Boolean.FALSE.equals(mostrarNombre));
  }

  /** "11 5555-0000", o "11 5555-0000 (WhatsApp)"; null sin número. */
  private static String telefono(DonacionesApi.Medio medio) {
    if (medio.numero() == null || medio.numero().isBlank()) {
      return null;
    }
    String numero =
        medio.codigoArea() == null || medio.codigoArea().isBlank()
            ? medio.numero()
            : medio.codigoArea() + " " + medio.numero();
    return "WHATSAPP".equals(medio.tipo()) ? numero + " (WhatsApp)" : numero;
  }

  /** "Calle Ejemplo 123, CABA"; null sin calle. */
  private static String direccion(DonacionesApi.Direccion direccion) {
    if (direccion == null || direccion.calle() == null || direccion.calle().isBlank()) {
      return null;
    }
    String calle =
        direccion.altura() == null
            ? direccion.calle()
            : direccion.calle() + " " + direccion.altura();
    return direccion.localidad() == null || direccion.localidad().isBlank()
        ? calle
        : calle + ", " + direccion.localidad();
  }

  @Override
  public Donante donante(String clave) {
    UUID id;
    try {
      id = UUID.fromString(clave == null ? "" : clave.strip());
    } catch (IllegalArgumentException noEsUnId) {
      return null;
    }
    try {
      DonacionesApi.Donante encontrado = donaciones.donante(id);
      return encontrado == null ? null : donante(encontrado);
    } catch (HttpClientErrorException.NotFound noExiste) {
      return null;
    }
  }

  @Override
  public SusDonaciones donacionesDe(String clave) {
    UUID id;
    try {
      id = UUID.fromString(clave == null ? "" : clave.strip());
    } catch (IllegalArgumentException noEsUnId) {
      return new SusDonaciones(0, List.of(), true);
    }
    MultiValueMap<String, String> filtros = new LinkedMultiValueMap<>();
    filtros.add("donanteId", id.toString());
    filtros.add("orden", "recientes");
    filtros.add("page", "0");
    filtros.add("size", String.valueOf(RECIENTES));
    ResponseEntity<List<DonacionesApi.Donacion>> respuesta = donaciones.pagina(filtros);
    List<DonacionesApi.Donacion> filas =
        Objects.requireNonNullElse(respuesta.getBody(), List.<DonacionesApi.Donacion>of());
    List<Reciente> recientes =
        filas.stream()
            .filter(d -> d.id() != null)
            .limit(RECIENTES)
            .map(AdminPersonasRest::reciente)
            .toList();
    return new SusDonaciones(Paginas.total(respuesta, filas, 0, RECIENTES), recientes, true);
  }

  private static Reciente reciente(DonacionesApi.Donacion d) {
    String que =
        (d.descripcion() == null ? "Donación" : d.descripcion())
            + " · "
            + Comun.cantidad(d.cantidad(), Comun.categoria(d));
    return new Reciente(
        d.id().toString(),
        que,
        AdminOperacionDonaciones.estado(Comun.normalizado(d.estadoActual())),
        d.fechaRegistro() == null ? "" : Comun.DIA.format(d.fechaRegistro()));
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

  /**
   * El tipo del renglón y su texto: una persona, una empresa u otra persona jurídica, que muestra
   * su tipo jurídico (R11-45: "organización" es quien recibe, no un tipo de donante).
   */
  private static String[] tipo(Persona persona) {
    if (persona == null || !JURIDICA.equals(persona.tipo())) {
      return new String[] {PERSONA, "Persona"};
    }
    return switch (persona.tipoJuridico() == null ? "" : persona.tipoJuridico()) {
      case "EMPRESA" -> new String[] {"empresa", "Empresa"};
      case "ONG" -> new String[] {JURIDICA_NO_EMPRESA, "ONG"};
      case "INSTITUCION" -> new String[] {JURIDICA_NO_EMPRESA, "Institución"};
      case "GUBERNAMENTAL" -> new String[] {JURIDICA_NO_EMPRESA, "Gubernamental"};
      default -> new String[] {JURIDICA_NO_EMPRESA, "Persona jurídica"};
    };
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

  @Override
  public void registrarEntidad(PersonaNueva.Juridica juridica) {
    Persona creada = donaciones.crearPersona(juridica);
    donaciones.registrarEntidad(new DonacionesApi.EntidadNueva(creada.id()));
  }

  // --- Importar donantes -----------------------------------------------------------------------

  @Override
  public UUID importar(MultipartFile archivo) {
    MultiValueMap<String, Object> partes = new LinkedMultiValueMap<>();
    partes.add("archivo", archivo.getResource());
    return donaciones.importarDonantes(partes).id();
  }

  @Override
  public AdminImportacionesVista importaciones(ConsultaImportaciones consulta, String ejemplo) {
    return importaciones.importaciones(consulta, ahora());
  }

  @Override
  public DescargaDeFilas filasConError(UUID archivo) {
    return importaciones.filasConError(archivo);
  }

  /**
   * El nombre y la fecha de subida salen de Donaciones (R11-20); {@code nombre} de la dirección
   * queda como respaldo de las importaciones que no lo guardaron.
   */
  @Override
  public Importacion importacion(UUID id, String respaldo, String ejemplo, int pagina) {
    if (id == null) {
      return null;
    }
    Archivo avance = donaciones.archivo(id);
    String nombre =
        avance.nombre() != null && !avance.nombre().isBlank() ? avance.nombre() : respaldo;
    int procesadas = entero(avance.procesadas());
    int conError = entero(avance.conError());
    String[] etapa = etapa(avance.estado(), procesadas, avance.totalFilas());
    String accion = etapa[0];
    Tramo<FilaConError> filas = null;
    if (IMPORTADO.equals(accion)) {
      int paginas = Tramo.paginas(conError, FILAS_POR_PAGINA);
      int mostrada = Math.clamp(pagina, 1, paginas);
      List<FilaConError> deLaPagina =
          conError == 0
              ? List.of()
              : donaciones.filasConError(id, mostrada - 1, FILAS_POR_PAGINA).stream()
                  .map(f -> new FilaConError("Fila " + numero(f.linea()), queFallo(f.motivo())))
                  .toList();
      filas = new Tramo<>(deLaPagina, conError, mostrada, paginas);
    }
    String ver =
        UriComponentsBuilder.fromPath("/admin-donantes-importado")
            .queryParam("archivo", id)
            .queryParamIfPresent(NOMBRE, Optional.ofNullable(respaldo))
            .encode()
            .toUriString();
    return new Importacion(
        accion,
        etapa[1],
        nombre == null || nombre.isBlank() ? null : nombre,
        avance.totalFilas() == null ? null : numero(avance.totalFilas()),
        numero(procesadas),
        numero(avance.creadas()),
        numero(avance.actualizadas()),
        numero(conError),
        procesadas,
        avance.totalFilas(),
        resultado(procesadas, avance.creadas(), avance.actualizadas(), conError),
        ultimos(avance.ultimos()),
        filas,
        ver,
        HORA.format(LocalDateTime.now(reloj.withZone(Comun.ARGENTINA))),
        avance.subidoEn() == null ? null : AL.format(avance.subidoEn()),
        avance.subidoEn() == null ? null : avance.subidoEn().toString(),
        id.toString());
  }

  /** Los últimos que leyó el archivo, con su nombre y si fue nuevo o actualizado. */
  private static List<Ultimo> ultimos(List<DonacionesApi.Ultimo> ultimos) {
    if (ultimos == null) {
      return List.of();
    }
    return ultimos.stream()
        .filter(u -> u != null && u.nombre() != null && !u.nombre().isBlank())
        .limit(ULTIMOS)
        .map(u -> new Ultimo(u.nombre(), "NUEVO".equals(u.resultado()) ? "Nuevo" : "Actualizado"))
        .toList();
  }

  /**
   * La pantalla y la etapa que le tocan al estado de Donaciones. Un {@code ERROR} con filas leídas
   * es una importación cortada a mitad de camino: esas filas quedaron guardadas.
   */
  private static String[] etapa(String estado, int procesadas, Integer total) {
    return switch (estado == null ? "" : estado) {
      case "PENDIENTE" -> new String[] {IMPORTANDO, "esperando"};
      case "PROCESADO", "PROCESADO_CON_ERRORES" -> new String[] {IMPORTADO, "terminada"};
      case "ERROR" ->
          procesadas > 0
              ? new String[] {IMPORTADO, "cortada"}
              : new String[] {"importar-error", "error"};
      default -> new String[] {IMPORTANDO, total == null ? "leyendo" : IMPORTANDO};
    };
  }

  /** "20.000 filas leídas: 19.100 donantes nuevos, 774 actualizados y 126 filas con error." */
  private static String resultado(
      int procesadas, Integer creadas, Integer actualizadas, int conError) {
    String leidas = numero(procesadas) + " filas leídas: " + numero(creadas) + " donantes nuevos";
    return conError == 0
        ? leidas + " y " + numero(actualizadas) + " actualizados."
        : leidas
            + ", "
            + numero(actualizadas)
            + " actualizados y "
            + numero(conError)
            + " filas con error.";
  }

  private static int entero(Integer valor) {
    return valor == null ? 0 : valor;
  }

  /**
   * Qué pasó con la fila, en palabras (R11-02): Donaciones manda el motivo como código
   * (openapi-donaciones, {@code FilaConError.motivo}). Uno que no conocemos va como el inesperado.
   */
  static String queFallo(String motivo) {
    return MOTIVOS.getOrDefault(motivo == null ? "" : motivo, MOTIVO_INESPERADO);
  }
}
