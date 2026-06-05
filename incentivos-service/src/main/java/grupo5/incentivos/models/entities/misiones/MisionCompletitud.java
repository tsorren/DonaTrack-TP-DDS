package grupo5.incentivos.models.entities.misiones;

import grupo5.incentivos.models.entities.donante.CategoriaDonante;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import grupo5.incentivos.models.entities.donante.EventoDonacion;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MisionCompletitud extends Mision {

  private final Set<String> subcategoriasdonadas = new HashSet<>();

  public MisionCompletitud(CategoriaDonante categoria, Integer subcategoriasObjetivo) {
    super(
        "Completitud",
        "Realizá donaciones de " + subcategoriasObjetivo + " subcategorías distintas.",
        categoria,
        subcategoriasObjetivo);
  }

  @Override
  protected Integer calcularNuevoProgreso(DonanteIncentivos donante, EventoDonacion evento) {
    if (evento.getSubcategoria() != null) {
      this.subcategoriasdonadas.add(evento.getSubcategoria());
    }
    return this.subcategoriasdonadas.size();
  }
}
