package grupo5.incentivos.infrastructure.persistencia.mappers;

import grupo5.incentivos.infrastructure.persistencia.entities.EntradaRankingEmbeddable;
import grupo5.incentivos.infrastructure.persistencia.entities.RankingMensualEntity;
import grupo5.incentivos.models.entities.ranking.EntradaRanking;
import grupo5.incentivos.models.entities.ranking.RankingMensual;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RankingPersistenciaMapper {

  public RankingMensualEntity toEntity(RankingMensual ranking) {
    if (ranking == null) {
      return null;
    }
    RankingMensualEntity entity = new RankingMensualEntity();
    entity.setId(ranking.getId());
    entity.setPeriodo(ranking.getPeriodo());
    ranking
        .getEntradas()
        .forEach(
            e ->
                entity
                    .getEntradas()
                    .add(
                        new EntradaRankingEmbeddable(
                            e.getPosicion(),
                            e.getDonanteId(),
                            e.getNombreDonante(),
                            e.getMisionesCompletadas())));
    return entity;
  }

  public RankingMensual toDomain(RankingMensualEntity entity) {
    if (entity == null) {
      return null;
    }
    List<EntradaRanking> entradas =
        entity.getEntradas().stream()
            .sorted(Comparator.comparingInt(EntradaRankingEmbeddable::getPosicion))
            .map(
                e ->
                    new EntradaRanking(
                        e.getPosicion(),
                        e.getDonanteId(),
                        e.getNombreDonante(),
                        e.getMisionesCompletadas()))
            .toList();
    return RankingMensual.reconstituir(entity.getId(), entity.getPeriodo(), entradas);
  }
}
