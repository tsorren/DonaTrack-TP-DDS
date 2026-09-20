package grupo5.notificaciones.mothers;

import grupo5.notificaciones.models.entities.personas.Correo;
import grupo5.notificaciones.models.entities.personas.Telefono;
import grupo5.notificaciones.models.entities.personas.TipoTelefono;

/**
 * Object Mother para {@code Correo}/{@code Telefono} (Oleada 8). Nunca sufijo {@code *Test} — ver
 * plan v2 §6, catálogo de errores recurrentes.
 */
public final class MedioDeContactoMother {

  private MedioDeContactoMother() {}

  public static Correo correo() {
    Correo correo = new Correo();
    correo.setDireccionCorreo("contacto@test.com");
    return correo;
  }

  public static Correo correoPredeterminado() {
    Correo correo = correo();
    correo.marcarComoPredeterminado();
    return correo;
  }

  public static Telefono telefono() {
    return telefono(TipoTelefono.ESTANDAR);
  }

  public static Telefono telefono(TipoTelefono tipo) {
    Telefono telefono = new Telefono();
    telefono.setCaracteristica("+54");
    telefono.setCodigoArea("11");
    telefono.setNumero("12345678");
    telefono.setTipo(tipo);
    return telefono;
  }

  public static Telefono telefonoPredeterminado(TipoTelefono tipo) {
    Telefono telefono = telefono(tipo);
    telefono.marcarComoPredeterminado();
    return telefono;
  }

  /**
   * Medio con {@code esPredeterminado == null} — estado que hoy solo se alcanza sin pasar por
   * {@code marcarComoPredeterminado()}/{@code desmarcarComoPredeterminado()} (ej. si algo asignara
   * el campo directamente); no es alcanzable a través de {@code MedioDeContactoMapper} (que siempre
   * invoca uno de los dos métodos), pero se prueba igual porque el contrato del getter sigue siendo
   * {@code Boolean} nullable. El constructor de {@code MedioDeContacto} ya deja {@code
   * esPredeterminado = false} por defecto, así que para llegar a {@code null} hay que forzarlo —
   * acá se hace con un subtipo anónimo mínimo en vez de reflection. Se usa para probar el guard de
   * {@code Notificacion.ordenarMedios()} (RF-07, Oleada 9.5): trata el {@code null} como "no
   * predeterminado" en vez de explotar con {@code NullPointerException}.
   */
  public static Correo correoConEsPredeterminadoNulo() {
    Correo correo =
        new Correo() {
          @Override
          public Boolean getEsPredeterminado() {
            return null;
          }
        };
    correo.setDireccionCorreo("contacto@test.com");
    return correo;
  }
}
