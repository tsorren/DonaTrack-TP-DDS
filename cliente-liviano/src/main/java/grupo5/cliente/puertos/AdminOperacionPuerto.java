package grupo5.cliente.puertos;

import grupo5.cliente.vistas.AdminAsignacionesVista;
import grupo5.cliente.vistas.AdminCamionesVista;
import grupo5.cliente.vistas.AdminDonacionesRegistroVista;
import grupo5.cliente.vistas.AdminDonacionesVista;
import grupo5.cliente.vistas.ConsultaDonaciones;
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

  /**
   * Una página de donaciones con el filtro y el orden de {@code consulta} (R11-01): nunca la lista
   * entera. La maqueta muestra con {@code vencida} la que se marcó vencida.
   */
  AdminDonacionesVista donaciones(ConsultaDonaciones consulta, String accion);

  /**
   * Una donación con su historial, pedida por su clave; null si no existe (la maqueta da la primera
   * de ejemplo).
   */
  AdminDonacionesVista.Donacion donacion(String clave, String accion);

  /**
   * Marca vencida una donación del depósito; {@code actor} queda en su historial. Si cambió a la
   * vez, es el 409 de Donaciones.
   */
  void marcarVencida(String donacion, String actor, String motivo);

  /**
   * Los donantes que coinciden con {@code buscar} (null: sin búsqueda), como máximo 10 y con
   * cuántos coinciden, y el elegido por su clave ({@code donante}, null si no hay).
   */
  AdminDonacionesRegistroVista registro(String buscar, String donante);

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
