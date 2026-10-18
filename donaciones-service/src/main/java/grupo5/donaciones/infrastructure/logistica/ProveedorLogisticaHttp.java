package grupo5.donaciones.infrastructure.logistica;

import grupo5.common.logging.FeignTraceRequestInterceptor;
import grupo5.donaciones.dto.comunicaciones.DestinoEventoDTO;
import grupo5.donaciones.dto.logistica.DatosEntregaLogistica;
import grupo5.donaciones.services.logistica.EnvioInciertoException;
import grupo5.donaciones.services.logistica.EnvioRechazadoException;
import grupo5.donaciones.services.logistica.ErrorContratoProveedorException;
import grupo5.donaciones.services.logistica.IProveedorLogistica;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.net.http.HttpConnectTimeoutException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Proveedor de logística que recibe los pedidos por HTTP: traduce el modelo canónico al contrato
 * REST del proveedor y lo envía con {@code POST /api/entregas}. Lo que responde el proveedor se
 * traduce a la misma clasificación que usa el adapter AMQP:
 *
 * <ul>
 *   <li>2xx o 409 (el proveedor ya tiene esa donación): publicado.
 *   <li>Conexión rechazada, host inexistente, timeout de conexión o 503: es seguro que el pedido no
 *       se procesó → {@link EnvioRechazadoException}.
 *   <li>Timeout de lectura, 500, 502, 504 o cualquier otro error del servidor: puede haberse
 *       procesado → {@link EnvioInciertoException}.
 *   <li>Cualquier otro 4xx: el proveedor rechazó el contenido → {@link
 *       ErrorContratoProveedorException}.
 * </ul>
 *
 * Los timeouts y la URL base los define quien arma el {@link RestClient}.
 */
public class ProveedorLogisticaHttp implements IProveedorLogistica {

  static final String PATH_ENTREGAS = "/api/entregas";

  private final String proveedorId;
  private final RestClient restClient;

  public ProveedorLogisticaHttp(String proveedorId, RestClient restClient) {
    this.proveedorId = proveedorId;
    this.restClient = restClient;
  }

  @Override
  public String id() {
    return proveedorId;
  }

  @Override
  public void enviar(UUID envioId, DatosEntregaLogistica datos, String traceId) {
    ResponseEntity<Void> respuesta;
    try {
      respuesta =
          restClient
              .post()
              .uri(PATH_ENTREGAS)
              .contentType(MediaType.APPLICATION_JSON)
              .header(FeignTraceRequestInterceptor.TRACE_HEADER, traceId)
              .body(aPedido(datos))
              .retrieve()
              .toBodilessEntity();
    } catch (RestClientResponseException e) {
      clasificarRespuestaDeError(e);
      return;
    } catch (ResourceAccessException e) {
      throw clasificarFalloDeRed(e);
    } catch (RestClientException e) {
      throw new EnvioInciertoException(proveedorId, "error al enviar el pedido por HTTP", e);
    }
    if (!respuesta.getStatusCode().is2xxSuccessful()) {
      throw new ErrorContratoProveedorException(
          proveedorId, "respuesta inesperada: " + respuesta.getStatusCode());
    }
  }

  /** Un 409 es éxito: el proveedor ya tiene la donación. El resto de los errores se clasifica. */
  private void clasificarRespuestaDeError(RestClientResponseException e) {
    HttpStatusCode estado = e.getStatusCode();
    if (estado.value() == HttpStatus.CONFLICT.value()) {
      return;
    }
    if (estado.value() == HttpStatus.SERVICE_UNAVAILABLE.value()) {
      throw new EnvioRechazadoException(proveedorId, "el proveedor respondió 503", e);
    }
    if (estado.is4xxClientError()) {
      throw new ErrorContratoProveedorException(
          proveedorId, "el proveedor rechazó el pedido con " + estado, e);
    }
    throw new EnvioInciertoException(proveedorId, "el proveedor respondió " + estado, e);
  }

  /**
   * Si la conexión nunca se estableció, el pedido no salió. Cualquier otra falla de red (por
   * ejemplo un timeout de lectura) puede haber ocurrido después de que el proveedor lo recibió.
   */
  private RuntimeException clasificarFalloDeRed(ResourceAccessException e) {
    for (Throwable causa = e; causa != null; causa = causa.getCause()) {
      if (causa instanceof ConnectException
          || causa instanceof UnknownHostException
          || causa instanceof HttpConnectTimeoutException) {
        return new EnvioRechazadoException(proveedorId, "no se pudo conectar con el proveedor", e);
      }
    }
    return new EnvioInciertoException(proveedorId, "sin respuesta del proveedor", e);
  }

  private static PedidoEntregaHttp aPedido(DatosEntregaLogistica datos) {
    return new PedidoEntregaHttp(
        datos.donacionIndependienteId(),
        datos.personaBeneficiariaId(),
        datos.destino(),
        datos.pesoTotalKG(),
        datos.volumenTotalM3());
  }

  /**
   * Contrato REST de {@code POST /api/entregas} de logística; los nombres son los del proveedor.
   */
  record PedidoEntregaHttp(
      UUID idDonacion,
      UUID idBeneficiaria,
      DestinoEventoDTO destino,
      Double pesoTotalKG,
      Double volumenTotalM3) {}
}
