package grupo5.donaciones.schedulers;

import grupo5.donaciones.services.GestorAlgoritmos;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
public class PlanificadorDeAlgoritmos {

  private final GestorAlgoritmos gestorAlgoritmos;

  private static final char segundos = '0';
  private static final char minutos = '0';
  private static final char horas = '2';
  private static final char diaDelMes = '*';
  private static final char mes = '*';
  private static final char diaDeLaSemana = '?';

  static final String expresionCron = segundos + " " + minutos + " " + horas + " " + diaDelMes + " " + mes + " " + diaDeLaSemana;

  @Autowired
  public PlanificadorDeAlgoritmos(GestorAlgoritmos gestorAlgoritmos) {
    this.gestorAlgoritmos = gestorAlgoritmos;
  }

  @Scheduled(cron = expresionCron)
  public void ejecutarAlgoritmos() {
    gestorAlgoritmos.ejecutar();
  }

  private String traducir(String valor) {
    return switch (valor) {
      case String s when s.matches("[0-9]+") -> s;
      case "*" -> "todos";
      case "?" -> "cualquier";
      case "-" -> "rango";
      case "," -> "únicamente los";
      case "/" -> "cada";
      default -> valor;
    };
  }

  public String paraCuandoEstaPlanificado() {
    String[] partes = expresionCron.split(" ");
    String[] etiquetas = {"segundos", "minutos", "horas", "día del mes", "mes", "día de la semana"};
    StringBuilder descripcion = new StringBuilder("El scheduler está planificado para correr en:\n");
    for (int i = 0; i < partes.length; i++) {
      descripcion.append("  ").append(etiquetas[i]).append(": ").append(traducir(partes[i])).append("\n");
    }
    return descripcion.toString();
  }
}
