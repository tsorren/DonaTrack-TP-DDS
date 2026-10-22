package grupo5.donaciones.infrastructure.logistica;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sun.net.httpserver.HttpServer;
import grupo5.donaciones.config.LogisticaProveedoresConfig;
import grupo5.donaciones.dto.comunicaciones.DestinoEventoDTO;
import grupo5.donaciones.dto.logistica.DatosEntregaLogistica;
import grupo5.donaciones.services.logistica.EnvioInciertoException;
import grupo5.donaciones.services.logistica.EnvioRechazadoException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Prueba el adapter HTTP con sockets reales (servidor en loopback) para confirmar que los timeouts
 * del cliente real se clasifican bien; {@code MockRestServiceServer} no pasa por esa capa.
 */
class ProveedorLogisticaHttpRedRealTest {

  private static final DatosEntregaLogistica DATOS =
      new DatosEntregaLogistica(
          UUID.randomUUID(),
          UUID.randomUUID(),
          new DestinoEventoDTO(
              "Av. Siempreviva", 742, null, null, "1000", "CABA", "Buenos Aires", "Argentina"),
          12.5,
          0.3,
          LocalDateTime.of(2026, Month.OCTOBER, 7, 12, 0));

  private final CountDownLatch liberarRespuesta = new CountDownLatch(1);
  private HttpServer servidor;

  @AfterEach
  void detenerServidor() {
    liberarRespuesta.countDown();
    if (servidor != null) {
      servidor.stop(0);
    }
  }

  private String levantarServidor(long demoraMs, int estado) throws IOException {
    servidor = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
    servidor.createContext(
        "/api/entregas",
        exchange -> {
          try {
            // simula un proveedor lento: retiene la respuesta hasta la demora o el fin del test
            if (liberarRespuesta.await(demoraMs, TimeUnit.MILLISECONDS)) {
              return; // el test terminó: el cliente ya no espera respuesta
            }
            exchange.sendResponseHeaders(estado, -1);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          } catch (IOException e) {
            // el cliente ya cortó por timeout: no hay a quién responder
          } finally {
            exchange.close();
          }
        });
    servidor.start();
    return "http://"
        + InetAddress.getLoopbackAddress().getHostAddress()
        + ":"
        + servidor.getAddress().getPort();
  }

  private ProveedorLogisticaHttp proveedor(String url, long readTimeoutMs) {
    return new ProveedorLogisticaHttp(
        "externo", LogisticaProveedoresConfig.restClient(url, 1000, readTimeoutMs));
  }

  @Test
  void respuestaRapidaEsExito() throws IOException {
    String url = levantarServidor(0, 201);

    assertDoesNotThrow(() -> proveedor(url, 2000).enviar(UUID.randomUUID(), DATOS, "trace"));
  }

  @Test
  void timeoutDeLecturaRealEsIncierto() throws IOException {
    ProveedorLogisticaHttp proveedor = proveedor(levantarServidor(1500, 201), 200);
    UUID envioId = UUID.randomUUID();

    assertThrows(EnvioInciertoException.class, () -> proveedor.enviar(envioId, DATOS, "trace"));
  }

  @Test
  void puertoCerradoRealEsRechazado() throws IOException {
    int puertoLibre;
    try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
      puertoLibre = socket.getLocalPort();
    }
    String url = "http://" + InetAddress.getLoopbackAddress().getHostAddress() + ":" + puertoLibre;
    ProveedorLogisticaHttp proveedor = proveedor(url, 200);
    UUID envioId = UUID.randomUUID();

    assertThrows(EnvioRechazadoException.class, () -> proveedor.enviar(envioId, DATOS, "trace"));
  }
}
