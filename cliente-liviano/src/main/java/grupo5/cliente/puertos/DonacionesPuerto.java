package grupo5.cliente.puertos;

import grupo5.cliente.vistas.DetalleDonacionVista;
import grupo5.cliente.vistas.MisDonacionesVista;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/** Las donaciones de un donante, para Mis donaciones y su detalle. */
public interface DonacionesPuerto {

  MisDonacionesVista misDonaciones(UUID donante);

  /**
   * Las del donante registradas en {@code mes} ("Ver todas" de un mes del inicio de la persona).
   * Por defecto filtra las de {@link #misDonaciones(UUID)} por la fecha que muestran (la maqueta);
   * el adaptador REST se lo pide filtrado a Donaciones.
   */
  default MisDonacionesVista misDonaciones(UUID donante, YearMonth mes) {
    MisDonacionesVista todas = misDonaciones(donante);
    String sufijo =
        mes.format(DateTimeFormatter.ofPattern("MMMM 'de' yyyy", Locale.forLanguageTag("es-AR")));
    return new MisDonacionesVista(
        todas.donaciones().stream().filter(d -> d.donada().endsWith(sufijo)).toList(),
        todas.categorias(),
        todas.actualizado());
  }

  /**
   * Una donación del donante por su clave; vacío si no existe o no es suya. Sin clave, la maqueta
   * muestra la que va en camino.
   */
  Optional<DetalleDonacionVista> detalle(UUID donante, String clave);
}
