package grupo5.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;

// Contrato de runtime del realm donatrack (SPEC-06 A1-02a, ADR 20261104).
//
// Construye la imagen con el Dockerfile de auth-service, la levanta con Testcontainers, crea un
// donante por la Admin API, hace el login por authorization code contra el cliente versionado
// (cliente-liviano) y lee los claims de los tokens que emite Keycloak.
//
// Corre solo con el perfil contrato-auth (necesita Docker):
//   mvn -f auth-service/pom.xml -Pcontrato-auth verify
//
// Usa start-dev con la base en memoria (KC_DB=dev-file) para no depender de un Postgres: verifica
// el realm y los tokens, no el cableado con Postgres, que cubre `docker compose up`.
class RealmTokenContractIT {

  private static final String REALM = "donatrack";
  private static final String CLIENT_ID = "cliente-liviano";
  private static final String API_AUDIENCE = "donatrack-api";
  private static final String BASE_URL_CLIENTE = "http://localhost:8090";
  private static final String REDIRECT_URI = BASE_URL_CLIENTE + "/login/oauth2/code/keycloak";
  private static final String ESTADO = "estado-de-prueba";
  private static final String EMAIL = "donante.contrato@example.com";
  private static final List<String> CLAIMS_CON_PII =
      List.of("email", "email_verified", "name", "given_name", "family_name", "preferred_username");
  private static final Pattern ACCION_DE_LOGIN =
      Pattern.compile("action=\"([^\"]*login-actions/authenticate[^\"]*)\"");
  private static final Pattern MENSAJE_DE_KEYCLOAK =
      Pattern.compile("(?s)kc-(?:error-message|feedback-text)[^>]*>\\s*(?:<p[^>]*>)?\\s*([^<]+)");

  // Se generan en cada corrida: no hay contraseñas literales en el repo.
  private static final String ADMIN_USER = "kc-admin-" + UUID.randomUUID();
  private static final String ADMIN_PASSWORD = "adm-" + UUID.randomUUID();
  private static final String CLIENT_SECRET = "sec-" + UUID.randomUUID();
  private static final String USER_PASSWORD = "usr-" + UUID.randomUUID();

  private static final ObjectMapper JSON = new ObjectMapper();
  private static final HttpClient HTTP = HttpClient.newHttpClient();

  private static GenericContainer<?> keycloak;
  private static String baseUrl;
  private static String userId;
  private static JsonNode accessToken;
  private static JsonNode idToken;

  @BeforeAll
  static void arrancarKeycloakYPedirUnToken() throws Exception {
    Path auth = Path.of(System.getProperty("basedir", ".")).toAbsolutePath().normalize();
    ImageFromDockerfile imagen =
        new ImageFromDockerfile("donatrack/keycloak-contrato", false)
            .withFileFromPath("Dockerfile", auth.resolve("Dockerfile"))
            .withFileFromPath("realm", auth.resolve("realm"))
            .withFileFromPath("themes", auth.resolve("themes"));

    keycloak =
        new GenericContainer<>(imagen)
            .withEnv("KC_DB", "dev-file")
            .withEnv("KC_HOSTNAME_STRICT", "false")
            .withEnv("KC_BOOTSTRAP_ADMIN_USERNAME", ADMIN_USER)
            .withEnv("KC_BOOTSTRAP_ADMIN_PASSWORD", ADMIN_PASSWORD)
            .withEnv("CLIENTE_LIVIANO_SECRET", CLIENT_SECRET)
            .withEnv("CLIENTE_LIVIANO_BASE_URL", BASE_URL_CLIENTE)
            .withCommand("start-dev", "--import-realm")
            .withExposedPorts(8080)
            .waitingFor(
                Wait.forHttp("/realms/" + REALM)
                    .forPort(8080)
                    .forStatusCode(200)
                    .withStartupTimeout(Duration.ofMinutes(4)));
    keycloak.start();
    baseUrl = "http://" + keycloak.getHost() + ":" + keycloak.getMappedPort(8080);

    userId = crearDonante(tokenDeAdmin());
    JsonNode respuesta = iniciarSesionYCanjearElCodigo();
    accessToken = claims(respuesta.get("access_token").asText());
    idToken = claims(respuesta.get("id_token").asText());
  }

  @AfterAll
  static void detenerKeycloak() {
    if (keycloak != null) {
      keycloak.stop();
    }
  }

  @Test
  void elAccessTokenLlevaElSubDelUsuario() {
    assertEquals(userId, accessToken.get("sub").asText());
  }

