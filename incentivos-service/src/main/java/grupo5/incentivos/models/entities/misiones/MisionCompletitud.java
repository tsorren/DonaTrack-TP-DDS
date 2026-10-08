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

  private final Set<String> categoriasDonadas = new HashSet<>();

  public MisionCompletitud(CategoriaDonante categoria, Integer subcategoriasObjetivo) {
    super(
        "Completitud",
        "Realizá donaciones de " + subcategoriasObjetivo + " subcategorías distintas.",
        categoria,
        subcategoriasObjetivo);
  }

  private MisionCompletitud(MisionEstado estado, Set<String> categoriasDonadas) {
    super(estado);
    if (categoriasDonadas != null) {
      this.categoriasDonadas.addAll(categoriasDonadas);
    }
  }

  public static MisionCompletitud reconstituir(MisionEstado estado, Set<String> categoriasDonadas) {
    return new MisionCompletitud(estado, categoriasDonadas);
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
