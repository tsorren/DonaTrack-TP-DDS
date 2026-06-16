package grupo5.donaciones.schedulers;

import grupo5.donaciones.services.GestorAlgoritmos;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Getter
@Component
public class PlanificadorDeAlgoritmos {

  private final GestorAlgoritmos gestorAlgoritmos;

  // Inyecta el timer desde application.properties, también permite mockear en tests
  @Value("${planificador.algoritmos.cron.expression}")
  private String cronExpression;

  @Autowired
  public PlanificadorDeAlgoritmos(GestorAlgoritmos gestorAlgoritmos) {
    this.gestorAlgoritmos = gestorAlgoritmos;
  }

  @Scheduled(cron = "${planificador.algoritmos.cron.expression}")
  public void ejecutarAlgoritmos() {
    gestorAlgoritmos.ejecutar();
  }

  private String traducir(String valor) {
    return switch (valor) {
      case "*" -> "todos";
      case "?" -> "cualquier";
      case "-" -> "rango";
      case "," -> "únicamente los";
      case "/" -> "cada";
      default -> valor;
    };
  }

  public String paraCuandoEstaPlanificado() {
    String[] partes = cronExpression.split(" ");
    String[] etiquetas = {"segundos", "minutos", "horas", "día del mes", "mes", "día de la semana"};
    StringBuilder descripcion =
        new StringBuilder("El scheduler está planificado para correr en:\n");
    for (int i = 0; i < partes.length; i++) {
      descripcion
          .append("  ")
          .append(etiquetas[i])
          .append(": ")
          .append(traducir(partes[i]))
          .append("\n");
    }
    return descripcion.toString();
  }
}
