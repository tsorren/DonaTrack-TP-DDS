package grupo5.donaciones.infrastructure.persistencia.entities.donaciones;

import grupo5.donaciones.models.entities.donaciones.Estado;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "item_donacion")
@Getter
@Setter
public class ItemDonacionEntity {
  @Id private UUID id = UUID.randomUUID();

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_donacion", nullable = false)
  private DonacionEntity donacion;

  @Column(name = "descripcion", nullable = false)
  private String descripcion;

  @Column(name = "foto_url")
  private String fotoUrl;

  @Column(name = "fecha_vencimiento")
  private LocalDate fechaVencimiento;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado", length = 20)
  private Estado estado;

  @Column(name = "peso_unitario", nullable = false, precision = 12, scale = 3)
  private BigDecimal pesoUnitario;

  @Column(name = "volumen_unitario", nullable = false, precision = 12, scale = 3)
  private BigDecimal volumenUnitario;

  @Column(name = "cantidad", nullable = false)
  private Long cantidad;
}
