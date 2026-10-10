package grupo5.incentivos.infrastructure.persistencia.entities;

import grupo5.incentivos.infrastructure.persistencia.YearMonthAttributeConverter;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ranking_mensual")
@Getter
@Setter
@NoArgsConstructor
public class RankingMensualEntity {

  @Id private UUID id;

  @Convert(converter = YearMonthAttributeConverter.class)
  @Column(name = "periodo", nullable = false, unique = true, length = 7)
  private YearMonth periodo;

  // FetchType.EAGER justificado: el mapeo a dominio ocurre fuera de la transacción del repo.
  // Única colección de la entidad, así que un bag no provoca MultipleBagFetchException; el mapper
  // ordena por posicion al reconstruir el dominio.
  @SuppressWarnings("squid:S1319")
  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
      name = "ranking_mensual_entrada",
      joinColumns = @JoinColumn(name = "ranking_id", nullable = false))
  private List<EntradaRankingEmbeddable> entradas = new ArrayList<>();
}