  @Test
  void elAccessTokenLlevaSoloElRolDeNegocioEnElClaimRoles() {
    assertEquals(List.of("DONANTE"), valores(accessToken.get("roles")));
  }

  @Test
  void elIdTokenTambienLlevaElClaimRoles() {
    assertEquals(List.of("DONANTE"), valores(idToken.get("roles")));
  }

  @Test
  void elAccessTokenTieneLaAudienciaDelApi() {
    assertEquals(List.of(API_AUDIENCE), valores(accessToken.get("aud")));
  }

  @Test
  void elAccessTokenVenceALos900Segundos() {
    assertEquals(900L, accessToken.get("exp").asLong() - accessToken.get("iat").asLong());
  }

  @Test
  void elAccessTokenNoLlevaDatosPersonales() {
    for (String claim : CLAIMS_CON_PII) {
      assertFalse(accessToken.has(claim), "el access token no debe llevar " + claim);
    }
  }

  @Test
  void elIdTokenNoLlevaDatosPersonales() {
    for (String claim : CLAIMS_CON_PII) {
      assertFalse(idToken.has(claim), "el ID token no debe llevar " + claim);
    }
  }

  @Test
  void elRealmFijaLaVidaDeLaSesionYDelAccessToken() throws Exception {
    JsonNode realm = admin("/admin/realms/" + REALM);
    assertEquals(900, realm.get("accessTokenLifespan").asInt());
    assertEquals(28800, realm.get("ssoSessionIdleTimeout").asInt());
    assertTrue(realm.get("registrationEmailAsUsername").asBoolean());
  }

  @Test
  void elClienteEsConfidencialYSoloUsaAuthorizationCode() throws Exception {
    JsonNode cliente = admin("/admin/realms/" + REALM + "/clients?clientId=" + CLIENT_ID).get(0);
    assertFalse(cliente.get("publicClient").asBoolean());
    assertTrue(cliente.get("standardFlowEnabled").asBoolean());
    assertFalse(cliente.get("directAccessGrantsEnabled").asBoolean());
    assertFalse(cliente.get("implicitFlowEnabled").asBoolean());
    assertFalse(cliente.get("serviceAccountsEnabled").asBoolean());
  }

  @Test
  void elSecretoYLasUrlsSalenDeLasVariablesDeEntorno() throws Exception {
    JsonNode cliente = admin("/admin/realms/" + REALM + "/clients?clientId=" + CLIENT_ID).get(0);
    String id = cliente.get("id").asText();
    JsonNode secreto = admin("/admin/realms/" + REALM + "/clients/" + id + "/client-secret");
    assertEquals(CLIENT_SECRET, secreto.get("value").asText());
    assertEquals(List.of(REDIRECT_URI), valores(cliente.get("redirectUris")));
    JsonNode atributos = cliente.get("attributes");
    assertEquals(BASE_URL_CLIENTE + "/", atributos.get("post.logout.redirect.uris").asText());
  }

  @Test
  void elPasswordGrantEstaRechazado() throws Exception {
    Map<String, String> campos = new LinkedHashMap<>();
    campos.put("grant_type", "password");
    campos.put("client_id", CLIENT_ID);
    campos.put("client_secret", CLIENT_SECRET);
    campos.put("username", EMAIL);
    campos.put("password", USER_PASSWORD);
    HttpResponse<String> respuesta = post(HTTP, urlDeToken(REALM), form(campos));
    assertTrue(respuesta.statusCode() >= 400 && respuesta.statusCode() < 500, respuesta.body());
    assertTrue(respuesta.body().contains("unauthorized_client"), respuesta.body());
  }

  // ---- Cliente HTTP mínimo: sin librerías nuevas ----

  private static String urlDeToken(String realm) {
    return baseUrl + "/realms/" + realm + "/protocol/openid-connect/token";
  }

  private static String form(Map<String, String> campos) {
    List<String> pares = new ArrayList<>();
    for (Map.Entry<String, String> campo : campos.entrySet()) {
      String valor = URLEncoder.encode(campo.getValue(), StandardCharsets.UTF_8);
      pares.add(campo.getKey() + "=" + valor);
    }
    return String.join("&", pares);
  }

  private static HttpResponse<String> post(HttpClient cliente, String url, String cuerpoForm)
      throws IOException, InterruptedException {
    return post(cliente, url, cuerpoForm, "");
  }

