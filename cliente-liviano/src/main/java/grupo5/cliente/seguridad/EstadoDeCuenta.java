package grupo5.cliente.seguridad;

import java.util.List;

/** Hasta dónde puede llegar una sesión según lo que su cuenta ya completó. */
public enum EstadoDeCuenta {
  SIN_PERFIL("/publico-registro", "/publico-registro-donante", "/publico-registro-entidad"),
  /**
   * La solicitud de su entidad espera la aprobación: puede mirar lo público (lo informativo, sin el
   * ingreso ni el registro) y no lo privado.
   */
  SOLICITUD_RECIBIDA(
      "/publico-registro-entidad-enviada",
      "/publico-registro-entidad-corregir",
      "/",
      "/publico-portada",
      "/publico-entidades",
      "/publico-donaciones",
      "/publico-mapa",
      "/publico-nosotros",
      "/publico-para-donantes",
      "/publico-para-entidades",
      "/publico-legales",
      "/publico-contacto",
      "/publico-contacto-enviado",
      "/publico-contacto-error"),
  ACTIVA;

  private final List<String> rutas;

  EstadoDeCuenta(String... rutas) {
    this.rutas = List.of(rutas);
  }

  public boolean permite(String ruta) {
    return rutas.isEmpty() || rutas.contains(ruta);
  }

  /** La pantalla a la que se lleva a quien pidió una ruta que su estado no permite (la primera). */
  public String destino() {
    return rutas.getFirst();
  }
}
