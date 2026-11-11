package grupo5.cliente.puertos;

import grupo5.cliente.vistas.AdminAsignacionesVista;
import grupo5.cliente.vistas.AdminCamionesVista;
import grupo5.cliente.vistas.AdminDonacionesRegistroVista;
import grupo5.cliente.vistas.AdminDonacionesVista;
import java.time.LocalDate;
import java.util.List;
import org.springframework.core.io.InputStreamSource;

/**
 * La operación del admin: camiones, asignaciones y donaciones. Lo implementan la maqueta y el
 * adaptador REST; {@code cliente.datos.admin-operacion} elige cuál. {@code accion} es la pantalla
 * de resultado de la maqueta ({@code agregado}, {@code deshabilitado}…; '' es la lista): el
 * adaptador REST la ignora.
 */
public interface AdminOperacionPuerto {

  AdminCamionesVista camiones(String accion);

  /** Agrega un camión y devuelve su clave. */
  String agregarCamion(String patente, float capacidadKg, float volumen, float altura);

  /**
   * Deshabilita el camión (con el motivo, opcional) o lo vuelve a habilitar; devuelve su patente.
   */
  String cambiarCamion(String camion, boolean habilitado, String motivo);

  AdminAsignacionesVista asignaciones();

  /** Corre la asignación ahora y devuelve cuántas propuestas generó. */
  int correrAsignacion();

  /**
   * Aprueba o descarta la propuesta y devuelve el nombre de la entidad que la pidió. Una aprobación
   * que no se puede hacer (la necesidad ya no está activa, la donación es de la misma entidad, la
   * propuesta cambió) es el 409 de Donaciones.
   */
  String decidirPropuesta(String propuesta, boolean aprobar, String motivo);

  /** Todas las donaciones; la maqueta muestra con {@code vencida} la que se marcó vencida. */
  AdminDonacionesVista donaciones(String accion);

  /**
   * Marca vencida una donación del depósito; {@code actor} queda en su historial. Si cambió a la
   * vez, es el 409 de Donaciones.
   */
  void marcarVencida(String donacion, String actor, String motivo);

  /** Los donantes para elegir y los que coinciden con {@code buscar} (null: sin búsqueda). */
  AdminDonacionesRegistroVista registro(String buscar);

  /**
   * Registra una donación con sus bienes (y la foto de cada uno, si la tiene). Un donante dado de
   * baja es el 409 de Donaciones.
   */
  void registrarDonacion(DonacionNueva donacion);

  /** Lo que se registra: quién dona, qué llegó y dónde quedó. */
  record DonacionNueva(
      String donante,
      String descripcion,
      List<BienNuevo> bienes,
      String deposito,
      LocalDate recepcion) {

    public DonacionNueva {
      bienes = List.copyOf(bienes);
    }
  }

  /**
   * Un bien: {@code estado} es NUEVO, USADO o DESGASTADO; {@code foto}, null si no tiene, con su
   * tipo de contenido.
   */
  record BienNuevo(
      String que,
      int cantidad,
      LocalDate vence,
      String estado,
      String tipoFoto,
      InputStreamSource foto) {}
}
