package grupo5.cliente.rest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import grupo5.cliente.puertos.DonantePerfilPuerto.Tipo;
import grupo5.cliente.vistas.CuentaVista;
import grupo5.cliente.vistas.DonantePerfilVista;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Mi perfil del donante contra respuestas de ejemplo de Donaciones e Incentivos. */
class DonantePerfilRestTest {

  private static final UUID DONANTE = UUID.fromString("6f1c2c1e-0000-4000-8000-0000000000aa");
  private static final String DONACIONES = "http://donaciones/api/donantes/" + DONANTE;
  private static final String LEGALES = "http://donaciones/api/personas/" + DONANTE + "/legales";
  private static final String INCENTIVOS = "http://incentivos/api/incentivos/donantes/" + DONANTE;

  private static final String EMPRESA =
      """
      {"idDonante": "%s", "mostrarNombre": false, "persona": {
        "tipo": "JURIDICA", "razonSocial": "Distribuidora Ejemplo SA", "rubro": "Alimentos",
        "documento": "30-00000000-0", "tipoJuridico": "EMPRESA",
        "direccion": {"calle": "Avenida Ejemplo", "altura": 1234, "piso": 3, "departamento": "B",
          "codigoPostal": "C1000", "localidad": "Caballito", "provincia": "Ciudad de Buenos Aires"},
        "mediosDeContacto": [
          {"tipo": "WHATSAPP", "codigoArea": "11", "numero": "5555-0100"},
          {"tipo": "CORREO", "direccionCorreo": "contacto@distribuidora-ejemplo.test"}]}}
      """
          .formatted(DONANTE);

  private MockRestServiceServer apis;
  private DonantePerfilRest adaptador;

  @BeforeEach
  void armar() {
    RestClient.Builder builder = RestClient.builder();
    apis = MockRestServiceServer.bindTo(builder).build();
    adaptador =
        new DonantePerfilRest(
            ClientesHttp.cliente(builder, "http://donaciones", DonacionesApi.class),
            ClientesHttp.cliente(builder, "http://incentivos", IncentivosApi.class));
  }

  @Test
  void armaElPerfilDeLaEmpresaConSuCategoriaYSuConstancia() {
    apis.expect(requestTo(DONACIONES)).andRespond(withSuccess(EMPRESA, MediaType.APPLICATION_JSON));
    apis.expect(requestTo(INCENTIVOS))
        .andRespond(withSuccess("{\"categoria\": \"SOSTENEDOR\"}", MediaType.APPLICATION_JSON));
    apis.expect(requestTo(LEGALES))
        .andRespond(
            withSuccess(
                "{\"versionAceptada\": 1, \"aceptadaEn\": \"2026-11-01T10:00:00\","
                    + " \"versionVigente\": 1, \"vigenteAceptada\": true}",
                MediaType.APPLICATION_JSON));

    DonantePerfilVista perfil = adaptador.perfil(DONANTE);

    apis.verify();
    assertEquals("Distribuidora Ejemplo SA", perfil.nombre());
    assertEquals("Empresa donante · contacto@distribuidora-ejemplo.test", perfil.cuenta());
    assertEquals("Empresa donante · Sostenedor", perfil.anonimo());
    assertTrue(perfil.oculto());
    assertEquals("30-00000000-0", perfil.cuit());
    assertEquals("1234", perfil.altura());
    assertEquals("3.º B", perfil.piso());
    assertEquals("11 5555-0100", perfil.whatsapp());
    assertNull(perfil.telefono());
    assertEquals("Aceptaste la versión 1 el 1 de noviembre de 2026.", perfil.legales());
  }

  @Test
  void unaPersonaSinIncentivosNiTerminosAceptadosNoRompeElPerfil() {
    apis.expect(requestTo(DONACIONES))
        .andRespond(
            withSuccess(
                "{\"mostrarNombre\": true, \"persona\": {\"tipo\": \"HUMANA\","
                    + " \"nombre\": \"Martina\", \"apellido\": \"Ruiz\","
                    + " \"tipoDocumento\": \"DNI\", \"documento\": \"12345678\"}}",
                MediaType.APPLICATION_JSON));
    apis.expect(requestTo(INCENTIVOS)).andRespond(withServerError());
    apis.expect(requestTo(LEGALES))
        .andRespond(withSuccess("{\"versionVigente\": 1}", MediaType.APPLICATION_JSON));

    DonantePerfilVista perfil = adaptador.perfil(DONANTE);

    assertEquals("Martina Ruiz", perfil.nombre());
    assertEquals("Persona donante", perfil.cuenta());
    assertEquals("Donante", perfil.anonimo());
    assertFalse(perfil.oculto());
    assertNull(perfil.calle());
    assertNull(perfil.cuit());
    assertEquals("Martina", perfil.nombrePila());
    assertEquals("Ruiz", perfil.apellido());
    assertEquals("DNI 12.345.678", perfil.documento());
    assertEquals(
        "Todavía no aceptaste los términos y la política de privacidad.", perfil.legales());
  }

