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

    // Borrar final para permitir setter
    private String cronExpression;

    // Usar annotation que inyecta el timer desde application.properties, también permite mockear
    @Value("${planificador.algoritmos.cron.expression}") 
    public void setCronExpression(String expression) {
        this.cronExpression = expression;
    }


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
