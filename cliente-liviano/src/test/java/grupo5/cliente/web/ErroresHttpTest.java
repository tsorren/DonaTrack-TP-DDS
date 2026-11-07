package grupo5.cliente.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

/** Qué página ve el usuario cuando una API responde con error o no responde (RF-CL-08). */
class ErroresHttpTest {

  /** Simula la respuesta de una API con el código que pide la dirección; 0 es que no responde. */
  @RestController
  static class Api {

    @GetMapping("/api/{codigo}")
    String responder(@PathVariable int codigo) {
      if (codigo == 0) {
        throw new ResourceAccessException("Connection refused");
      }
      HttpStatus estado = HttpStatus.valueOf(codigo);
      throw estado.is4xxClientError()
          ? HttpClientErrorException.create(estado, "", null, null, null)
          : HttpServerErrorException.create(estado, "", null, null, null);
    }
  }

  private final MockMvc mvc =
      MockMvcBuilders.standaloneSetup(new Api()).setControllerAdvice(new ErroresHttp()).build();

  @Test
  void sinSesionVaAIngresar() throws Exception {
    mvc.perform(get("/api/401")).andExpect(redirectedUrl("/publico-ingresar"));
  }

  @Test
  void sinPermisoYNoEncontradoTienenSuPagina() throws Exception {
    mvc.perform(get("/api/403")).andExpect(status().isForbidden());
    mvc.perform(get("/api/404")).andExpect(status().isNotFound());
  }

  @Test
  void cualquierOtraFallaOUnaApiCaidaEsUnaFallaNuestra() throws Exception {
    mvc.perform(get("/api/500")).andExpect(status().isBadGateway());
    mvc.perform(get("/api/409")).andExpect(status().isBadGateway());
    mvc.perform(get("/api/0")).andExpect(status().isBadGateway());
  }
}
