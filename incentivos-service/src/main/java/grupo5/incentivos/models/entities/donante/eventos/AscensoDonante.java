package grupo5.incentivos.models.entities.donante.eventos;

import grupo5.incentivos.models.entities.donante.CategoriaDonante;
import java.util.UUID;
import lombok.Getter;

@Getter
public class AscensoDonante extends EventoDonanteIncentivos {
  private final CategoriaDonante categoriaAnterior;
  private final CategoriaDonante categoriaNueva;

  public AscensoDonante(
      UUID donanteId,
      UUID idPersona,
      CategoriaDonante categoriaAnterior,
      CategoriaDonante categoriaNueva) {
    super(donanteId, idPersona);
    this.categoriaAnterior = categoriaAnterior;
    this.categoriaNueva = categoriaNueva;
  }

  public CategoriaDonante categoriaAnterior() {
    return categoriaAnterior;
  }

  public CategoriaDonante categoriaNueva() {
    return categoriaNueva;
  }
}
