package grupo5.incentivos.dto;

import java.util.Map;

public record ResumenSistemaDTO(
    int totalDonantes,
    int donantesMesActual,
    int donantesMesAnterior,
    long totalMisionesCompletadas,
    long misionesCompletadasMesActual,
    Map<String, Long> donantesPorCategoria,
    Map<String, Long> evolucionMensual) {}