  private static HttpResponse<String> post(
      HttpClient cliente, String url, String cuerpoForm, String galletas)
      throws IOException, InterruptedException {
    HttpRequest.Builder peticion =
        HttpRequest.newBuilder(URI.create(url))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(cuerpoForm));
    if (!galletas.isEmpty()) {
      peticion.header("Cookie", galletas);
    }
    return cliente.send(peticion.build(), HttpResponse.BodyHandlers.ofString());
  }

  private static HttpResponse<String> postJson(String url, String token, String cuerpo)
      throws IOException, InterruptedException {
    HttpRequest peticion =
        HttpRequest.newBuilder(URI.create(url))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + token)
            .POST(HttpRequest.BodyPublishers.ofString(cuerpo))
            .build();
    return HTTP.send(peticion, HttpResponse.BodyHandlers.ofString());
  }

  private static JsonNode admin(String ruta) throws IOException, InterruptedException {
    HttpRequest peticion =
        HttpRequest.newBuilder(URI.create(baseUrl + ruta))
            .header("Authorization", "Bearer " + tokenDeAdmin())
            .GET()
            .build();
    HttpResponse<String> respuesta = HTTP.send(peticion, HttpResponse.BodyHandlers.ofString());
    assertEquals(200, respuesta.statusCode(), respuesta.body());
    return JSON.readTree(respuesta.body());
  }

  // ---- Pasos del escenario ----

  private static String tokenDeAdmin() throws IOException, InterruptedException {
    Map<String, String> campos = new LinkedHashMap<>();
    campos.put("grant_type", "password");
    campos.put("client_id", "admin-cli");
    campos.put("username", ADMIN_USER);
    campos.put("password", ADMIN_PASSWORD);
    HttpResponse<String> respuesta = post(HTTP, urlDeToken("master"), form(campos));
    assertEquals(200, respuesta.statusCode(), respuesta.body());
    return JSON.readTree(respuesta.body()).get("access_token").asText();
  }

  // Con nombre y email cargados, para que la ausencia de esos claims en el token sea significativa.
  private static String crearDonante(String tokenAdmin) throws IOException, InterruptedException {
    Map<String, Object> credencial = new LinkedHashMap<>();
    credencial.put("type", "password");
    credencial.put("value", USER_PASSWORD);
    credencial.put("temporary", false);
    Map<String, Object> usuario = new LinkedHashMap<>();
    usuario.put("username", EMAIL);
    usuario.put("email", EMAIL);
    usuario.put("firstName", "Dona");
    usuario.put("lastName", "Tante");
    usuario.put("enabled", true);
    usuario.put("emailVerified", true);
    usuario.put("credentials", List.of(credencial));

    String urlUsuarios = baseUrl + "/admin/realms/" + REALM + "/users";
    HttpResponse<String> creado =
        postJson(urlUsuarios, tokenAdmin, JSON.writeValueAsString(usuario));
    assertEquals(201, creado.statusCode(), creado.body());
    String ubicacion = creado.headers().firstValue("Location").orElseThrow();
    String id = ubicacion.substring(ubicacion.lastIndexOf('/') + 1);

    HttpRequest pedirRol =
        HttpRequest.newBuilder(URI.create(baseUrl + "/admin/realms/" + REALM + "/roles/DONANTE"))
            .header("Authorization", "Bearer " + tokenAdmin)
            .GET()
            .build();
    HttpResponse<String> rol = HTTP.send(pedirRol, HttpResponse.BodyHandlers.ofString());
    assertEquals(200, rol.statusCode(), rol.body());
    String urlRoles = urlUsuarios + "/" + id + "/role-mappings/realm";
    HttpResponse<String> asignado = postJson(urlRoles, tokenAdmin, "[" + rol.body() + "]");
    assertEquals(204, asignado.statusCode(), asignado.body());
    return id;
  }

  // Lo que hace el navegador: pantalla de login, envío del formulario y redirect con el código.
  private static JsonNode iniciarSesionYCanjearElCodigo() throws IOException, InterruptedException {
    HttpClient navegador =
        HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();

    Map<String, String> autorizacion = new LinkedHashMap<>();
    autorizacion.put("client_id", CLIENT_ID);
    autorizacion.put("redirect_uri", REDIRECT_URI);
    autorizacion.put("response_type", "code");
    autorizacion.put("scope", "openid");
    autorizacion.put("state", ESTADO);
    String urlAuth =
        baseUrl + "/realms/" + REALM + "/protocol/openid-connect/auth?" + form(autorizacion);
    HttpResponse<String> pantalla =
        navegador.send(
            HttpRequest.newBuilder(URI.create(urlAuth)).GET().build(),
            HttpResponse.BodyHandlers.ofString());
    assertEquals(200, pantalla.statusCode(), pantalla.body());

    Matcher accion = ACCION_DE_LOGIN.matcher(pantalla.body());
    assertTrue(accion.find(), "no se encontró el formulario de login en la pantalla de Keycloak");
    String urlLogin = accion.group(1).replace("&amp;", "&");

    Map<String, String> credenciales = new LinkedHashMap<>();
    credenciales.put("username", EMAIL);
    credenciales.put("password", USER_PASSWORD);
    credenciales.put("credentialId", "");
    HttpResponse<String> envio = post(navegador, urlLogin, form(credenciales), galletas(pantalla));
    String detalle =
        "Keycloak no redirigió tras el login: status="
            + envio.statusCode()
            + ", Set-Cookie recibidas="
            + galletasRecibidas(pantalla)
            + ", mensaje="
            + mensajeDeKeycloak(envio.body());
    assertEquals(302, envio.statusCode(), detalle);

    URI destino = URI.create(envio.headers().firstValue("Location").orElseThrow());
    assertTrue(destino.toString().startsWith(REDIRECT_URI), destino.toString());
    Map<String, String> consulta = consulta(destino.getRawQuery());
    assertEquals(ESTADO, consulta.get("state"));
    String codigo = consulta.get("code");
    assertNotNull(codigo, "Keycloak no devolvió el código: " + destino);

    Map<String, String> canje = new LinkedHashMap<>();
    canje.put("grant_type", "authorization_code");
    canje.put("code", codigo);
    canje.put("redirect_uri", REDIRECT_URI);
    canje.put("client_id", CLIENT_ID);
    canje.put("client_secret", CLIENT_SECRET);
    HttpResponse<String> tokens = post(HTTP, urlDeToken(REALM), form(canje));
    assertEquals(200, tokens.statusCode(), tokens.body());
    return JSON.readTree(tokens.body());
  }

  // ---- Cookies a mano: lo que hace el navegador, sin las reglas de Secure ni de SameSite ----

  private static String galletas(HttpResponse<?> respuesta) {
    Map<String, String> guardadas = new LinkedHashMap<>();
    for (String setCookie : respuesta.headers().allValues("Set-Cookie")) {
      String par = setCookie.split(";", 2)[0];
      int igual = par.indexOf('=');
      boolean borrada = setCookie.toLowerCase(Locale.ROOT).contains("max-age=0");
      if (igual > 0 && igual < par.length() - 1 && !borrada) {
        guardadas.put(par.substring(0, igual).trim(), par.substring(igual + 1));
      }
    }
    List<String> pares = new ArrayList<>();
    for (Map.Entry<String, String> galleta : guardadas.entrySet()) {
      pares.add(galleta.getKey() + "=" + galleta.getValue());
    }
    return String.join("; ", pares);
  }

  // Para diagnosticar: las cabeceras Set-Cookie sin sus valores (quedan nombre y atributos).
  private static List<String> galletasRecibidas(HttpResponse<?> respuesta) {
    List<String> recibidas = new ArrayList<>();
    for (String setCookie : respuesta.headers().allValues("Set-Cookie")) {
      recibidas.add(setCookie.replaceFirst("=[^;]*", "=<valor>"));
    }
    return recibidas;
  }

  private static String mensajeDeKeycloak(String html) {
    Matcher mensaje = MENSAJE_DE_KEYCLOAK.matcher(html);
    return mensaje.find() ? mensaje.group(1).trim() : "(sin mensaje en la página)";
  }

  // ---- Lectura de resultados ----

  private static Map<String, String> consulta(String consultaCruda) {
    Map<String, String> parametros = new LinkedHashMap<>();
    for (String par : consultaCruda.split("&")) {
      int igual = par.indexOf('=');
      String clave = igual < 0 ? par : par.substring(0, igual);
      String valor = igual < 0 ? "" : par.substring(igual + 1);
      parametros.put(
          URLDecoder.decode(clave, StandardCharsets.UTF_8),
          URLDecoder.decode(valor, StandardCharsets.UTF_8));
    }
    return parametros;
  }

  private static JsonNode claims(String jwt) throws IOException {
    String carga = jwt.split("\\.")[1];
    return JSON.readTree(Base64.getUrlDecoder().decode(carga));
  }

  // Un claim puede venir como texto (un solo valor) o como arreglo.
  private static List<String> valores(JsonNode nodo) {
    assertNotNull(nodo, "falta el claim");
    List<String> valores = new ArrayList<>();
    if (nodo.isArray()) {
      nodo.forEach(elemento -> valores.add(elemento.asText()));
    } else {
      valores.add(nodo.asText());
    }
    return valores;
  }
}
