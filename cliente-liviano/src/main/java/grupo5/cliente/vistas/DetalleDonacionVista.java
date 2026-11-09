package grupo5.cliente.vistas;

import grupo5.cliente.vistas.MisDonacionesVista.Donacion;
import java.util.List;

/**
 * El detalle de una donación: la misma fila de Mis donaciones y, además, sus etapas, el aviso si
 * tuvo un problema, la foto de la entidad y las noticias, de la más nueva a la primera.
 *
 * @param llego la fecha de entrega ("9 de octubre de 2026"), o null si no llegó
 * @param problema el aviso de "con problema", o null
 * @param foto la URL de la foto de la entidad, o null
 * @param pieFoto el pie de la foto, o lo que dice una entregada sin foto; null si no llegó
 * @param figuraComo el nombre que ve la entidad, o null mientras la API no lo da (HB-20)
 */
public record DetalleDonacionVista(
    Donacion donacion,
    String llego,
    List<PasoRecorrido> pasos,
    Problema problema,
    String foto,
    String fotoAlt,
    String pieFoto,
    List<Noticia> noticias,
    String figuraComo) {

  /** Qué pasó, con la última noticia. */
  public record Problema(String titulo, String texto) {}

  /**
   * Una noticia: {@code cuando} ya escrito para leer ("Hoy, 14:30"), {@code fecha} para {@code
   * <time datetime>}.
   */
  public record Noticia(String cuando, String fecha, String texto) {}
}
