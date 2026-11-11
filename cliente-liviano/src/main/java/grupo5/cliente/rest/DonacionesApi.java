package grupo5.cliente.rest;

import grupo5.cliente.puertos.PersonaNueva;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.PatchExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;

/**
 * Lo que el cliente usa de Donaciones (docs/arquitectura/contratos/openapi-donaciones.yaml). Los
 * records copian los campos de cada DTO; el resto del JSON se ignora. La interfaz entera la declara
 * el esqueleto de la conexión de pantallas (SPEC-08): los frentes la usan y no la cambian. Quién
 * puede llamar cada endpoint lo dice su {@code @PreAuthorize}: las llamadas llevan el token de
 * quien tiene la sesión ({@link ClientesHttp}).
 *
 * <p>Los filtros van como {@code MultiValueMap}: solo viajan las claves que se cargan (por ejemplo
 * {@code estado}, {@code categoriaId}, {@code donanteId}, {@code entidadId}, {@code desde}, {@code
 * hasta} y {@code necesidadId} en las donaciones independientes; {@code nombre}, {@code page} y
 * {@code size} en las entidades; {@code entidadId}, {@code tipo} y {@code estado}, repetible, en
 * las necesidades).
 */
interface DonacionesApi {

  // --- Donaciones independientes ---------------------------------------------------------------

  @GetExchange("/donaciones-independientes")
  List<Donacion> donaciones(@RequestParam UUID donanteId);

  /** Las asignadas a una entidad beneficiaria. */
  @GetExchange("/donaciones-independientes")
  List<Donacion> asignadas(@RequestParam UUID entidadId);

  /** Con cualquier filtro; sin {@code donanteId} ni {@code entidadId} la puede pedir solo ADMIN. */
  @GetExchange("/donaciones-independientes")
  List<Donacion> buscar(@RequestParam MultiValueMap<String, String> filtros);

  /** El donante (dueño) o ADMIN. La entidad no puede leer una por id: usa el listado. */
  @GetExchange("/donaciones-independientes/{id}")
  Donacion donacionIndependiente(@PathVariable UUID id);

  /** Solo ADMIN; {@code actor} queda en el historial. */
  @PatchExchange("/donaciones-independientes/{id}/estado")
  Donacion cambiarEstado(
      @PathVariable UUID id, @RequestHeader("X-Actor") String actor, @RequestBody Cambiar cambio);

  // --- Donaciones originales -------------------------------------------------------------------

  /** Solo ADMIN. */
  @GetExchange("/api/donaciones")
  List<Original> originales();

  /** La donación original, para saber quién donó: el donante (dueño) o ADMIN. */
  @GetExchange("/api/donaciones/{id}")
  Original donacion(@PathVariable UUID id);

  /** El donante (con REGISTRAR_DONACIONES, sobre sí mismo) o ADMIN. */
  @PostExchange("/api/donaciones")
  Original registrarDonacion(@RequestBody DonacionNueva donacion);

  /** Público. */
  @GetExchange("/api/donaciones/contadores")
  Contadores contadores();

  /** Público; {@code periodo} y {@code limite}, opcionales. */
  @GetExchange("/api/donaciones/destacadas")
  List<Destacada> destacadas(@RequestParam MultiValueMap<String, String> filtros);

  /** La dirección firmada para subir la foto de un bien: el donante o ADMIN. */
  @PostExchange("/api/donaciones/fotos")
  SubidaDeFoto pedirSubidaDeFoto(@RequestBody PedidoDeSubida pedido);

  // --- Categorías ------------------------------------------------------------------------------

  /** Con sesión. */
  @GetExchange("/api/categorias")
  List<Categoria> categorias();

  /** Con sesión. */
  @GetExchange("/api/subcategorias")
  List<Subcategoria> subcategorias();

  // --- Entidades beneficiarias -----------------------------------------------------------------

  /** La entidad (dueña) o ADMIN. */
  @GetExchange("/api/entidades/{id}")
  Entidad entidad(@PathVariable UUID id);

