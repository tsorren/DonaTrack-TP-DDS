package grupo5.cliente.vistas;

import java.util.List;

/** Modelo de vista para la pantalla de misiones e insignias del donante (RF-07). */
public record MisionesVista(
    boolean persona,
    boolean nueva,
    String bajada,
    List<Mision> misiones,
    List<Categoria> categorias,
    List<Mision> obtenidas) {

  public record Mision(
      String categoria,
      String nombre,
      String pide,
      String estado,
      String fecha,
      int progreso,
      int objetivo,
      String cuenta,
      String nota,
      String espera,
      Insignia insignia) {}

  public record Categoria(String nombre, String estado, String siguiente, String anterior) {}

  public record Insignia(
      String nombre, String descripcion, String imagenUrl, boolean visible, String fechaObtenida) {}

  public static MisionesVista sinPermiso() {
    return new MisionesVista(false, false, null, List.of(), List.of(), List.of());
  }
}
