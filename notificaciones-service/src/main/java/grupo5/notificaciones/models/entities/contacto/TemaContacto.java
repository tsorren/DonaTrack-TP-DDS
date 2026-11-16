package grupo5.notificaciones.models.entities.contacto;

/**
 * Los seis temas del formulario de contacto, con cómo se nombran en el aviso a la Administración.
 */
public enum TemaContacto {
  ENTREGA("una entrega"),
  DONACION("una donación"),
  NECESIDAD("una necesidad"),
  CUENTA("una cuenta"),
  SITIO("el sitio"),
  OTRO("otro tema");

  private final String descripcion;

  TemaContacto(String descripcion) {
    this.descripcion = descripcion;
  }

  public String getDescripcion() {
    return descripcion;
  }
}
