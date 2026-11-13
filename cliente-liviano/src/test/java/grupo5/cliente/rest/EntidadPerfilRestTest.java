package grupo5.cliente.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import grupo5.cliente.vistas.CuentaVista;
import grupo5.cliente.vistas.EntidadPerfilVista;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** El adaptador REST de Mi perfil de la entidad contra respuestas de Donaciones. */
class EntidadPerfilRestTest {

  private static final UUID ENTIDAD = UUID.fromString("6f1c2c1e-0000-4000-8000-0000000000e1");
  private static final String BASE = "http://donaciones/api/";

  private final RestClient.Builder builder = RestClient.builder();
  private final MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
  private final EntidadPerfilRest adaptador =
      new EntidadPerfilRest(
          ClientesHttp.cliente(builder, "http://donaciones", DonacionesApi.class));

  @Test
  void armaElPerfilConSusMediosYLaConstancia() {
    servidor
        .expect(requestTo(BASE + "entidades/" + ENTIDAD))
        .andRespond(
            withSuccess(
                """
                {"id": "%s", "activo": true, "juridica": {"tipo": "JURIDICA",
                  "razonSocial": "Merendero Ejemplo", "tipoJuridico": "ONG", "rubro": "Merendero",
                  "direccion": {"calle": "Calle Ejemplo", "altura": 120, "localidad": "Localidad Ejemplo",
                                "provincia": "Buenos Aires"},
                  "mediosDeContacto": [
                    {"tipo": "WHATSAPP", "esPredeterminado": true, "codigoArea": "11", "numero": "5555-0300"},
                    {"tipo": "CORREO", "esPredeterminado": false, "direccionCorreo": "merendero@ejemplo.org"}],
                  "representantes": [{"tipo": "HUMANA", "nombre": "Ana", "apellido": "Ejemplo",
                    "mediosDeContacto": [{"tipo": "CORREO", "direccionCorreo": "ana@ejemplo.org"}]}]}}
                """
                    .formatted(ENTIDAD),
                MediaType.APPLICATION_JSON));
    servidor
        .expect(requestTo(BASE + "personas/" + ENTIDAD + "/legales"))
        .andRespond(
            withSuccess(
                "{\"versionAceptada\": 2, \"aceptadaEn\": \"2026-11-03T10:00:00\","
                    + " \"versionVigente\": 2, \"vigenteAceptada\": true}",
                MediaType.APPLICATION_JSON));

    EntidadPerfilVista perfil = adaptador.perfil(ENTIDAD);

    servidor.verify();
    assertEquals("Merendero Ejemplo", perfil.nombre());
    assertEquals("Merendero · Ana, ana@ejemplo.org", perfil.cuentaLinea());
    assertEquals("Merendero", perfil.queEs());
    assertEquals("asociacion", perfil.tipo());
    assertEquals("Calle Ejemplo 120, Localidad Ejemplo, Buenos Aires", perfil.direccion());
    assertEquals("Ana Ejemplo", perfil.representante());
    assertEquals("ana@ejemplo.org", perfil.correoRepresentante());
    assertEquals("merendero@ejemplo.org", perfil.correo());
    assertEquals("11 5555-0300", perfil.whatsapp());
    assertNull(perfil.telefono());
    assertEquals("whatsapp", perfil.medio());
    assertEquals("Aceptaste la versión 2 el 3 de noviembre de 2026.", perfil.legales());
  }

  @Test
  void sinRepresentanteNiMediosNiLegalesNoInventaNada() {
    servidor
        .expect(requestTo(BASE + "entidades/" + ENTIDAD))
        .andRespond(
            withSuccess(
                "{\"juridica\": {\"razonSocial\": \"Club Ejemplo\", \"tipoJuridico\": \"EMPRESA\"}}",
                MediaType.APPLICATION_JSON));
    servidor
        .expect(requestTo(BASE + "personas/" + ENTIDAD + "/legales"))
        .andRespond(withSuccess("{\"versionVigente\": 1}", MediaType.APPLICATION_JSON));

    EntidadPerfilVista perfil = adaptador.perfil(ENTIDAD);

    assertNull(perfil.cuentaLinea());
    assertNull(perfil.tipo());
    assertNull(perfil.direccion());
    assertNull(perfil.correo());
    assertNull(perfil.medio());
    assertEquals(
        "Todavía no aceptaste los términos y la política de privacidad.", perfil.legales());
  }

  @Test
  void laCuentaDelMenuEsLaRazonSocialSinPedirLaConstancia() {
    servidor
        .expect(requestTo(BASE + "entidades/" + ENTIDAD))
        .andRespond(
            withSuccess(
                "{\"juridica\": {\"razonSocial\": \"Merendero Ejemplo\"}}",
                MediaType.APPLICATION_JSON));

    CuentaVista cuenta = adaptador.cuenta(ENTIDAD);

    servidor.verify();
    assertEquals("Merendero Ejemplo", cuenta.nombre());
    assertEquals("Organización", cuenta.rol());
  }
}
