package grupo5.donaciones.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ApiKeyFilterTest {

  private static final String CLAVE_ADMIN = "clave-sintetica-admin";
  private static final String RUTA_LISTA = "/api/logistica/proveedores";
  private static final String RUTA_PREFERIDO = "/api/logistica/proveedor-preferido";

  private MockEnvironment environment;
  private ApiKeyFilter filtro;

  @BeforeEach
  void setUp() {
    environment =
        new MockEnvironment().withProperty("donatrack.logistica.admin-api-key", CLAVE_ADMIN);
    filtro = new ApiKeyFilter(environment);
  }

  private MockHttpServletResponse ejecutar(String ruta, String clave, MockFilterChain cadena)
      throws ServletException, IOException {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", ruta);
    if (clave != null) {
      request.addHeader(ApiKeyFilter.HEADER_API_KEY, clave);
    }
    MockHttpServletResponse response = new MockHttpServletResponse();
    filtro.doFilter(request, response, cadena);
    return response;
  }

  private static boolean pasoAlControlador(MockFilterChain cadena) {
    return cadena.getRequest() != null;
  }

  @Test
  void rutasAjenasALaAdministracion_pasanSinClave() throws Exception {
    MockFilterChain cadena = new MockFilterChain();

    MockHttpServletResponse response = ejecutar("/api/donaciones", null, cadena);

    assertEquals(200, response.getStatus());
    assertTrue(pasoAlControlador(cadena));
  }

  @Test
  void rutasDeAdministracion_pasanConLaClaveDeAdministracion() throws Exception {
    for (String ruta : new String[] {RUTA_LISTA, RUTA_PREFERIDO}) {
      MockFilterChain cadena = new MockFilterChain();

      MockHttpServletResponse response = ejecutar(ruta, CLAVE_ADMIN, cadena);

      assertEquals(200, response.getStatus(), ruta);
      assertTrue(pasoAlControlador(cadena), ruta);
    }
  }

  @Test
  void rutasDeAdministracion_sinClaveOConClaveIncorrecta_dan401() throws Exception {
    for (String clave : new String[] {null, "", "otra-clave"}) {
      MockFilterChain cadena = new MockFilterChain();

      MockHttpServletResponse response = ejecutar(RUTA_PREFERIDO, clave, cadena);

      assertEquals(401, response.getStatus(), String.valueOf(clave));
      assertFalse(pasoAlControlador(cadena));
      assertTrue(response.getContentAsString().contains("ERR-AUT-401"));
    }
  }

  @Test
  void sinClaveDeAdministracionConfigurada_fallaCerrado() throws Exception {
    environment.setProperty("donatrack.logistica.admin-api-key", "");
    MockFilterChain cadena = new MockFilterChain();

    MockHttpServletResponse response = ejecutar(RUTA_LISTA, "", cadena);

    assertEquals(401, response.getStatus());
    assertFalse(pasoAlControlador(cadena));
  }

  @Test
  void rutaDeAdministracionDesconocida_seRechazaAunqueLaClaveSeaCorrecta() throws Exception {
    MockFilterChain cadena = new MockFilterChain();

    MockHttpServletResponse response =
        ejecutar("/api/logistica/proveedor-preferido/extra", CLAVE_ADMIN, cadena);

    assertEquals(401, response.getStatus());
    assertFalse(pasoAlControlador(cadena));
  }

  @Test
  void elCallbackHttpDeProveedoresYaNoExiste_seRechazaAunqueLaClaveSeaCorrecta() throws Exception {
    MockFilterChain cadena = new MockFilterChain();

    MockHttpServletResponse response =
        ejecutar("/api/logistica/proveedores/externo/avisos", CLAVE_ADMIN, cadena);

    assertEquals(401, response.getStatus());
    assertFalse(pasoAlControlador(cadena));
  }

  @Test
  void barrasRepetidasOSegmentosRelativos_noEsquivanLaProteccion() throws Exception {
    for (String ruta :
        new String[] {
          "//api/logistica/proveedores",
          "/api//logistica///proveedor-preferido",
          "/api/x/../logistica/proveedores",
          "/api/logistica/./proveedor-preferido"
        }) {
      MockFilterChain cadena = new MockFilterChain();

      MockHttpServletResponse response = ejecutar(ruta, null, cadena);

      assertEquals(401, response.getStatus(), ruta);
      assertFalse(pasoAlControlador(cadena), ruta);
    }
  }

  @Test
  void uriNoInterpretable_fallaCerrado() throws Exception {
    MockFilterChain cadena = new MockFilterChain();

    MockHttpServletResponse response =
        ejecutar("/api/logistica/proveedores con espacio", CLAVE_ADMIN, cadena);

    assertEquals(401, response.getStatus());
    assertFalse(pasoAlControlador(cadena));
  }

  @Test
  void laRespuestaDeRechazoNoRevelaLasClaves() throws Exception {
    MockFilterChain cadena = new MockFilterChain();

    MockHttpServletResponse response = ejecutar(RUTA_LISTA, "intento", cadena);

    String cuerpo = response.getContentAsString();
    assertFalse(cuerpo.contains(CLAVE_ADMIN));
    assertFalse(cuerpo.contains("intento"));
    assertNotNull(response.getContentType());
  }
}
