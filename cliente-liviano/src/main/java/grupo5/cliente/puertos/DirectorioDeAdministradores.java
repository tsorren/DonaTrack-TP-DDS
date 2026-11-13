package grupo5.cliente.puertos;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Las cuentas de administración (R11-10, ficha docs/interfaz/flujos/r11-p1-admins.md): cada persona
 * de la administración con su cuenta, su perfil y el alta de otra. Fuera de la maqueta, en el
 * proveedor de ingreso (Keycloak) por la cuenta de servicio del sitio; se reconocen por el atributo
 * {@code administracion}. Nunca devuelve la lista entera: de a {@link #POR_PAGINA}.
 */
public interface DirectorioDeAdministradores {

  /** Cuentas por página de la lista. */
  int POR_PAGINA = 10;

  /**
   * Una cuenta de administración. {@code nombre} y {@code apellido} pueden faltar (el admin del
   * seed, hasta que complete Mi perfil); {@code entro} es false mientras tiene la contraseña
   * provisoria sin cambiar.
   */
  record Administrador(
      String id, String nombre, String apellido, String correo, LocalDate creada, boolean entro) {

    /** "Ana Pérez", o null si no tiene nombre cargado. */
    public String nombreCompleto() {
      String completo =
          ((nombre == null ? "" : nombre.strip())
                  + " "
                  + (apellido == null ? "" : apellido.strip()))
              .strip();
      return completo.isEmpty() ? null : completo;
    }
  }

  /** Los datos del alta: con el correo entra. */
  record Alta(String nombre, String apellido, String correo) {}

  /** Cuántas cuentas de administración hay: el total del servicio, no el de una página. */
  long total();

  /** La página {@code pagina} (desde 1) de las cuentas, de a {@link #POR_PAGINA}, por correo. */
  List<Administrador> pagina(int pagina);

  /**
   * Las cuentas de administración cuyo nombre, apellido o correo contienen {@code texto}, como
   * mucho {@code max} (R11-26: la búsqueda por administrador del registro de actividad pide 21 para
   * saber si son más de 20). Nunca la lista entera.
   */
  List<Administrador> buscar(String texto, int max);

  /** La cuenta por su id; vacío si no existe o no es de administración. */
  Optional<Administrador> cuenta(String id);

  /** Cambia el nombre y el apellido de la cuenta y deja el resto como estaba. */
  void cambiarNombre(String id, String nombre, String apellido);

  /**
   * Da de alta una cuenta de administración con la contraseña provisoria (Keycloak pide cambiarla
   * en el primer ingreso), marcada como dada de alta por {@code altaPor}. Termina con la cuenta
   * completa o sin cuenta: si falla después de crearla, la borra y lanza {@link AltaIncompleta}.
   *
   * @return el id de la cuenta nueva
   * @throws CorreoEnUso si ya hay una cuenta con ese correo
   */
  String alta(Alta alta, String contrasena, String altaPor);

  /**
   * Reemplaza la contraseña provisoria de una cuenta que todavía no entró.
   *
   * @return false si la cuenta ya eligió su contraseña (no se cambia nada)
   */
  boolean otraContrasena(String id, String contrasena);

  /** Ya hay una cuenta con ese correo. */
  final class CorreoEnUso extends RuntimeException {
    public CorreoEnUso() {
      super("Ya hay una cuenta con ese correo");
    }
  }

  /** El alta falló a mitad de camino; la cuenta creada se borró. */
  final class AltaIncompleta extends RuntimeException {
    public AltaIncompleta(Throwable causa) {
      super("No se creó la cuenta de administración", causa);
    }
  }
}
