package ar.edu.utn.frba.ddsi.donaciones.models.ports;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.personas.Humana;
import java.util.List;

public interface CargadorDonantes {
  List<Humana> cargarDonantes(String rutaArchivo);
}
