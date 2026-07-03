package grupo5.logistica.dto.entregas;

public record ReportarNoRecepcionRequestDTO(
    String actor, String justificacion, Boolean replanificable) {}
