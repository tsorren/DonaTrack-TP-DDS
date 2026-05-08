package ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bien;
import lombok.Data;

@Data
public class ItemDonacion {
  private Bien bien;
  private Integer cantidad;
}
