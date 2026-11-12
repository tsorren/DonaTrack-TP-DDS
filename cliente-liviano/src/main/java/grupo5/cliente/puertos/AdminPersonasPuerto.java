package grupo5.cliente.puertos;

import grupo5.cliente.vistas.AdminDonantesVista;
import grupo5.cliente.vistas.AdminDonantesVista.Importacion;
import grupo5.cliente.vistas.AdminEntidadesVista;
import grupo5.cliente.vistas.AdminInicioVista;
import grupo5.cliente.vistas.ConsultaDonantes;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

/**
 * Las personas que administra el admin: donantes y entidades, con sus altas y la importación de
 * donantes. Lo implementan la maqueta y el adaptador REST; {@code cliente.datos.admin-personas}
 * elige cuál.
 */
public interface AdminPersonasPuerto {

  /**
   * Una página de donantes con la búsqueda y los filtros de {@code consulta} (R11-01): nunca la
   * lista entera. Los filtros que la fuente no ofrece se descartan.
   */
  AdminDonantesVista donantes(ConsultaDonantes consulta);

  /**
   * El avance de la importación {@code archivo} (su id), que se subió con el nombre {@code nombre},
   * con la página {@code pagina} (desde 1) de sus filas con error si ya terminó (R11-02). {@code
   * ejemplo} es la pantalla pedida ({@code importando}, {@code importado} o {@code importar-error})
   * y, si hay, su escenario después de dos puntos ({@code importando:esperando}): la maqueta elige
   * con eso su ejemplo y el adaptador REST lo ignora. Fuera de la maqueta, sin {@code archivo} no
   * hay importación: null.
   */
  Importacion importacion(UUID archivo, String nombre, String ejemplo, int pagina);

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

  /** El inicio del admin: lo que espera una decisión y cómo está todo. */
  AdminInicioVista inicio();
}
