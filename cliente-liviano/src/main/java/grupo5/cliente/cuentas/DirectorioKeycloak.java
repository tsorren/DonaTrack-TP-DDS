package grupo5.cliente.cuentas;

import grupo5.cliente.puertos.DirectorioDeCuentas;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
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

  private static final String SOLICITUDES =
      "/users?q=solicitudEntidad:pendiente&briefRepresentation=false";
  private static final String ROLES_DEL_REALM = "/users/{id}/role-mappings/realm";
  private static final String ROLES_DE_LA_API = "/users/{id}/role-mappings/clients/" + API;

  private record Usuario(String id, String email, Map<String, List<String>> attributes) {}

  private static final ParameterizedTypeReference<Map<String, Object>> OBJETO =
      new ParameterizedTypeReference<>() {};
  private static final ParameterizedTypeReference<List<Map<String, Object>>> LISTA =
      new ParameterizedTypeReference<>() {};
  private static final ParameterizedTypeReference<List<Usuario>> USUARIOS =
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
    cambiarAtributos(
        cuentaId,
        propios -> atributos.forEach((nombre, valor) -> propios.put(nombre, List.of(valor))));
    plantilla.ifPresent(rol -> asignarPlantilla(cuentaId, rol));
  }

  @Override
  public List<SolicitudDeEntidad> solicitudesDeEntidad() {
    return keycloak.get().uri(SOLICITUDES).retrieve().body(USUARIOS).stream()
        .map(u -> new SolicitudDeEntidad(u.id(), personaDe(u), u.email()))
        .toList();
  }

  @Override
  public void asignarRol(String cuentaId, String rol) {
    asignar(ROLES_DEL_REALM, cuentaId, rol);
  }

  @Override
  public void asignarPlantilla(String cuentaId, String plantilla) {
    asignar(ROLES_DE_LA_API, cuentaId, plantilla);
  }

  @Override
  public void quitarAtributo(String cuentaId, String atributo) {
    cambiarAtributos(cuentaId, propios -> propios.remove(atributo));
  }

  private static UUID personaDe(Usuario usuario) {
    return UUID.fromString(usuario.attributes().get("personaId").get(0));
  }

  private Map<String, Object> cuenta(String cuentaId) {
    return new HashMap<>(keycloak.get().uri("/users/{id}", cuentaId).retrieve().body(OBJETO));
  }

  /** Reescribe la cuenta con sus atributos cambiados, sin perder los que ya tenía. */
  private void cambiarAtributos(String cuentaId, Consumer<Map<String, Object>> cambio) {
    Map<String, Object> cuenta = cuenta(cuentaId);
    Map<String, Object> propios = new HashMap<>();
    if (cuenta.get("attributes") instanceof Map<?, ?> actuales) {
      actuales.forEach((nombre, valor) -> propios.put((String) nombre, valor));
    }
    cambio.accept(propios);
    cuenta.put("attributes", propios);
    keycloak.put().uri("/users/{id}", cuentaId).body(cuenta).retrieve().toBodilessEntity();
  }

  /** Los roles se leen de los disponibles para la cuenta, que pide view-users y no view-clients. */
  private void asignar(String ruta, String cuentaId, String rol) {
    List<Map<String, Object>> disponibles =
        keycloak.get().uri(ruta + "/available", cuentaId).retrieve().body(LISTA);
    Map<String, Object> elegido =
        disponibles.stream().filter(r -> rol.equals(r.get("name"))).findFirst().orElseThrow();
    keycloak.post().uri(ruta, cuentaId).body(List.of(elegido)).retrieve().toBodilessEntity();
  }
}
