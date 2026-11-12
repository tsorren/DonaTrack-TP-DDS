package grupo5.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import java.util.Set;
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
  private static final String BLOQUEO_EMAIL = "bloqueo.contrato@example.com";
  private static final String POLITICA_EMAIL = "politica.contrato@example.com";
  private static final String REGISTRO_EMAIL = "registro.contrato@example.com";
  private static final String ADMIN_DONATRACK_EMAIL = "admin.contrato@example.com";
  private static final String MIEMBRO_EMAIL = "miembro.contrato@example.com";
  private static final String CUENTA_EMAIL = "cuenta.contrato@example.com";
  private static final String ALTA_POR_SERVICIO_EMAIL = "alta.servicio.contrato@example.com";
  // Id fijo en el realm: la cuenta de servicio no tiene view-clients para buscarlo.
  private static final String API_CLIENT_UUID = "7f3c2a9e-5b1d-4e8a-9c6f-2d4b8a1e0c53";
  private static final String PERSONA_ID = UUID.randomUUID().toString();
  private static final String OTRA_PERSONA_ID = UUID.randomUUID().toString();
  private static final String GOOGLE_CLIENT_ID = "google-de-prueba-" + UUID.randomUUID();
  // OPERADOR_ENTIDAD (matriz de permisos, §3) más VER_RUTAS suelto, que la plantilla no trae.
  private static final Set<String> PERMISOS_DEL_OPERADOR_CON_RUTAS =
      Set.of(
          "CONFIRMAR_RECEPCION",
          "GESTIONAR_NECESIDADES",
          "VER_DONACIONES",
          "VER_ENTREGAS",
          "VER_NECESIDADES",
          "VER_NOTIFICACIONES",
          "VER_RUTAS");
  private static final String SIN_MENSAJE = "(sin mensaje en la página)";
  private static final List<String> CLAIMS_CON_PII =
      List.of("email", "email_verified", "name", "given_name", "family_name", "preferred_username");
  private static final Pattern ACCION_DE_LOGIN =
      Pattern.compile("action=\"([^\"]*login-actions/authenticate[^\"]*)\"");
  private static final Pattern CSS_DEL_TEMA =
      Pattern.compile("href=\"([^\"]*/login/donatrack/css/donatrack\\.css)\"");
  private static final Pattern ACCION_DE_REGISTRO =
      Pattern.compile("action=\"([^\"]*login-actions/registration[^\"]*)\"");
  private static final Pattern MENSAJE_DE_KEYCLOAK =
      Pattern.compile("(?s)kc-(?:error-message|feedback-text)[^>]*>\\s*(?:<p[^>]*>)?\\s*([^<]+)");

  // Se generan en cada corrida: no hay contraseñas literales en el repo.
  private static final String ADMIN_USER = "kc-admin-" + UUID.randomUUID();
  private static final String ADMIN_PASSWORD = "adm-" + UUID.randomUUID();
  private static final String CLIENT_SECRET = "sec-" + UUID.randomUUID();
  private static final String PRUEBAS_CLIENT_ID = "donatrack-pruebas";
  private static final String PRUEBAS_SECRET = "pru-" + UUID.randomUUID();
  private static final String USER_PASSWORD = "usr-" + UUID.randomUUID();
  private static final String CLAVE_INCORRECTA = "mal-" + UUID.randomUUID();
  private static final String ADMIN_DONATRACK_CLAVE = "adm-app-" + UUID.randomUUID();
  private static final String GOOGLE_CLIENT_SECRET = "ggl-" + UUID.randomUUID();

  private static final ObjectMapper JSON = new ObjectMapper();
  private static final HttpClient HTTP = HttpClient.newHttpClient();

  private static GenericContainer<?> keycloak;
  private static String baseUrl;
  private static String userId;
  private static JsonNode accessToken;
  private static JsonNode idToken;
  private static String refreshToken;

  @BeforeAll
  static void arrancarKeycloakYPedirUnToken() throws Exception {
    Path auth = Path.of(System.getProperty("basedir", ".")).toAbsolutePath().normalize();
    ImageFromDockerfile imagen =
        new ImageFromDockerfile("donatrack/keycloak-contrato", false)
            .withFileFromPath("Dockerfile", auth.resolve("Dockerfile"))
            .withFileFromPath("realm", auth.resolve("realm"))
            .withFileFromPath("themes", auth.resolve("themes"))
            .withFileFromPath("spi", auth.resolve("spi"));

    keycloak =
        new GenericContainer<>(imagen)
            .withEnv("KC_DB", "dev-file")
            .withEnv("KC_HOSTNAME_STRICT", "false")
            .withEnv("KC_BOOTSTRAP_ADMIN_USERNAME", ADMIN_USER)
            .withEnv("KC_BOOTSTRAP_ADMIN_PASSWORD", ADMIN_PASSWORD)
            .withEnv("CLIENTE_LIVIANO_SECRET", CLIENT_SECRET)
            .withEnv("CLIENTE_LIVIANO_BASE_URL", BASE_URL_CLIENTE)
            .withEnv("PRUEBAS_CLIENT_SECRET", PRUEBAS_SECRET)
            .withEnv("AUTH_ADMIN_EMAIL", ADMIN_DONATRACK_EMAIL)
            .withEnv("AUTH_ADMIN_PASSWORD", ADMIN_DONATRACK_CLAVE)
            .withEnv("GOOGLE_CLIENT_ID", GOOGLE_CLIENT_ID)
            .withEnv("GOOGLE_CLIENT_SECRET", GOOGLE_CLIENT_SECRET)
            .withEnv("GOOGLE_HABILITADO", "true")
            .withCommand("start-dev", "--import-realm")
            .withExposedPorts(8080)
            .waitingFor(
                Wait.forHttp("/realms/" + REALM)
                    .forPort(8080)
                    .forStatusCode(200)
                    .withStartupTimeout(Duration.ofMinutes(4)));
    keycloak.start();
    baseUrl = "http://" + keycloak.getHost() + ":" + keycloak.getMappedPort(8080);

    userId = crearUsuario(tokenDeAdmin(), EMAIL, USER_PASSWORD);
    JsonNode respuesta = iniciarSesionYCanjearElCodigo(EMAIL, USER_PASSWORD);
    refreshToken = respuesta.get("refresh_token").asText();
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
    // La cuenta de servicio da el token técnico SERVICIO (client credentials).
    assertTrue(cliente.get("serviceAccountsEnabled").asBoolean());
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

  @Test
  void elPerfilDeUsuarioNoExigeNombreNiApellido() throws Exception {
    JsonNode perfil = admin("/admin/realms/" + REALM + "/users/profile");
    assertFalse(esObligatorio(atributo(perfil, "firstName")), "el nombre no debe ser obligatorio");
    assertFalse(esObligatorio(atributo(perfil, "lastName")), "el apellido no debe ser obligatorio");
    JsonNode requeridoDelEmail = atributo(perfil, "email").get("required");
    assertEquals(List.of("user"), valores(requeridoDelEmail.get("roles")));
  }

  @Test
  void laPoliticaDeClaveRespetaLosLimitesDeLongitudYRechazaElEmail() throws Exception {
    String token = tokenDeAdmin();
    String id = crearUsuario(token, POLITICA_EMAIL, null);
    assertEquals(400, cambiarClave(token, id, "x".repeat(9)).statusCode(), "9 caracteres");
    assertEquals(400, cambiarClave(token, id, "x".repeat(129)).statusCode(), "129 caracteres");
    assertEquals(400, cambiarClave(token, id, POLITICA_EMAIL).statusCode(), "igual al email");
    assertEquals(204, cambiarClave(token, id, "y".repeat(10)).statusCode(), "10 caracteres");
    assertEquals(204, cambiarClave(token, id, "z".repeat(128)).statusCode(), "128 caracteres");
  }

  @Test
  void elRealmFijaLaFuerzaBrutaLaPoliticaDeClaveYLaRotacionDelRefresh() throws Exception {
    JsonNode realm = admin("/admin/realms/" + REALM);
    assertTrue(realm.get("bruteForceProtected").asBoolean());
    assertFalse(realm.get("permanentLockout").asBoolean());
    assertEquals(5, realm.get("failureFactor").asInt());
    assertEquals(900, realm.get("maxDeltaTimeSeconds").asInt());
    assertEquals(900, realm.get("waitIncrementSeconds").asInt());
    assertEquals(900, realm.get("maxFailureWaitSeconds").asInt());
    assertTrue(realm.get("revokeRefreshToken").asBoolean());
    assertEquals(0, realm.get("refreshTokenMaxReuse").asInt());
    String politica = realm.get("passwordPolicy").asText();
    assertTrue(politica.contains("length(10)"), politica);
    assertTrue(politica.contains("maxLength(128)"), politica);
    assertTrue(politica.contains("notEmail"), politica);
  }

  @Test
  void despuesDeCincoFallosLaClaveCorrectaTambienFallaConElMismoMensaje() throws Exception {
    String id = crearUsuario(tokenDeAdmin(), BLOQUEO_EMAIL, USER_PASSWORD);
    String mensajeDeClaveMala = "";
    for (int intento = 1; intento <= 5; intento++) {
      HttpResponse<String> fallido = enviarLogin(BLOQUEO_EMAIL, CLAVE_INCORRECTA).envio();
      assertEquals(200, fallido.statusCode(), "intento fallido " + intento);
      mensajeDeClaveMala = mensajeDeKeycloak(fallido.body());
    }
    assertNotEquals(SIN_MENSAJE, mensajeDeClaveMala, "la página de error no trae mensaje");
    esperarBloqueo(id);

    HttpResponse<String> sexto = enviarLogin(BLOQUEO_EMAIL, USER_PASSWORD).envio();
    assertEquals(200, sexto.statusCode(), "el sexto intento no debería iniciar sesión");
    assertEquals(mensajeDeClaveMala, mensajeDeKeycloak(sexto.body()));
  }

  @Test
  void elRefreshRotaYReusarUnoViejoCortaLaSesion() throws Exception {
    JsonNode login = iniciarSesionYCanjearElCodigo(EMAIL, USER_PASSWORD);
    String primero = login.get("refresh_token").asText();

    HttpResponse<String> rotado = refrescar(primero);
    assertEquals(200, rotado.statusCode(), rotado.body());
    String segundo = JSON.readTree(rotado.body()).get("refresh_token").asText();
    assertNotEquals(primero, segundo);

    HttpResponse<String> reuso = refrescar(primero);
    assertEquals(400, reuso.statusCode(), reuso.body());
    assertTrue(reuso.body().contains("invalid_grant"), reuso.body());

    HttpResponse<String> sesionCortada = refrescar(segundo);
    assertEquals(400, sesionCortada.statusCode(), sesionCortada.body());
    assertTrue(sesionCortada.body().contains("invalid_grant"), sesionCortada.body());
  }

  @Test
  void elRefreshTokenVenceALas8HorasDeInactividad() throws IOException {
    JsonNode refresh = claims(refreshToken);
    assertEquals(28800L, refresh.get("exp").asLong() - refresh.get("iat").asLong());
  }

  @Test
  void elAdminInicialTieneSoloElRolAdmin() throws Exception {
    String id = idDelUsuario(ADMIN_DONATRACK_EMAIL);
    String base = "/admin/realms/" + REALM + "/users/" + id;
    assertEquals(List.of("ADMIN"), nombresDeRoles(admin(base + "/role-mappings/realm")));
    assertEquals(List.of("ADMIN"), nombresDeRoles(admin(base + "/role-mappings/realm/composite")));
  }

  @Test
  void laClaveDelAdminSalioDeLaVariableYNoDelLiteral() throws Exception {
    JsonNode login = iniciarSesionYCanjearElCodigo(ADMIN_DONATRACK_EMAIL, ADMIN_DONATRACK_CLAVE);
    JsonNode access = claims(login.get("access_token").asText());
    assertEquals(List.of("ADMIN"), valores(access.get("roles")));

    String literalDeLaVariable = "${AUTH_ADMIN_PASSWORD}";
    HttpResponse<String> literal = enviarLogin(ADMIN_DONATRACK_EMAIL, literalDeLaVariable).envio();
    assertEquals(200, literal.statusCode(), "el literal no debe ser la clave del admin");
  }

  @Test
  void unUsuarioAutorregistradoQuedaConElRolDonanteYSinNombreNiApellido() throws Exception {
    JsonNode registro = registrarseYCanjearElCodigo(REGISTRO_EMAIL, USER_PASSWORD);
    JsonNode access = claims(registro.get("access_token").asText());
    assertEquals(List.of("DONANTE"), valores(access.get("roles")));

    String id = idDelUsuario(REGISTRO_EMAIL);
    assertEquals(id, access.get("sub").asText());
    String ruta = "/admin/realms/" + REALM + "/users/" + id + "/role-mappings/realm/composite";
    List<String> efectivos = nombresDeRoles(admin(ruta));
    assertTrue(efectivos.contains("DONANTE"), efectivos.toString());
    assertFalse(efectivos.contains("ADMIN"), efectivos.toString());
    assertFalse(efectivos.contains("ENTIDAD"), efectivos.toString());
  }

  @Test
  void laPantallaDeLoginUsaElTemaDonatrackEnEspanol() throws Exception {
    verificarTema(abrirPantalla(HTTP, null).body());
  }

  @Test
  void laPantallaDeRegistroUsaElMismoTema() throws Exception {
    verificarTema(abrirPantalla(HTTP, "create").body());
  }

  @Test
  void laPantallaDeLoginOfreceEntrarConGoogle() throws Exception {
    assertTrue(abrirPantalla(HTTP, null).body().contains("/broker/google/login"));
  }

  @Test
  void googleEsProveedorDeIdentidadConLasCredencialesDeLasVariables() throws Exception {
    JsonNode google = admin("/admin/realms/" + REALM + "/identity-provider/instances/google");
    assertEquals("google", google.get("providerId").asText());
    assertTrue(google.get("enabled").asBoolean());
    assertEquals(GOOGLE_CLIENT_ID, google.get("config").get("clientId").asText());
    // D4: el flujo propio pide la clave para vincular Google con una cuenta que ya existe.
    assertEquals("donatrack first broker login", google.get("firstBrokerLoginFlowAlias").asText());
  }

  @Test
  void elFlujoDeVinculacionPideLaClaveYNoOfreceElCaminoPorEmail() throws Exception {
    String alias =
        URLEncoder.encode("donatrack first broker login", StandardCharsets.UTF_8)
            .replace("+", "%20");
    JsonNode ejecuciones =
        admin("/admin/realms/" + REALM + "/authentication/flows/" + alias + "/executions");
    List<String> autenticadores = new ArrayList<>();
    for (JsonNode ejecucion : ejecuciones) {
      if (ejecucion.hasNonNull("providerId")) {
        autenticadores.add(ejecucion.get("providerId").asText());
      }
    }

    assertTrue(autenticadores.contains("idp-confirm-link"), autenticadores.toString());
    assertTrue(autenticadores.contains("idp-username-password-form"), autenticadores.toString());
    // El camino por email de Keycloak mandaría el correo por SMTP, que acá no hay.
    assertFalse(autenticadores.contains("idp-email-verification"), autenticadores.toString());
  }

  // Keycloak crea default-roles-donatrack con offline_access antes de leer el JSON, y el import
  // solo suma composites: el rol queda. Lo que se garantiza es que el cliente no pueda pedirlo.
  @Test
  void elClienteNoPuedePedirTokensOffline() throws Exception {
    JsonNode cliente = admin("/admin/realms/" + REALM + "/clients?clientId=" + CLIENT_ID).get(0);
    String base = "/admin/realms/" + REALM + "/clients/" + cliente.get("id").asText();
    for (String tipo : List.of("/default-client-scopes", "/optional-client-scopes")) {
      List<String> scopes = nombresDeRoles(admin(base + tipo));
      assertFalse(scopes.contains("offline_access"), tipo + " " + scopes);
    }
  }

  @Test
  void unDonanteSinPlantillaNiPersonaNoLlevaEsosClaims() {
    assertFalse(accessToken.has("permisos"));
    assertFalse(accessToken.has("personaId"));
  }

  // RX-05 y RX-06: los permisos salen planos de la plantilla más los sueltos, y personaId sale del
  // atributo de la cuenta. El nombre de la plantilla no viaja: no es un permiso.
  @Test
  void losTokensLlevanLosPermisosDeLaPlantillaMasLosSueltosYElPersonaId() throws Exception {
    String admin = tokenDeAdmin();
    String id = crearUsuario(admin, MIEMBRO_EMAIL, USER_PASSWORD, "ENTIDAD");
    asignarRolesDeLaApi(admin, id, List.of("OPERADOR_ENTIDAD", "VER_RUTAS"));
    assertEquals(204, editarAtributo(admin, id, "personaId", PERSONA_ID).statusCode());

    JsonNode login = iniciarSesionYCanjearElCodigo(MIEMBRO_EMAIL, USER_PASSWORD);
    for (String tipo : List.of("access_token", "id_token")) {
      JsonNode token = claims(login.get(tipo).asText());
      Set<String> permisos = Set.copyOf(valores(token.get("permisos")));
      assertEquals(PERMISOS_DEL_OPERADOR_CON_RUTAS, permisos, tipo);
      assertEquals(PERSONA_ID, token.get("personaId").asText(), tipo);
      // Además trae DONANTE: la Admin API le da el rol por defecto a toda cuenta que crea.
      assertTrue(valores(token.get("roles")).contains("ENTIDAD"), tipo);
    }
    JsonNode access = claims(login.get("access_token").asText());
    assertEquals(List.of(API_AUDIENCE), valores(access.get("aud")));
  }

  @Test
  void laCuentaDeServicioObtieneUnTokenConElRolServicio() throws Exception {
    JsonNode access = claims(tokenDeServicio());
    assertEquals(List.of("SERVICIO"), valores(access.get("roles")));
    assertTrue(valores(access.get("aud")).contains(API_AUDIENCE), access.toString());
    assertFalse(access.has("permisos"));
    assertFalse(access.has("personaId"));
  }

  // E2-07: la cuenta de pruebas llama a los servicios como ADMIN con la seguridad encendida.
  @Test
  void laCuentaDePruebasObtieneUnTokenConElRolAdmin() throws Exception {
    JsonNode access = claims(tokenDePruebas());
    assertEquals(List.of("ADMIN"), valores(access.get("roles")));
    assertTrue(valores(access.get("aud")).contains(API_AUDIENCE), access.toString());
    assertFalse(access.has("permisos"));
    assertFalse(access.has("personaId"));
  }

  // Es la credencial más poderosa del realm: ser ADMIN de DonaTrack no da poder sobre Keycloak.
  @Test
  void laCuentaDePruebasNoAdministraElRealm() throws Exception {
    String pruebas = tokenDePruebas();
    HttpResponse<String> roles = get(baseUrl + "/admin/realms/" + REALM + "/roles", pruebas);
    assertEquals(403, roles.statusCode(), roles.body());
    HttpResponse<String> usuarios = get(baseUrl + "/admin/realms/" + REALM + "/users", pruebas);
    assertEquals(403, usuarios.statusCode(), usuarios.body());
  }

  @Test
  void laCuentaDePruebasSoloHaceClientCredentials() throws Exception {
    Map<String, String> campos = new LinkedHashMap<>();
    campos.put("grant_type", "password");
    campos.put("client_id", PRUEBAS_CLIENT_ID);
    campos.put("client_secret", PRUEBAS_SECRET);
    campos.put("username", ADMIN_DONATRACK_EMAIL);
    campos.put("password", ADMIN_DONATRACK_CLAVE);
    HttpResponse<String> respuesta = post(HTTP, urlDeToken(REALM), form(campos));
    assertTrue(respuesta.statusCode() >= 400 && respuesta.statusCode() < 500, respuesta.body());
    assertTrue(respuesta.body().contains("unauthorized_client"), respuesta.body());
  }

  // El secreto de una cuenta de servicio no sirve para la otra: no se pasa de SERVICIO a ADMIN.
  @Test
  void elSecretoDeUnaCuentaDeServicioNoSirveParaLaOtra() throws Exception {
    Map<String, String> pidiendoAdmin = new LinkedHashMap<>();
    pidiendoAdmin.put("grant_type", "client_credentials");
    pidiendoAdmin.put("client_id", PRUEBAS_CLIENT_ID);
    pidiendoAdmin.put("client_secret", CLIENT_SECRET);
    assertEquals(401, post(HTTP, urlDeToken(REALM), form(pidiendoAdmin)).statusCode());

    Map<String, String> alReves = new LinkedHashMap<>();
    alReves.put("grant_type", "client_credentials");
    alReves.put("client_id", CLIENT_ID);
    alReves.put("client_secret", PRUEBAS_SECRET);
    assertEquals(401, post(HTTP, urlDeToken(REALM), form(alReves)).statusCode());
  }

  // Lo que necesita el alta de perfil: crear la cuenta, buscarla y editarle personaId y roles.
  @Test
  void laCuentaDeServicioCreaBuscaYEditaUsuarios() throws Exception {
    String servicio = tokenDeServicio();
    String id = crearUsuario(servicio, ALTA_POR_SERVICIO_EMAIL, USER_PASSWORD, null);
    asignarRolesDeLaApi(servicio, id, List.of("DONANTE_PERSONA"));

    String email = URLEncoder.encode(ALTA_POR_SERVICIO_EMAIL, StandardCharsets.UTF_8);
    String busqueda = baseUrl + "/admin/realms/" + REALM + "/users?exact=true&email=" + email;
    HttpResponse<String> encontrados = get(busqueda, servicio);
    assertEquals(200, encontrados.statusCode(), encontrados.body());
    assertEquals(id, JSON.readTree(encontrados.body()).get(0).get("id").asText());

    assertEquals(204, editarAtributo(servicio, id, "personaId", PERSONA_ID).statusCode());
    assertEquals(PERSONA_ID, atributoDelUsuario(id, "personaId"));
  }

  @Test
  void laCuentaDeServicioNoAdministraElRealm() throws Exception {
    String servicio = tokenDeServicio();
    HttpResponse<String> roles = get(baseUrl + "/admin/realms/" + REALM + "/roles", servicio);
    assertEquals(403, roles.statusCode(), roles.body());
    String urlProveedores = baseUrl + "/admin/realms/" + REALM + "/identity-provider/instances";
    HttpResponse<String> proveedores = get(urlProveedores, servicio);
    assertEquals(403, proveedores.statusCode(), proveedores.body());
  }

  // El usuario edita su cuenta por la Account API de Keycloak: personaId no se toca desde ahí.
  @Test
  void unUsuarioNoPuedeCambiarSuPersonaId() throws Exception {
    String admin = tokenDeAdmin();
    String id = crearUsuario(admin, CUENTA_EMAIL, USER_PASSWORD);
    assertEquals(204, editarAtributo(admin, id, "personaId", PERSONA_ID).statusCode());

    String propio = tokenDeLaCuenta(CUENTA_EMAIL, USER_PASSWORD);
    String urlCuenta = baseUrl + "/realms/" + REALM + "/account";
    HttpResponse<String> cuenta = get(urlCuenta, propio);
    assertEquals(200, cuenta.statusCode(), cuenta.body());
    ObjectNode cambio = (ObjectNode) JSON.readTree(cuenta.body());
    cambio.putObject("attributes").putArray("personaId").add(OTRA_PERSONA_ID);

    HttpResponse<String> intento = postJson(urlCuenta, propio, cambio.toString());
    int estado = intento.statusCode();
    // Que la Account API haya atendido el pedido: si no, el test pasaría sin probar nada.
    assertTrue(estado != 401 && estado != 403, estado + " " + intento.body());
    assertEquals(PERSONA_ID, atributoDelUsuario(id, "personaId"));
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

  private static HttpResponse<String> putJson(String url, String token, String cuerpo)
      throws IOException, InterruptedException {
    HttpRequest peticion =
        HttpRequest.newBuilder(URI.create(url))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + token)
            .PUT(HttpRequest.BodyPublishers.ofString(cuerpo))
            .build();
    return HTTP.send(peticion, HttpResponse.BodyHandlers.ofString());
  }

  private static HttpResponse<String> get(String url, String token)
      throws IOException, InterruptedException {
    HttpRequest peticion =
        HttpRequest.newBuilder(URI.create(url))
            .header("Authorization", "Bearer " + token)
            .header("Accept", "application/json")
            .GET()
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

  // Client credentials del cliente liviano: el token técnico SERVICIO.
  private static String tokenDeServicio() throws IOException, InterruptedException {
    Map<String, String> campos = new LinkedHashMap<>();
    campos.put("grant_type", "client_credentials");
    campos.put("client_id", CLIENT_ID);
    campos.put("client_secret", CLIENT_SECRET);
    HttpResponse<String> respuesta = post(HTTP, urlDeToken(REALM), form(campos));
    assertEquals(200, respuesta.statusCode(), respuesta.body());
    return JSON.readTree(respuesta.body()).get("access_token").asText();
  }

  // Client credentials de la cuenta de pruebas: el token ADMIN de integration-tests, k6 y Postman.
  private static String tokenDePruebas() throws IOException, InterruptedException {
    Map<String, String> campos = new LinkedHashMap<>();
    campos.put("grant_type", "client_credentials");
    campos.put("client_id", PRUEBAS_CLIENT_ID);
    campos.put("client_secret", PRUEBAS_SECRET);
    HttpResponse<String> respuesta = post(HTTP, urlDeToken(REALM), form(campos));
    assertEquals(200, respuesta.statusCode(), respuesta.body());
    return JSON.readTree(respuesta.body()).get("access_token").asText();
  }

  // Token del propio usuario para la Account API, por admin-cli del realm: el cliente liviano no
  // tiene password grant ni los roles del cliente account en su scope.
  private static String tokenDeLaCuenta(String email, String clave)
      throws IOException, InterruptedException {
    Map<String, String> campos = new LinkedHashMap<>();
    campos.put("grant_type", "password");
    campos.put("client_id", "admin-cli");
    campos.put("username", email);
    campos.put("password", clave);
    HttpResponse<String> respuesta = post(HTTP, urlDeToken(REALM), form(campos));
    assertEquals(200, respuesta.statusCode(), respuesta.body());
    return JSON.readTree(respuesta.body()).get("access_token").asText();
  }

  // Lee la cuenta y la vuelve a guardar con el atributo cambiado: un PUT parcial borraría el resto.
  private static HttpResponse<String> editarAtributo(
      String token, String id, String nombre, String valor)
      throws IOException, InterruptedException {
    String url = baseUrl + "/admin/realms/" + REALM + "/users/" + id;
    HttpResponse<String> actual = get(url, token);
    assertEquals(200, actual.statusCode(), actual.body());
    ObjectNode usuario = (ObjectNode) JSON.readTree(actual.body());
    ObjectNode atributos =
        usuario.has("attributes")
            ? (ObjectNode) usuario.get("attributes")
            : usuario.putObject("attributes");
    atributos.putArray(nombre).add(valor);
    return putJson(url, token, usuario.toString());
  }

  private static String atributoDelUsuario(String id, String nombre)
      throws IOException, InterruptedException {
    JsonNode atributos = admin("/admin/realms/" + REALM + "/users/" + id).get("attributes");
    assertNotNull(atributos, "la cuenta no tiene atributos");
    assertNotNull(atributos.get(nombre), "la cuenta no tiene " + nombre);
    return atributos.get(nombre).get(0).asText();
  }

  // Plantillas y permisos sueltos: roles del cliente donatrack-api. Se leen de los disponibles
  // para la cuenta (pide view-users), no de /clients/{id}/roles (pide view-clients).
  private static void asignarRolesDeLaApi(String token, String id, List<String> nombres)
      throws IOException, InterruptedException {
    String urlRealm = baseUrl + "/admin/realms/" + REALM;
    String urlMapeo = urlRealm + "/users/" + id + "/role-mappings/clients/" + API_CLIENT_UUID;
    HttpResponse<String> disponibles = get(urlMapeo + "/available", token);
    assertEquals(200, disponibles.statusCode(), disponibles.body());
    List<JsonNode> roles = new ArrayList<>();
    for (JsonNode rol : JSON.readTree(disponibles.body())) {
      if (nombres.contains(rol.get("name").asText())) {
        roles.add(rol);
      }
    }
    assertEquals(nombres.size(), roles.size(), "no están disponibles todos los roles " + nombres);
    HttpResponse<String> asignado = postJson(urlMapeo, token, JSON.writeValueAsString(roles));
    assertEquals(204, asignado.statusCode(), asignado.body());
  }

  // Con nombre y email cargados, para que la ausencia de esos claims en el token sea significativa.
  // Con clave == null se crea sin credenciales (para probar la política de clave).
  private static String crearUsuario(String tokenAdmin, String email, String clave)
      throws IOException, InterruptedException {
    return crearUsuario(tokenAdmin, email, clave, "DONANTE");
  }

  private static String crearUsuario(
      String tokenAdmin, String email, String clave, String rolDelRealm)
      throws IOException, InterruptedException {
    Map<String, Object> usuario = new LinkedHashMap<>();
    usuario.put("username", email);
    usuario.put("email", email);
    usuario.put("firstName", "Dona");
    usuario.put("lastName", "Tante");
    usuario.put("enabled", true);
    usuario.put("emailVerified", true);
    if (clave != null) {
      usuario.put("credentials", List.of(credencial(clave)));
    }

    String urlUsuarios = baseUrl + "/admin/realms/" + REALM + "/users";
    HttpResponse<String> creado =
        postJson(urlUsuarios, tokenAdmin, JSON.writeValueAsString(usuario));
    assertEquals(201, creado.statusCode(), creado.body());
    String ubicacion = creado.headers().firstValue("Location").orElseThrow();
    String id = ubicacion.substring(ubicacion.lastIndexOf('/') + 1);
    if (rolDelRealm == null) {
      return id;
    }

    HttpRequest pedirRol =
        HttpRequest.newBuilder(
                URI.create(baseUrl + "/admin/realms/" + REALM + "/roles/" + rolDelRealm))
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

  private static Map<String, Object> credencial(String clave) {
    Map<String, Object> credencial = new LinkedHashMap<>();
    credencial.put("type", "password");
    credencial.put("value", clave);
    credencial.put("temporary", false);
    return credencial;
  }

  private static HttpResponse<String> cambiarClave(String tokenAdmin, String id, String clave)
      throws IOException, InterruptedException {
    String url = baseUrl + "/admin/realms/" + REALM + "/users/" + id + "/reset-password";
    return putJson(url, tokenAdmin, JSON.writeValueAsString(credencial(clave)));
  }

  // Keycloak puede registrar los fallos de forma asíncrona: se espera a que la cuenta figure
  // bloqueada antes de intentar entrar con la clave correcta.
  private static void esperarBloqueo(String id) throws IOException, InterruptedException {
    String ruta = "/admin/realms/" + REALM + "/attack-detection/brute-force/users/" + id;
    for (int espera = 0; espera < 20; espera++) {
      if (admin(ruta).get("disabled").asBoolean()) {
        return;
      }
      Thread.sleep(250);
    }
    throw new AssertionError("Keycloak no bloqueó la cuenta después de 5 fallos");
  }

  private static HttpResponse<String> refrescar(String refresh)
      throws IOException, InterruptedException {
    Map<String, String> campos = new LinkedHashMap<>();
    campos.put("grant_type", "refresh_token");
    campos.put("client_id", CLIENT_ID);
    campos.put("client_secret", CLIENT_SECRET);
    campos.put("refresh_token", refresh);
    return post(HTTP, urlDeToken(REALM), form(campos));
  }

  // Abre la pantalla de Keycloak: la de login o, con prompt == "create", la de registro.
  private static HttpResponse<String> abrirPantalla(HttpClient cliente, String prompt)
      throws IOException, InterruptedException {
    Map<String, String> autorizacion = new LinkedHashMap<>();
    autorizacion.put("client_id", CLIENT_ID);
    autorizacion.put("redirect_uri", REDIRECT_URI);
    autorizacion.put("response_type", "code");
    autorizacion.put("scope", "openid");
    autorizacion.put("state", ESTADO);
    if (prompt != null) {
      autorizacion.put("prompt", prompt);
    }
    String urlAuth =
        baseUrl + "/realms/" + REALM + "/protocol/openid-connect/auth?" + form(autorizacion);
    HttpResponse<String> pantalla =
        cliente.send(
            HttpRequest.newBuilder(URI.create(urlAuth)).GET().build(),
            HttpResponse.BodyHandlers.ofString());
    assertEquals(200, pantalla.statusCode(), pantalla.body());
    return pantalla;
  }

  private static HttpResponse<String> descargar(String ruta)
      throws IOException, InterruptedException {
    HttpRequest peticion = HttpRequest.newBuilder(URI.create(baseUrl + ruta)).GET().build();
    return HTTP.send(peticion, HttpResponse.BodyHandlers.ofString());
  }

  // El tema se aplica, la pantalla está en español y Keycloak sirve el CSS y el logo del tema.
  private static void verificarTema(String pantalla) throws IOException, InterruptedException {
    assertTrue(pantalla.contains("lang=\"es\""), "la pantalla no está en español");
    assertTrue(pantalla.contains("DonaTrack"), "la pantalla no muestra el nombre DonaTrack");
    Matcher css = CSS_DEL_TEMA.matcher(pantalla);
    assertTrue(css.find(), "la pantalla no carga /login/donatrack/css/donatrack.css");
    String rutaCss = css.group(1);
    HttpResponse<String> hoja = descargar(rutaCss);
    assertEquals(200, hoja.statusCode(), rutaCss);
    assertTrue(hoja.body().contains("--color-accion-primaria"), "el CSS no define los tokens");
    String rutaLogo = rutaCss.replace("css/donatrack.css", "img/logo.svg");
    HttpResponse<String> logo = descargar(rutaLogo);
    assertEquals(200, logo.statusCode(), rutaLogo);
    assertTrue(logo.body().contains("<svg"), "el logo no es un SVG");
    String rutaLogoOscuro = rutaCss.replace("css/donatrack.css", "img/logo-oscuro.svg");
    HttpResponse<String> logoOscuro = descargar(rutaLogoOscuro);
    assertEquals(200, logoOscuro.statusCode(), rutaLogoOscuro);
    assertTrue(logoOscuro.body().contains("<svg"), "el logo oscuro no es un SVG");
  }

  private record ResultadoDeLogin(HttpResponse<String> envio, List<String> galletasRecibidas) {}

  // Lo que hace el navegador: abre la pantalla de Keycloak y envía su formulario (sin seguir el
  // redirect). Con prompt == "create" la pantalla es la de registro.
  private static ResultadoDeLogin enviar(
      String prompt, Pattern accionDelFormulario, Map<String, String> campos)
      throws IOException, InterruptedException {
    HttpClient navegador =
        HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();

    HttpResponse<String> pantalla = abrirPantalla(navegador, prompt);

    Matcher accion = accionDelFormulario.matcher(pantalla.body());
    assertTrue(accion.find(), "no se encontró el formulario en la pantalla de Keycloak");
    String urlFormulario = accion.group(1).replace("&amp;", "&");

    HttpResponse<String> envio = post(navegador, urlFormulario, form(campos), galletas(pantalla));
    return new ResultadoDeLogin(envio, galletasRecibidas(pantalla));
  }

  private static ResultadoDeLogin enviarLogin(String email, String clave)
      throws IOException, InterruptedException {
    Map<String, String> credenciales = new LinkedHashMap<>();
    credenciales.put("username", email);
    credenciales.put("password", clave);
    credenciales.put("credentialId", "");
    return enviar(null, ACCION_DE_LOGIN, credenciales);
  }

  // Sin nombre ni apellido: el perfil de usuario no los exige (A1-02b).
  private static ResultadoDeLogin enviarRegistro(String email, String clave)
      throws IOException, InterruptedException {
    Map<String, String> campos = new LinkedHashMap<>();
    campos.put("email", email);
    campos.put("password", clave);
    campos.put("password-confirm", clave);
    return enviar("create", ACCION_DE_REGISTRO, campos);
  }

  private static JsonNode iniciarSesionYCanjearElCodigo(String email, String clave)
      throws IOException, InterruptedException {
    return canjearElCodigo(enviarLogin(email, clave));
  }

  private static JsonNode registrarseYCanjearElCodigo(String email, String clave)
      throws IOException, InterruptedException {
    return canjearElCodigo(enviarRegistro(email, clave));
  }

  // Tras el formulario Keycloak redirige al cliente con el código, que se canjea por tokens.
  private static JsonNode canjearElCodigo(ResultadoDeLogin resultado)
      throws IOException, InterruptedException {
    HttpResponse<String> envio = resultado.envio();
    String detalle =
        "Keycloak no redirigió tras el formulario: status="
            + envio.statusCode()
            + ", Set-Cookie recibidas="
            + resultado.galletasRecibidas()
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

  private static String idDelUsuario(String email) throws IOException, InterruptedException {
    String consulta = URLEncoder.encode(email, StandardCharsets.UTF_8);
    JsonNode usuarios = admin("/admin/realms/" + REALM + "/users?exact=true&email=" + consulta);
    assertEquals(1, usuarios.size(), "no se encontró un único usuario con el email " + email);
    return usuarios.get(0).get("id").asText();
  }

  private static List<String> nombresDeRoles(JsonNode roles) {
    List<String> nombres = new ArrayList<>();
    roles.forEach(rol -> nombres.add(rol.get("name").asText()));
    nombres.sort(null);
    return nombres;
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
    return mensaje.find() ? mensaje.group(1).trim() : SIN_MENSAJE;
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

  private static JsonNode atributo(JsonNode perfil, String nombre) {
    for (JsonNode atributo : perfil.get("attributes")) {
      if (nombre.equals(atributo.get("name").asText())) {
        return atributo;
      }
    }
    throw new AssertionError("el perfil de usuario no define el atributo " + nombre);
  }

  private static boolean esObligatorio(JsonNode atributo) {
    JsonNode requerido = atributo.get("required");
    return requerido != null && !requerido.isNull();
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
