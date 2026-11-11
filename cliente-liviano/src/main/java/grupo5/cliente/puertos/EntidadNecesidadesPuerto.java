package grupo5.cliente.puertos;

import grupo5.cliente.vistas.EntidadNecesidadesVista;
import java.time.LocalDate;
import java.util.UUID;

/** Necesidades de la entidad (RF-10): leerlas, publicar, cambiar y dar de baja. */
public interface EntidadNecesidadesPuerto {

  /**
   * Las necesidades de la entidad. {@code accion} es la pantalla de después de un envío en la
   * maqueta ({@code publicada}, {@code cambiada}, {@code baja}, {@code publicar-error}) o vacía.
   */
  EntidadNecesidadesVista necesidades(UUID entidad, String accion);

  /** {@code periodicidad}: SEMANAL, QUINCENAL o MENSUAL; null, una sola vez hasta {@code hasta}. */
  void publicar(
      UUID entidad,
      UUID subcategoria,
      int cantidad,
      String periodicidad,
      LocalDate hasta,
      String motivo);

  /** Con la {@code version} leída; si otra persona la cambió, Donaciones responde 409. */
  void cambiar(UUID necesidad, long version, int cantidad, String motivo);

  void darDeBaja(UUID necesidad);
}