  @Test
  void unaCuentaSinTipoNoSeTrataComoEmpresa() {
    apis.expect(requestTo(DONACIONES))
        .andRespond(
            withSuccess(
                "{\"persona\": {\"nombre\": \"Ana\", \"apellido\": \"Ejemplo\","
                    + " \"documento\": \"30-00000000-0\", \"mediosDeContacto\": [{\"tipo\":"
                    + " \"CORREO\", \"direccionCorreo\": \"ana@correo-ejemplo.test\"}]}}",
                MediaType.APPLICATION_JSON));
    apis.expect(requestTo(INCENTIVOS))
        .andRespond(withSuccess("{\"categoria\": \"SOSTENEDOR\"}", MediaType.APPLICATION_JSON));
    apis.expect(requestTo(LEGALES))
        .andRespond(withSuccess("{\"versionVigente\": 1}", MediaType.APPLICATION_JSON));

    DonantePerfilVista perfil = adaptador.perfil(DONANTE);

    assertEquals("Ana Ejemplo", perfil.nombre());
    assertEquals("Donante · ana@correo-ejemplo.test", perfil.cuenta());
    assertEquals("Donante Sostenedor", perfil.anonimo());
    assertNull(perfil.cuit());
    assertNull(perfil.rubro());
    assertEquals("30-00000000-0", perfil.documento());
  }

  @Test
  void sinNombreNiApellidoElNombreEsTuCuenta() {
    apis.expect(requestTo(DONACIONES))
        .andRespond(withSuccess("{\"mostrarNombre\": true}", MediaType.APPLICATION_JSON));
    apis.expect(requestTo(INCENTIVOS)).andRespond(withServerError());
    apis.expect(requestTo(LEGALES))
        .andRespond(withSuccess("{\"versionVigente\": 1}", MediaType.APPLICATION_JSON));

    DonantePerfilVista perfil = adaptador.perfil(DONANTE);

    assertEquals("Tu cuenta", perfil.nombre());
    assertEquals("Donante", perfil.cuenta());
  }

  @Test
  void elTipoSaleDeLaPersonaDeDonaciones() {
    apis.expect(requestTo(DONACIONES))
        .andRespond(
            withSuccess("{\"persona\": {\"tipo\": \"HUMANA\"}}", MediaType.APPLICATION_JSON));
    apis.expect(requestTo(DONACIONES)).andRespond(withSuccess(EMPRESA, MediaType.APPLICATION_JSON));
    apis.expect(requestTo(DONACIONES))
        .andRespond(withSuccess("{\"persona\": {}}", MediaType.APPLICATION_JSON));
    apis.expect(requestTo(DONACIONES)).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

    assertEquals(Tipo.PERSONA, adaptador.tipo(DONANTE));
    assertEquals(Tipo.EMPRESA, adaptador.tipo(DONANTE));
    assertEquals(Tipo.SIN_TIPO, adaptador.tipo(DONANTE));
    assertEquals(Tipo.SIN_TIPO, adaptador.tipo(DONANTE));
    apis.verify();
  }

  @Test
  void mostrarElNombreLoGuardaEnLasPreferencias() {
    apis.expect(requestTo(DONACIONES + "/preferencias"))
        .andExpect(method(HttpMethod.PATCH))
        .andExpect(content().json("{\"mostrarNombre\": true}"))
        .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

    adaptador.mostrarNombre(DONANTE, true);

    assertDoesNotThrow(() -> apis.verify());
  }

  @Test
  void laCuentaDelMenuEsElNombreYElTipoConUnSoloPedido() {
    apis.expect(requestTo(DONACIONES)).andRespond(withSuccess(EMPRESA, MediaType.APPLICATION_JSON));

    CuentaVista cuenta = adaptador.cuenta(DONANTE);

    apis.verify();
    assertEquals("Distribuidora Ejemplo SA", cuenta.nombre());
    assertEquals("Empresa donante", cuenta.rol());
  }

  @Test
  void elDonanteJuridicoQueNoEsEmpresaDiceSuTipoYNoOrganizacion() {
    apis.expect(requestTo(DONACIONES))
        .andRespond(
            withSuccess(
                "{\"persona\": {\"tipo\": \"JURIDICA\", \"razonSocial\": \"Fundación Ejemplo\","
                    + " \"tipoJuridico\": \"ONG\"}}",
                MediaType.APPLICATION_JSON));

    CuentaVista cuenta = adaptador.cuenta(DONANTE);

    assertEquals("Fundación Ejemplo", cuenta.nombre());
    assertEquals("ONG donante", cuenta.rol());
  }
}
