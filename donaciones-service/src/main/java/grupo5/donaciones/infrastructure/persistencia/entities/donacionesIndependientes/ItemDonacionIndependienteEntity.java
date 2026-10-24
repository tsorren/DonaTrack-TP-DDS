package grupo5.donaciones.infrastructure.persistencia.entities.donacionesIndependientes;

import grupo5.donaciones.infrastructure.persistencia.entities.itemsNormalizados.ItemDonacionNormalizadoEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "item_donacion_independiente")
@Getter
@Setter
public class ItemDonacionIndependienteEntity {
  @Id private UUID id = UUID.randomUUID();

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_donacion_independiente", nullable = false)
  private DonacionIndependienteEntity donacion;

  // D-24: de acá se reconstituye el bien normalizado
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_item_normalizado", nullable = false)
  private ItemDonacionNormalizadoEntity itemNormalizado;

  @Column(name = "cantidad", nullable = false)
  private Long cantidad;

  // D4: foto del texto del bien
  @Column(name = "bien_descripcion", nullable = false)
  private String bienDescripcion;
}
