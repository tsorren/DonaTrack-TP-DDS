package grupo5.cliente.vistas;

import java.util.List;

/**
 * Una donación del donante con todo lo que muestran el inicio y Seguimiento, con los mismos nombres
 * que las de templates/viaje-datos.html (que siguen leyendo las pantallas de la persona y los
 * componentes huerta y la entrega de Seguimiento). Un dato que no hay es null.
 *
 * @param categoria el ícono del sprite de categorías ("cat-alimentos"), o null
 * @param destino la entidad, o null si todavía no tiene
 * @param llego la fecha de entrega ("9 de octubre de 2026"), o null
 * @param seguimiento lo que dice la hoja de ruta de una entrega en curso, o null
 * @param cartas las noticias de la donación, de la más nueva a la primera
 */
public record DonanteDonacionVista(
    String clave,
    long numero,
    String titulo,
    String cantidad,
    String categoria,
    String categoriaTexto,
    String destino,
    String donada,
    String llego,
    EstadoEntrega estado,
    String nota,
    String foto,
    String fotoAlt,
    String seguimiento,
    List<PasoRecorrido> pasos,
    List<Carta> cartas) {

  /** Una noticia: {@code cuando} para leer ("14 de octubre, 17:00"), {@code fecha} en ISO. */
  public record Carta(String texto, String cuando, String fecha) {}
}
