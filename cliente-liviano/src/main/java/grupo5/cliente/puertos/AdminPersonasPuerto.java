package grupo5.cliente.puertos;

import grupo5.cliente.vistas.AdminDonantesVista;
import grupo5.cliente.vistas.AdminDonantesVista.Importacion;
import grupo5.cliente.vistas.AdminEntidadesVista;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

/**
 * Las personas que administra el admin: donantes y entidades, con sus altas y la importación de
 * donantes. Lo implementan la maqueta y el adaptador REST; {@code cliente.datos.admin-personas}
 * elige cuál.
 */
public interface AdminPersonasPuerto {

  AdminDonantesVista donantes();

  /**
   * El avance de la importación {@code archivo} (su id), que se subió con el nombre {@code nombre}.
   * Fuera de la maqueta, sin {@code archivo} no hay importación: null.
   */
  Importacion importacion(UUID archivo, String nombre);

  /** Sube el CSV y devuelve el id de la importación. */
  UUID importar(MultipartFile archivo);

  /** Da de alta la persona y la hace donante. */
  void registrarDonante(PersonaNueva persona);

  /** Da de alta la persona jurídica y la hace entidad beneficiaria (queda activa). */
  void registrarEntidad(PersonaNueva.Juridica juridica);

  /** Las entidades: las que piden sumarse, las activas y las inactivas, por nombre. */
  AdminEntidadesVista entidades();

  /** Aprueba a la entidad que pidió sumarse (su {@code clave}) y devuelve su nombre. */
  String aprobarEntidad(String clave);
}
