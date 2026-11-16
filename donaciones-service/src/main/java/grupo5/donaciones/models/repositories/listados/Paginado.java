package grupo5.donaciones.models.repositories.listados;

/** Qué tramo de un listado se pide: la página (desde 0) y su tamaño, ya validados. */
public record Paginado(int pagina, int tamanio) {

  public long desplazamiento() {
    return (long) pagina * tamanio;
  }
}
