package grupo5.donaciones.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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

  private static final String CLAVE_EXTERNO = "clave-sintetica-externo";
  private static final String CLAVE_ADMIN = "clave-sintetica-admin";
  private static final String RUTA_EXTERNO = "/api/logistica/proveedores/externo/avisos";

  private MockEnvironment environment;
  private ApiKeyFilter filtro;

  @BeforeEach
  void setUp() {
    environment =
        new MockEnvironment()
            .withProperty("donatrack.logistica.proveedor.externo.callback-api-key", CLAVE_EXTERNO)
            .withProperty(
                "donatrack.logistica.proveedor.otro.callback-api-key", "clave-sintetica-otro")
            .withProperty("donatrack.logistica.admin-api-key", CLAVE_ADMIN);
    filtro = new ApiKeyFilter(environment);
  }

  private MockHttpServletResponse ejecutar(String ruta, String clave, MockFilterChain cadena)
      throws ServletException, IOException {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", ruta);
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
  void rutasAjenasAlCallback_pasanSinClave() throws Exception {
    MockFilterChain cadena = new MockFilterChain();

    MockHttpServletResponse response = ejecutar("/api/donaciones", null, cadena);

    assertEquals(200, response.getStatus());
    assertTrue(pasoAlControlador(cadena));
  }

  @Test
  void avisoConLaClaveDelProveedor_pasa() throws Exception {
    MockFilterChain cadena = new MockFilterChain();

    MockHttpServletResponse response = ejecutar(RUTA_EXTERNO, CLAVE_EXTERNO, cadena);

    assertEquals(200, response.getStatus());
    assertTrue(pasoAlControlador(cadena));
  }

  @Test
  void avisoSinClave_da401YNoLlegaAlControlador() throws Exception {
    MockFilterChain cadena = new MockFilterChain();

    MockHttpServletResponse response = ejecutar(RUTA_EXTERNO, null, cadena);

    assertEquals(401, response.getStatus());
    assertFalse(pasoAlControlador(cadena));
    assertTrue(response.getContentAsString().contains("ERR-AUT-401"));
  }

  @Test
  void avisoConClaveIncorrecta_da401() throws Exception {
    MockFilterChain cadena = new MockFilterChain();

    MockHttpServletResponse response = ejecutar(RUTA_EXTERNO, "otra-clave", cadena);

    assertEquals(401, response.getStatus());
    assertFalse(pasoAlControlador(cadena));
  }

  @Test
  void laClaveDeOtroProveedor_noSirveParaAvisarComoEsteProveedor() throws Exception {
    MockFilterChain cadena = new MockFilterChain();

    MockHttpServletResponse response = ejecutar(RUTA_EXTERNO, "clave-sintetica-otro", cadena);

    assertEquals(401, response.getStatus());
    assertFalse(pasoAlControlador(cadena));
  }

  @Test
  void proveedorSinClaveConfigurada_fallaCerrado() throws Exception {
    MockFilterChain cadena = new MockFilterChain();

    MockHttpServletResponse response =
        ejecutar("/api/logistica/proveedores/sin-clave/avisos", "cualquiera", cadena);

    assertEquals(401, response.getStatus());
    assertFalse(pasoAlControlador(cadena));
  }

  @Test
  void claveConfiguradaEnBlanco_fallaCerradoAunqueLleguePorLaCabeceraVacia() throws Exception {
    environment.setProperty("donatrack.logistica.proveedor.vacio.callback-api-key", "");
    MockFilterChain cadena = new MockFilterChain();

    MockHttpServletResponse response =
        ejecutar("/api/logistica/proveedores/vacio/avisos", "", cadena);

    assertEquals(401, response.getStatus());
    assertFalse(pasoAlControlador(cadena));
  }

  @Test
  void rutaDesconocidaBajoElPrefijo_seRechaza() throws Exception {
    MockFilterChain cadena = new MockFilterChain();

    MockHttpServletResponse response =
        ejecutar("/api/logistica/proveedores/externo/otra-cosa", CLAVE_EXTERNO, cadena);

    assertEquals(401, response.getStatus());
    assertFalse(pasoAlControlador(cadena));
  }

  @Test
  void idDeProveedorConCaracteresRaros_seRechaza() throws Exception {
    MockFilterChain cadena = new MockFilterChain();

    MockHttpServletResponse response =
        ejecutar("/api/logistica/proveedores/externo.url/avisos", CLAVE_EXTERNO, cadena);

    assertEquals(401, response.getStatus());
    assertFalse(pasoAlControlador(cadena));
  }

  @Test
  void laRespuestaDeRechazoNoRevelaLasClaves() throws Exception {
    MockFilterChain cadena = new MockFilterChain();

    MockHttpServletResponse response = ejecutar(RUTA_EXTERNO, "intento", cadena);

    String cuerpo = response.getContentAsString();
    assertFalse(cuerpo.contains(CLAVE_EXTERNO));
    assertFalse(cuerpo.contains("intento"));
    assertNotNull(response.getContentType());
    assertNull(response.getHeader("X-API-Key"));
  }

  @Test
  void barrasRepetidasOSegmentosRelativos_noEsquivanLaProteccion() throws Exception {
    for (String ruta :
        new String[] {
          "//api/logistica/proveedores/externo/avisos",
          "/api//logistica///proveedores/externo/avisos",
          "/api/x/../logistica/proveedores/externo/avisos",
          "/api/logistica/./proveedores/externo/avisos"
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
        ejecutar("/api/logistica/proveedores/externo/avisos con espacio", CLAVE_EXTERNO, cadena);

    assertEquals(401, response.getStatus());
    assertFalse(pasoAlControlador(cadena));
  }

  @Test
  void rutasDeAdministracion_pasanConLaClaveDeAdministracion() throws Exception {
    for (String ruta :
        new String[] {"/api/logistica/proveedores", "/api/logistica/proveedor-preferido"}) {
      MockFilterChain cadena = new MockFilterChain();

      MockHttpServletResponse response = ejecutar(ruta, CLAVE_ADMIN, cadena);

      assertEquals(200, response.getStatus(), ruta);
      assertTrue(pasoAlControlador(cadena), ruta);
    }
  }

  @Test
  void rutasDeAdministracion_sinClaveOConClaveIncorrecta_dan401() throws Exception {
    for (String clave : new String[] {null, "otra-clave", CLAVE_EXTERNO}) {
      MockFilterChain cadena = new MockFilterChain();

      MockHttpServletResponse response =
          ejecutar("/api/logistica/proveedor-preferido", clave, cadena);

      assertEquals(401, response.getStatus(), String.valueOf(clave));
      assertFalse(pasoAlControlador(cadena));
    }
  }

  @Test
  void laClaveDeAdministracion_noSirveParaElCallbackDeUnProveedor() throws Exception {
    MockFilterChain cadena = new MockFilterChain();

    MockHttpServletResponse response = ejecutar(RUTA_EXTERNO, CLAVE_ADMIN, cadena);

    assertEquals(401, response.getStatus());
    assertFalse(pasoAlControlador(cadena));
  }

  @Test
  void sinClaveDeAdministracionConfigurada_lasRutasDeAdministracionFallanCerrado()
      throws Exception {
    environment.setProperty("donatrack.logistica.admin-api-key", "");
    MockFilterChain cadena = new MockFilterChain();

    MockHttpServletResponse response = ejecutar("/api/logistica/proveedores", "", cadena);

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
}
