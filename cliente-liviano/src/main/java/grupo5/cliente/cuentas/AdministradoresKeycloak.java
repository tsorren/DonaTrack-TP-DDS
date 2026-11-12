package grupo5.cliente.cuentas;

import grupo5.cliente.puertos.DirectorioDeAdministradores;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Las cuentas de administración en Keycloak (R11-10), por la Admin API con la cuenta de servicio
 * del sitio, la misma de {@link DirectorioKeycloak}. Se reconocen por el atributo {@code
 * administracion}, que solo ve la administración del realm y no viaja en el token. La lista se pide
 * de a una página y el total con {@code /users/count}: nunca entera.
 */
@Component
@Profile("!maqueta")
class AdministradoresKeycloak implements DirectorioDeAdministradores {

  private static final String MARCA = "administracion";
  private static final String SI = "si";
  private static final String CONSULTA = "q=" + MARCA + ":" + SI;
  private static final String USUARIO = "/users/{id}";
  private static final String ROLES = "/users/{id}/role-mappings/realm";
  private static final String ADMIN = "ADMIN";
  private static final String POR_DEFECTO = "default-roles-donatrack";
  private static final String CAMBIAR_CONTRASENA = "UPDATE_PASSWORD";
  private static final ZoneId ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");

  private record Usuario(
      String id,
      String email,
      String firstName,
      String lastName,
      Long createdTimestamp,
      List<String> requiredActions,
      Map<String, List<String>> attributes) {

    boolean esDeAdministracion() {
      return attributes != null && List.of(SI).equals(attributes.get(MARCA));
    }

    boolean entro() {
      return requiredActions == null || !requiredActions.contains(CAMBIAR_CONTRASENA);
    }

    Administrador comoAdministrador() {
      LocalDate creada =
          createdTimestamp == null
              ? null
              : Instant.ofEpochMilli(createdTimestamp).atZone(ARGENTINA).toLocalDate();
      return new Administrador(id, firstName, lastName, email, creada, entro());
    }
  }

  private static final ParameterizedTypeReference<Map<String, Object>> OBJETO =
      new ParameterizedTypeReference<>() {};
  private static final ParameterizedTypeReference<List<Map<String, Object>>> LISTA =
      new ParameterizedTypeReference<>() {};
  private static final ParameterizedTypeReference<List<Usuario>> USUARIOS =
      new ParameterizedTypeReference<>() {};

  private final RestClient keycloak;

  AdministradoresKeycloak(RestClient keycloak) {
    this.keycloak = keycloak;
  }

  @Override
  public long total() {
    Long total = keycloak.get().uri("/users/count?" + CONSULTA).retrieve().body(Long.class);
    return total == null ? 0 : total;
  }

  @Override
  public List<Administrador> pagina(int pagina) {
    int primera = (Math.max(pagina, 1) - 1) * POR_PAGINA;
    List<Usuario> usuarios =
        keycloak
            .get()
            .uri(
                "/users?" + CONSULTA + "&first={first}&max={max}&briefRepresentation=false",
                primera,
                POR_PAGINA)
            .retrieve()
            .body(USUARIOS);
    return usuarios == null
        ? List.of()
        : usuarios.stream().map(Usuario::comoAdministrador).toList();
  }

  @Override
  public Optional<Administrador> cuenta(String id) {
    return usuario(id).filter(Usuario::esDeAdministracion).map(Usuario::comoAdministrador);
  }

  @Override
  public void cambiarNombre(String id, String nombre, String apellido) {
    Map<String, Object> cuenta =
        new HashMap<>(keycloak.get().uri(USUARIO, id).retrieve().body(OBJETO));
    cuenta.put("firstName", nombre);
    cuenta.put("lastName", apellido);
    keycloak.put().uri(USUARIO, id).body(cuenta).retrieve().toBodilessEntity();
  }

  @Override
  public String alta(Alta alta, String contrasena, String altaPor) {
    Map<String, Object> cuenta =
        Map.of(
            "username", alta.correo(),
            "email", alta.correo(),
            "firstName", alta.nombre(),
            "lastName", alta.apellido(),
            "enabled", true,
            "emailVerified", true,
            "attributes", Map.of(MARCA, List.of(SI), "altaPor", List.of(altaPor)),
            "credentials", List.of(provisoria(contrasena)));
    URI creada;
    try {
      creada =
          keycloak
              .post()
              .uri("/users")
              .body(cuenta)
              .retrieve()
              .toBodilessEntity()
              .getHeaders()
              .getLocation();
    } catch (HttpClientErrorException.Conflict repetido) {
      throw new CorreoEnUso();
    }
    String id = idDe(creada);
    try {
      asignarAdmin(id);
      quitarRolPorDefecto(id);
    } catch (RestClientException falla) {
      keycloak.delete().uri(USUARIO, id).retrieve().toBodilessEntity();
      throw new AltaIncompleta(falla);
    }
    return id;
  }

  @Override
  public boolean otraContrasena(String id, String contrasena) {
    boolean pendiente =
        usuario(id).filter(Usuario::esDeAdministracion).filter(u -> !u.entro()).isPresent();
    if (pendiente) {
      keycloak
          .put()
          .uri(USUARIO + "/reset-password", id)
          .body(provisoria(contrasena))
          .retrieve()
          .toBodilessEntity();
    }
    return pendiente;
  }

  private Optional<Usuario> usuario(String id) {
    try {
      return Optional.ofNullable(keycloak.get().uri(USUARIO, id).retrieve().body(Usuario.class));
    } catch (HttpClientErrorException.NotFound noExiste) {
      return Optional.empty();
    }
  }

  private static Map<String, Object> provisoria(String contrasena) {
    return Map.of("type", "password", "value", contrasena, "temporary", true);
  }

  private static String idDe(URI creada) {
    if (creada == null) {
      throw new AltaIncompleta(new IllegalStateException("Keycloak no devolvió la cuenta creada"));
    }
    String ruta = creada.getPath();
    return ruta.substring(ruta.lastIndexOf('/') + 1);
  }

  /** El rol se lee de los disponibles para la cuenta, que pide view-users y no view-realm. */
  private void asignarAdmin(String id) {
    List<Map<String, Object>> disponibles =
        keycloak.get().uri(ROLES + "/available", id).retrieve().body(LISTA);
    Map<String, Object> admin =
        disponibles == null
            ? null
            : disponibles.stream()
                .filter(r -> ADMIN.equals(r.get("name")))
                .findFirst()
                .orElse(null);
    if (admin == null) {
      throw new RestClientException("El rol ADMIN no está disponible para la cuenta");
    }
    keycloak.post().uri(ROLES, id).body(List.of(admin)).retrieve().toBodilessEntity();
  }

  /** La Admin API le da el rol por defecto del realm (que trae DONANTE): se lo saca. */
  private void quitarRolPorDefecto(String id) {
    List<Map<String, Object>> asignados = keycloak.get().uri(ROLES, id).retrieve().body(LISTA);
    List<Map<String, Object>> porDefecto =
        asignados == null
            ? List.of()
            : asignados.stream().filter(r -> POR_DEFECTO.equals(r.get("name"))).toList();
    if (!porDefecto.isEmpty()) {
      keycloak
          .method(HttpMethod.DELETE)
          .uri(ROLES, id)
          .body(porDefecto)
          .retrieve()
          .toBodilessEntity();
    }
  }
}
