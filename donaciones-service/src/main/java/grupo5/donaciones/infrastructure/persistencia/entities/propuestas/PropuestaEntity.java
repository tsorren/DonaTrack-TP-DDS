package grupo5.donaciones.infrastructure.persistencia.entities.propuestas;

import grupo5.donaciones.models.entities.propuestas.EstadoPropuesta;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "propuesta")
@Getter
@Setter
public class PropuestaEntity {
  @Id private UUID id;

  // FK a necesidad en la base; en el dominio es una referencia por id a otro agregado
  @Column(name = "id_necesidad_que_satisface", nullable = false)
  private UUID necesidadId;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado", nullable = false, length = 20)
  private EstadoPropuesta estado;

  @Column(name = "fecha_creacion", nullable = false)
  private LocalDateTime fechaCreacion;

  @Column(name = "decidida_por")
  private String decididaPor;

  @Column(name = "fecha_decision")
  private LocalDateTime fechaDecision;

  @OneToMany(mappedBy = "propuesta", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<PosibleFragmentacionEntity> fragmentaciones = new ArrayList<>();

  // R11-24 (V24): qué algoritmos la sugirieron; se leen por lote (default_batch_fetch_size)
  @ElementCollection
  @CollectionTable(
      name = "sugerencia_propuesta",
      joinColumns = @JoinColumn(name = "id_propuesta", nullable = false))
  private List<SugerenciaPropuestaEmbeddable> sugerencias = new ArrayList<>();
}