  /** Solo ADMIN; el total viene en la cabecera {@code X-Total-Count}. */
  @GetExchange("/api/entidades")
  ResponseEntity<List<Entidad>> entidades(@RequestParam MultiValueMap<String, String> filtros);

  /** Público, solo las activas; el total viene en la cabecera {@code X-Total-Count}. */
  @GetExchange("/api/entidades/publicas")
  ResponseEntity<List<EntidadPublica>> entidadesPublicas(
      @RequestParam MultiValueMap<String, String> filtros);

  /** Solo ADMIN. */
  @PostExchange("/api/entidades")
  Entidad registrarEntidad(@RequestBody EntidadNueva entidad);

  // --- Necesidades -----------------------------------------------------------------------------

  /** La entidad (con VER_NECESIDADES, de su {@code entidadId}) o ADMIN. */
  @GetExchange("/api/necesidades")
  List<Necesidad> necesidades(@RequestParam MultiValueMap<String, String> filtros);

  /** Público: las vigentes, sin las vencidas. */
  @GetExchange("/api/necesidades/publicas")
  List<NecesidadPublica> necesidadesPublicas(@RequestParam(required = false) UUID entidadId);

  @GetExchange("/api/necesidades/{id}")
  Necesidad necesidad(@PathVariable UUID id);

  /** Lo pedido, lo asignado y lo recibido de cada período de una recurrente. */
  @GetExchange("/api/necesidades/{id}/periodos")
  List<Periodo> periodos(@PathVariable UUID id);

  /** La entidad, con GESTIONAR_NECESIDADES. */
  @PostExchange("/api/necesidades")
  Necesidad publicarNecesidad(@RequestBody Necesidad necesidad);

  /** Con la {@code version} leída: otra responde 409. */
  @PutExchange("/api/necesidades/{id}")
  Necesidad cambiarNecesidad(@PathVariable UUID id, @RequestBody Necesidad necesidad);

  @DeleteExchange("/api/necesidades/{id}")
  void darDeBajaNecesidad(@PathVariable UUID id);

  // --- Personas, donantes y legales ------------------------------------------------------------

  /** ADMIN o el token técnico; {@code tipo}: HUMANA o JURIDICA. */
  @GetExchange("/api/personas")
  List<Persona> personas(@RequestParam(required = false) String tipo);

  /** ADMIN o el token técnico. */
  @PostExchange("/api/personas")
  Persona crearPersona(@RequestBody PersonaNueva persona);

  /** La persona (con EDITAR_DATOS_ORGANIZACION, sobre sí misma) o ADMIN. */
  @PutExchange("/api/personas/{id}")
  Persona cambiarPersona(@PathVariable UUID id, @RequestBody PersonaNueva persona);

  /** La persona (donante o entidad), el token técnico o ADMIN. */
  @GetExchange("/api/personas/{id}/legales")
  Legales legales(@PathVariable UUID id);

  @PutExchange("/api/personas/{id}/legales")
  Legales aceptarLegales(@PathVariable UUID id, @RequestBody AceptarLegales aceptar);

  /** Solo ADMIN; {@code canal}, opcional. */
  @GetExchange("/api/donantes")
  List<Donante> donantes(@RequestParam(required = false) String canal);

  /** El donante (el id es el de su persona) o ADMIN. */
  @GetExchange("/api/donantes/{id}")
  Donante donante(@PathVariable UUID id);

  /** ADMIN o el token técnico. */
  @PostExchange("/api/donantes")
  Donante registrarDonante(@RequestBody DonanteNuevo donante);

  /** El donante, sobre sí mismo, o ADMIN. */
  @PatchExchange("/api/donantes/{id}/preferencias")
  Donante cambiarPreferencias(@PathVariable UUID id, @RequestBody Preferencias preferencias);

  /** Solo ADMIN: el CSV va en la parte {@code archivo}. */
  @PostExchange(value = "/api/donantes/archivos", contentType = MediaType.MULTIPART_FORM_DATA_VALUE)
  Archivo importarDonantes(@RequestBody MultiValueMap<String, ?> partes);

  @GetExchange("/api/donantes/archivos/{id}")
  Archivo archivo(@PathVariable UUID id);

