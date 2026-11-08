package grupo5.cliente.rest;

import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * Lo que el cliente usa de Logística (docs/arquitectura/contratos/openapi-logistica.yaml): la
 * entrega de una donación y sus fotos de recepción. Sin entrega, Logística responde 404.
 */
@HttpExchange("/api/entregas")
interface LogisticaApi {

  @GetExchange("/seguimiento/donaciones/{idDonacion}")
  Seguimiento seguimiento(@PathVariable UUID idDonacion);

  @GetExchange("/{id}")
  Entrega entrega(@PathVariable UUID id);

  record Seguimiento(UUID idEntrega) {}

  record Entrega(String fotoRecepcionUrl, List<String> fotosRecepcion) {}
}
