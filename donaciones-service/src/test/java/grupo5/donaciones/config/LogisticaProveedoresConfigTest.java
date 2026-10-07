package grupo5.donaciones.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import grupo5.donaciones.infrastructure.logistica.ProveedorLogisticaAmqp;
import grupo5.donaciones.infrastructure.logistica.ProveedoresLogistica;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.mock.env.MockEnvironment;

class LogisticaProveedoresConfigTest {

  private final RabbitMQConfig rabbitConfig = new RabbitMQConfig();

  private ProveedoresLogistica armar(List<String> proveedores, MockEnvironment env) {
    return new LogisticaProveedoresConfig()
        .proveedoresLogistica(
            proveedores,
            5000,
            env,
            mock(ConnectionFactory.class),
            rabbitConfig.messageConverter(rabbitConfig.classMapper()));
  }

  @Test
  void deberiaCrearUnAdapterAmqpPorCadaProveedorConTransporteAmqp() {
    MockEnvironment env =
        new MockEnvironment()
            .withProperty("donatrack.logistica.proveedor.donatrack.transporte", "amqp")
            .withProperty("donatrack.logistica.proveedor.otra.transporte", " AMQP ");

    ProveedoresLogistica proveedores = armar(List.of("donatrack", "otra"), env);

    assertEquals(2, proveedores.cantidad());
    ProveedorLogisticaAmqp otra =
        assertInstanceOf(ProveedorLogisticaAmqp.class, proveedores.buscar("otra").orElseThrow());
    assertEquals("entrega.solicitada.otra.v1", otra.routingKey());
  }

  @Test
  void unProveedorSinTransporteSoportadoNoDeberiaTenerAdapter() {
    MockEnvironment env =
        new MockEnvironment()
            .withProperty("donatrack.logistica.proveedor.donatrack.transporte", "amqp")
            .withProperty("donatrack.logistica.proveedor.externo.transporte", "http");

    ProveedoresLogistica proveedores = armar(List.of("donatrack", "externo", "sin-config"), env);

    assertEquals(1, proveedores.cantidad());
    assertTrue(proveedores.buscar("donatrack").isPresent());
    assertTrue(proveedores.buscar("externo").isEmpty());
    assertTrue(proveedores.buscar("sin-config").isEmpty());
  }
}