  @GetExchange("/api/donantes/archivos/{id}/filas-con-error")
  List<FilaConError> filasConError(
      @PathVariable UUID id,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer size);

  // --- Asignaciones (solo ADMIN) ---------------------------------------------------------------

  @PostExchange("/api/asignaciones/ejecuciones")
  List<Propuesta> correrAsignacion();

  @GetExchange("/api/asignaciones/ejecuciones")
  List<Ejecucion> ejecuciones();

  @GetExchange("/api/asignaciones/propuestas")
  List<Propuesta> propuestas();

  /** {@code estado}: APROBADA o RECHAZADA. */
  @PutExchange("/api/asignaciones/propuestas/{id}/estado")
  void decidirPropuesta(@PathVariable UUID id, @RequestBody Decision decision);

  // --- Records de las respuestas ---------------------------------------------------------------

  /**
   * Una donación independiente. {@code estadoActual} y {@code estadoNuevo} llegan como "EnDeposito"
   * (ver el mapper). {@code quienDono} trae el nombre visible, o null si el donante lo ocultó.
   */
  record Donacion(
      UUID id,
      String descripcion,
      String estadoActual,
      LocalDateTime fechaRegistro,
      List<Cambio> historial,
      List<Item> items,
      int cantidad,
      UUID entidadBeneficiariaId,
      UUID donacionOriginalId,
      UUID necesidadId,
      Boolean cancelacionPedida,
      QuienDono quienDono) {}

  record Cambio(
      String estadoNuevo,
      LocalDateTime timestamp,
      String justificacion,
      String estadoAnterior,
      String actor) {}

  record Item(BienNormalizado bien, Integer cantidad) {}

  record BienNormalizado(Categoria categoria, Bien bien, Subcategoria subcategoria) {}

  record Bien(String descripcion, String fotoUrl, LocalDate fechaVencimiento, String estado) {}

  /** {@code tipo}: HUMANA o JURIDICA; {@code nombreVisible}, null si el donante lo ocultó. */
  record QuienDono(String tipo, String nombreVisible, UUID donanteId) {}

  /** El cuerpo de {@code PATCH .../estado}: {@code estado} con el nombre del enum del contrato. */
  record Cambiar(
      String estado,
      String justificacion,
      UUID necesidadId,
      String urlMapa,
      String patenteCamion,
      Boolean replanificable) {}

  record Categoria(
      UUID id,
      String nombre,
      String unidad,
      Boolean conUso,
      Boolean conVencimiento,
      List<Subcategoria> subcategorias) {}

  record Subcategoria(UUID id, String nombre, Categoria categoria, String unidad) {}

  record Original(
      UUID id,
      Donante donante,
      List<ItemOriginal> items,
      String descripcion,
      LocalDateTime fecha,
      Direccion deposito,
      String estadoActual,
      List<Cambio> historialEstados) {}

  record ItemOriginal(
      String descripcionBien,
      String fotoUrl,
      LocalDate fechaVencimiento,
      String estadoBien,
      Double pesoUnitario,
      Double volumenUnitario,
      Integer cantidad) {}

  /** {@code idDonante} es el id de su persona (en Donaciones, {@code Donante.id == personaId}). */
  record Donante(
      UUID idDonante, UUID personaId, Persona persona, Boolean activo, Boolean mostrarNombre) {}

  /**
   * El cuerpo del alta: {@code idDonante} es su persona; {@code descripcion} e ítems, obligatorios.
   */
  record DonacionNueva(
      UUID idDonante,
      String descripcion,
      List<ItemNuevo> items,
      String nombreDeposito,
      Direccion direccion,
      LocalDateTime fecha,
      Boolean mostrarNombre) {}

  /** {@code descripcionBien} y {@code cantidad} (mayor que cero), obligatorios. */
  record ItemNuevo(
      String descripcionBien,
      String fotoUrl,
      LocalDate fechaVencimiento,
      String estadoBien,
      Double pesoUnitario,
      Double volumenUnitario,
      Integer cantidad) {}

