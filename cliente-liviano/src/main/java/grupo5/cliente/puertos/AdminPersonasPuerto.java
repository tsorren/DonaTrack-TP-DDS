package grupo5.cliente.puertos;

import grupo5.cliente.vistas.AdminDonantesVista;
import grupo5.cliente.vistas.AdminDonantesVista.Importacion;
import grupo5.cliente.vistas.AdminEntidadesVista;
import grupo5.cliente.vistas.AdminImportacionesVista;
import grupo5.cliente.vistas.AdminInicioVista;
import grupo5.cliente.vistas.ConsultaDonantes;
import grupo5.cliente.vistas.ConsultaImportaciones;
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

  /**
   * Una página de importaciones de donantes, en curso e históricas, con la búsqueda y los filtros
   * de {@code consulta} resueltos en la fuente (R11-20): nunca la lista entera; las que siguen en
   * curso, como un número. {@code ejemplo} es el escenario de la maqueta ({@code vacia}, {@code
   * sin-resultados} o {@code con-viejas}); el adaptador REST lo ignora.
   */
  AdminImportacionesVista importaciones(ConsultaImportaciones consulta, String ejemplo);

  /**
   * Las filas con error de la importación {@code archivo}, para bajarlas como CSV (R11-20). Se
   * piden de a tandas mientras se escribe el archivo.
   */
  DescargaDeFilas filasConError(UUID archivo);

  /** Sube el CSV y devuelve el id de la importación. */
  UUID importar(MultipartFile archivo);

  /** Da de alta la persona y la hace donante. */
  void registrarDonante(PersonaNueva persona);

  /** Da de alta la persona jurídica y la hace entidad beneficiaria (queda activa). */
  void registrarEntidad(PersonaNueva.Juridica juridica);

  /**
   * Las entidades: las que piden sumarse, las activas y las inactivas, por nombre. {@code buscar},
   * parte del nombre ('' para todas); {@code pagina}, desde 1. La fuente que pide de a una página
   * (R11-01) resuelve allá la búsqueda y, desde R11-43, los filtros ({@code filtro}); la maqueta
   * devuelve la lista entera y la pantalla filtra.
   */
  AdminEntidadesVista entidades(String buscar, AdminEntidadesVista.Filtro filtro, int pagina);

  /** Aprueba a la entidad que pidió sumarse (su {@code clave}) y devuelve su nombre. */
  String aprobarEntidad(String clave);

  /** El inicio del admin: lo que espera una decisión y cómo está todo. */
  AdminInicioVista inicio();
}
