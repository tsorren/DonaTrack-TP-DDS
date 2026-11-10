package grupo5.cliente.seguridad;

import java.util.List;

/** Hasta dónde puede llegar una sesión según lo que su cuenta ya completó. */
public enum EstadoDeCuenta {
  SIN_PERFIL("/publico-registro", "/publico-registro-donante", "/publico-registro-entidad"),
  SOLICITUD_RECIBIDA("/publico-registro-entidad-enviada"),
  ACTIVA;

  private final List<String> rutas;

  EstadoDeCuenta(String... rutas) {
    this.rutas = List.of(rutas);
  }

  public boolean permite(String ruta) {
    return rutas.isEmpty() || rutas.contains(ruta);
  }

  /** La pantalla a la que se lleva a quien pidió una ruta que su estado no permite. */
  public String destino() {
    return rutas.getFirst();
  }
}
