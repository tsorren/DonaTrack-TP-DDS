package grupo5.cliente.cuentas;

import grupo5.cliente.puertos.DirectorioDeCuentas;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Las cuentas en Keycloak, por la Admin API con la cuenta de servicio del cliente liviano. */
@Component
@Profile("!maqueta")
class DirectorioKeycloak implements DirectorioDeCuentas {

  /** El id fijo de {@code donatrack-api} en el realm (la cuenta de servicio no lista clientes). */
  private static final String API = "7f3c2a9e-5b1d-4e8a-9c6f-2d4b8a1e0c53";

  private static final ParameterizedTypeReference<Map<String, Object>> OBJETO =
      new ParameterizedTypeReference<>() {};
  private static final ParameterizedTypeReference<List<Map<String, Object>>> LISTA =
      new ParameterizedTypeReference<>() {};

  private final RestClient keycloak;

  DirectorioKeycloak(RestClient keycloak) {
    this.keycloak = keycloak;
  }

  @Override
  public String correoDe(String cuentaId) {
    return (String) cuenta(cuentaId).get("email");
  }

  @Override
  public void vincular(String cuentaId, Map<String, String> atributos, Optional<String> plantilla) {
    Map<String, Object> cuenta = cuenta(cuentaId);
    Map<String, Object> propios = new HashMap<>();
    if (cuenta.get("attributes") instanceof Map<?, ?> actuales) {
      actuales.forEach((nombre, valor) -> propios.put((String) nombre, valor));
    }
    atributos.forEach((nombre, valor) -> propios.put(nombre, List.of(valor)));
    cuenta.put("attributes", propios);
    keycloak.put().uri("/users/{id}", cuentaId).body(cuenta).retrieve().toBodilessEntity();
    plantilla.ifPresent(rol -> asignar(cuentaId, rol));
  }

  private Map<String, Object> cuenta(String cuentaId) {
    return new HashMap<>(keycloak.get().uri("/users/{id}", cuentaId).retrieve().body(OBJETO));
  }

  /** Los roles se leen de los disponibles para la cuenta, que pide view-users y no view-clients. */
  private void asignar(String cuentaId, String rol) {
    String ruta = "/users/{id}/role-mappings/clients/{cliente}";
    List<Map<String, Object>> disponibles =
        keycloak.get().uri(ruta + "/available", cuentaId, API).retrieve().body(LISTA);
    Map<String, Object> elegido =
        disponibles.stream().filter(r -> rol.equals(r.get("name"))).findFirst().orElseThrow();
    keycloak.post().uri(ruta, cuentaId, API).body(List.of(elegido)).retrieve().toBodilessEntity();
  }
}
