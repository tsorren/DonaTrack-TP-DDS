package grupo5.cliente.rest;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.PatchExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * Lo que el cliente usa de Logística (docs/arquitectura/contratos/openapi-logistica.yaml): las
 * entregas, su seguimiento y sus fotos, el mapa público y los camiones. Sin entrega, Logística
 * responde 404. Los records copian los campos de cada DTO; el resto del JSON se ignora. La declara
 * entera el esqueleto de la conexión de pantallas (SPEC-08): los frentes la usan y no la cambian.
 * Con ingreso, la llamada lleva el token del usuario (lo pone {@code ClientesHttp}, SPEC-09, F5);
 * sin ingreso va sin cabecera.
 */
interface LogisticaApi {

  // --- Entregas --------------------------------------------------------------------------------

  @GetExchange("/api/entregas/seguimiento/donaciones/{idDonacion}")
  Seguimiento seguimiento(@PathVariable UUID idDonacion);

  /** Las entregas de una entidad, también las ya entregadas (las filtra quien llama). */
  @GetExchange("/api/entregas/seguimiento/beneficiarios/{idBeneficiaria}")
  List<Seguimiento> seguimientoDeEntidad(@PathVariable UUID idBeneficiaria);

  /** Las entregas activas de un donante (HB-16); las creadas antes de HB-16 no tienen donante. */
  @GetExchange("/api/entregas/seguimiento/donantes/{donanteId}")
  List<Seguimiento> seguimientoDeDonante(@PathVariable UUID donanteId);

  @GetExchange("/api/entregas/{id}")
  Entrega entrega(@PathVariable UUID id);

  /** Las entregadas, con coordenadas exactas y sin datos de la donación (D24). */
  @GetExchange("/api/entregas/mapa")
  List<EntregaEnElMapa> mapa();

  /**
   * Confirmar la recepción (ENTREGADA, con fotos, remito y reporte) o decir que no llegó
   * (NO_RECIBIDA, con justificación).
   */
  @PatchExchange("/api/entregas/{id}/estado")
  Entrega cambiarEstado(@PathVariable UUID id, @RequestBody CambioDeEntrega cambio);

  /** Sube las fotos del acta: cada archivo en una parte {@code foto}; devuelve sus URL. */
  @PostExchange(
      value = "/api/entregas/{id}/fotos",
      contentType = MediaType.MULTIPART_FORM_DATA_VALUE)
  List<FotoSubida> subirFotos(@PathVariable UUID id, @RequestBody MultiValueMap<String, ?> partes);

  // --- Camiones --------------------------------------------------------------------------------

  /**
   * Los camiones. Hoy, sin los deshabilitados; con {@code incluirDeshabilitados=true}, todos
   * (HB-23, contrato fijado en SPEC-08; mientras no esté en ENTREGA_6, el parámetro se ignora).
   */
  @GetExchange("/api/camiones")
  List<Camion> camiones(@RequestParam(required = false) Boolean incluirDeshabilitados);

  @PostExchange("/api/camiones")
  Camion agregarCamion(@RequestBody CamionNuevo camion);

  /** {@code estado}: DISPONIBLE o DESHABILITADO, con su motivo. */
  @PatchExchange("/api/camiones/{id}/estado")
  Camion cambiarEstadoDeCamion(@PathVariable UUID id, @RequestBody CambioDeCamion cambio);

  // --- Records ---------------------------------------------------------------------------------

  record Seguimiento(
      UUID idEntrega,
      UUID idDonacion,
      String estadoEntrega,
      Boolean rutaAsignada,
      UUID idRuta,
      LocalDate fechaRuta,
      String estadoRuta,
      String patenteCamion,
      Integer ordenVisita,
      Integer totalParadas,
      Integer paradasEntregadas,
      Integer paradasProcesadas,
      Instant horaSalida,
      Instant horaArribo,
      Instant ultimaActualizacion,
      List<Parada> paradas,
      Posicion posicionCamion) {}

  record Parada(
      Integer ordenVisita, String estado, Boolean esEntregaConsultada, Instant horaArribo) {}

  record Posicion(Double latitud, Double longitud, Instant registradaEn) {}

  record Entrega(
      UUID id,
      UUID idRuta,
      UUID idDonacion,
      UUID idBeneficiaria,
      Destino destino,
      String estadoActual,
      Instant horaSalida,
      Instant horaArribo,
      String fotoRecepcionUrl,
      List<String> fotosRecepcion,
      Float pesoTotalKG,
      Float volumenTotalM3,
      List<CambioDeEstado> historialEstado,
      String numeroRemito,
      Reporte reporte) {}

  record Destino(
      String calle,
      Integer altura,
      Integer piso,
      String departamento,
      String codigoPostal,
      String localidad,
      String provincia,
      String pais,
      Double latitud,
      Double longitud) {}

  record CambioDeEstado(String estadoAnterior, String estadoNuevo, Instant timestamp) {}

  /** {@code motivo}: el del reporte del contrato (cantidad incompleta, dañada u otro). */
  record Reporte(String motivo, String detalle, Double cantidadRecibida) {}

  record EntregaEnElMapa(
      UUID idEntrega,
      Double latitud,
      Double longitud,
      String localidad,
      String provincia,
      String pais,
      Instant fechaEntrega,
      Float pesoTotalKG,
      Float volumenTotalM3) {}

  /** El cuerpo de {@code PATCH .../estado}; {@code estado} con el nombre del enum del contrato. */
  record CambioDeEntrega(
      String estado,
      String actor,
      String justificacion,
      Boolean replanificable,
      List<String> fotosRecepcion,
      String fotoRecepcionUrl,
      String numeroRemito,
      Reporte reporte) {}

  record FotoSubida(UUID fotoId, String url) {}

  record Camion(
      UUID id,
      String patente,
      Float capacidadVolumen,
      Float altura,
      Float capacidadKG,
      String estado,
      UUID rutaId) {}

  record CamionNuevo(String patente, Float capacidadVolumen, Float altura, Float capacidadKG) {}

  record CambioDeCamion(String estado, String motivo) {}
}
