package grupo5.donaciones.models.entities.actividad;

/**
 * Qué hizo una persona de administración (R11-26, spec R11-p2-actividad §3.3). Las nueve primeras
 * pasan por Donaciones y las registra Donaciones; las cinco últimas pasan por Keycloak o Logística
 * y las anota el sitio con {@code POST /api/actividad}.
 */
public enum AccionDeAdministracion {
  PROPUESTA_APROBADA(false),
  PROPUESTA_DESCARTADA(false),
  ASIGNACION_CORRIDA(false),
  DONACION_REGISTRADA(false),
  DONACION_VENCIDA(false),
  DONANTE_REGISTRADO(false),
  IMPORTACION_SUBIDA(false),
  ORGANIZACION_APROBADA(false),
  ORGANIZACION_REGISTRADA(false),
  ADMINISTRADOR_DADO_DE_ALTA(true),
  CONTRASENA_PROVISORIA_NUEVA(true),
  CAMION_AGREGADO(true),
  CAMION_DESHABILITADO(true),
  CAMION_HABILITADO(true);

  private final boolean laAnotaElSitio;

  AccionDeAdministracion(boolean laAnotaElSitio) {
    this.laAnotaElSitio = laAnotaElSitio;
  }

  /** Si es de las que el sitio anota (las que no pasan por Donaciones). */
  public boolean laAnotaElSitio() {
    return laAnotaElSitio;
  }

  /** Si lo que tocó es una cuenta de administración: su detalle va vacío (es una persona). */
  public boolean esSobreUnaCuenta() {
    return this == ADMINISTRADOR_DADO_DE_ALTA || this == CONTRASENA_PROVISORIA_NUEVA;
  }
}
