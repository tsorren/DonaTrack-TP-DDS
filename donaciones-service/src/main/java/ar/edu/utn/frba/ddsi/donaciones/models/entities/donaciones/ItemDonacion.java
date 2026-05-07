package ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones;

import Bienes.Bien;
import lombok.Data;

@Data
public class ItemDonacion {
  private Bien bien;
  private Integer cantidad;
}