  record Contadores(
      Long donantesActivos,
      Long donacionesRealizadas,
      Long beneficiariosActivos,
      OffsetDateTime calculadoEn) {}

  record Destacada(
      UUID id,
      String categoria,
      String subcategoria,
      Integer cantidad,
      String unidad,
      UUID entidadId,
      String entidad,
      LocalDateTime fechaEntrega,
      List<String> fotos) {}

  record PedidoDeSubida(String tipoContenido) {}

  record SubidaDeFoto(
      String uploadUrl, String metodo, String tipoContenido, String referencia, Instant expiraEn) {}

  record Entidad(UUID id, Persona juridica, Boolean activo) {}

  record EntidadPublica(UUID id, String razonSocial, String rubro) {}

  record EntidadNueva(UUID juridicaId) {}

  /**
   * Una necesidad: sirve de respuesta y de cuerpo. {@code estado}, {@code periodicidad} y {@code
   * unidad} con el nombre del enum del contrato; {@code version}, la que se leyó, para el 409.
   */
  record Necesidad(
      UUID id,
      String tipo,
      UUID idEntidad,
      UUID idSubcategoria,
      Integer cantidadNecesitada,
      String descripcion,
      Boolean estaSatisfecha,
      LocalDate fechaInicio,
      LocalDate fechaFin,
      String estado,
      Long version,
      LocalDate fechaLimite,
      String periodicidad,
      String unidad,
      UltimoCambio ultimoCambio) {}

  record UltimoCambio(Integer cantidadAnterior, LocalDate fecha) {}

  record NecesidadPublica(
      UUID id,
      UUID idEntidad,
      UUID idSubcategoria,
      String descripcion,
      Integer cantidadNecesitada,
      String tipo,
      LocalDate fechaFin) {}

  record Periodo(
      UUID id,
      LocalDate inicio,
      LocalDate fin,
      Integer objetivo,
      Integer asignado,
      Integer recibido,
      String resultado) {}

  /**
   * Una persona humana trae {@code nombre} y {@code apellido}; una jurídica, {@code razonSocial},
   * {@code tipoJuridico}, {@code rubro} y sus {@code representantes}.
   */
  record Persona(
      String tipo,
      UUID id,
      String tipoDocumento,
      String documento,
      Direccion direccion,
      List<Medio> mediosDeContacto,
      String nombre,
      String apellido,
      String razonSocial,
      String tipoJuridico,
      String rubro,
      List<Persona> representantes) {}

  record Direccion(
      String calle,
      Integer altura,
      Integer piso,
      String departamento,
      String codigoPostal,
      String localidad,
      String provincia,
      String pais) {}

  /** {@code tipo}: CORREO (con {@code direccionCorreo}), TELEFONO o WHATSAPP (con el número). */
  record Medio(
      String tipo,
      Boolean esPredeterminado,
      String direccionCorreo,
      String caracteristica,
      String codigoArea,
      String numero) {}

  record Legales(
      Integer versionAceptada,
      LocalDateTime aceptadaEn,
      Integer versionVigente,
      Boolean vigenteAceptada) {}

  record AceptarLegales(int version) {}

  record DonanteNuevo(UUID idPersona) {}

  record Preferencias(boolean mostrarNombre) {}

  /** El avance de un CSV: {@code estado} y los contadores de filas. */
  record Archivo(
      UUID id,
      String path,
      String estado,
      Integer totalFilas,
      Integer procesadas,
      Integer creadas,
      Integer actualizadas,
      Integer conError) {}

  record FilaConError(Integer linea, String motivo, String codigo) {}

  record Propuesta(
      UUID id,
      String estado,
      LocalDateTime fechaCreacion,
      NecesidadResumen necesidad,
      List<Fragmento> fragmentaciones) {}

  record NecesidadResumen(
      UUID id, String descripcion, Integer cantidadNecesitada, String tipo, String estado) {}

  record Fragmento(Donacion donacionIndependiente, Integer cantidadNecesaria) {}

  record Ejecucion(UUID id, LocalDateTime fechaEjecucion, Integer cantidadPropuestasGeneradas) {}

  record Decision(String estado, String justificacion) {}
}
