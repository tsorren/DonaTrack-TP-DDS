package grupo5.incentivos.models.entities.misiones;

import grupo5.incentivos.models.entities.donante.CategoriaDonante;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import grupo5.incentivos.models.entities.donante.EventoDonacion;
import grupo5.incentivos.models.entities.insignias.Insignia;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;

@Getter
public class MisionCompletitud extends Mision {

  private final Set<String> categoriasDonadas = new HashSet<>();

  public MisionCompletitud(CategoriaDonante categoria, Integer subcategoriasObjetivo) {
    super(
        "Completitud",
        "Realizá donaciones de " + subcategoriasObjetivo + " subcategorías distintas.",
        categoria,
        subcategoriasObjetivo);
  }

  private MisionCompletitud(
      UUID id,
      Integer numeroMision,
      String nombre,
      String descripcion,
      CategoriaDonante categoria,
      Integer objetivo,
      Integer progresoActual,
      boolean completada,
      LocalDate fechaCompletada,
      Insignia insignia,
      Set<String> categoriasDonadas) {
    super(
        id,
        numeroMision,
        nombre,
        descripcion,
        categoria,
        objetivo,
        progresoActual,
        completada,
        fechaCompletada,
        insignia);
    if (categoriasDonadas != null) {
      this.categoriasDonadas.addAll(categoriasDonadas);
    }
  }

  public static MisionCompletitud reconstituir(
      UUID id,
      Integer numeroMision,
      String nombre,
      String descripcion,
      CategoriaDonante categoria,
      Integer objetivo,
      Integer progresoActual,
      boolean completada,
      LocalDate fechaCompletada,
      Insignia insignia,
      Set<String> categoriasDonadas) {
    return new MisionCompletitud(
        id,
        numeroMision,
        nombre,
        descripcion,
        categoria,
        objetivo,
        progresoActual,
        completada,
        fechaCompletada,
        insignia,
        categoriasDonadas);
  }

  @Override
  protected Integer calcularNuevoProgreso(DonanteIncentivos donante, EventoDonacion evento) {
    if (evento.getCategorias() != null) {
      for (String cat : evento.getCategorias()) {
        if (cat != null && !cat.trim().isEmpty()) {
          this.categoriasDonadas.add(cat.trim().toLowerCase(Locale.ROOT));
        }
      }
    }
    return this.categoriasDonadas.size();
  }
}
