package grupo5.incentivos.models.entities.misiones;

import grupo5.incentivos.models.entities.donante.CategoriaDonante;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import grupo5.incentivos.models.entities.donante.EventoDonacion;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import lombok.Getter;

@Getter
public class MisionCompletitud extends Mision {

  private final Set<String> categoriasdonadas = new HashSet<>();

  public MisionCompletitud(CategoriaDonante categoria, Integer subcategoriasObjetivo) {
    super(
        "Completitud",
        "Realizá donaciones de " + subcategoriasObjetivo + " subcategorías distintas.",
        categoria,
        subcategoriasObjetivo);
  }

  @Override
  protected Integer calcularNuevoProgreso(DonanteIncentivos donante, EventoDonacion evento) {
    if (evento.getCategorias() != null) {
      for (String cat : evento.getCategorias()) {
        if (cat != null && !cat.trim().isEmpty()) {
          this.categoriasdonadas.add(cat.trim().toLowerCase(Locale.ROOT));
        }
      }
    }
    return this.categoriasdonadas.size();
  }
}
