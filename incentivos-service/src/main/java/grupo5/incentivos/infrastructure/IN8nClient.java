package grupo5.incentivos.infrastructure;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface IN8nClient {

  void publicarInsigniaGanada(
      UUID donanteId, String nombreDonante, String nombreInsignia, String descripcionInsignia);

  void notificarRankingCalculado(String periodo, List<Map<String, Object>> top3);
}
