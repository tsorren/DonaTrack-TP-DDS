package grupo5.donaciones.infrastructure.persistencia.entities.itemsNormalizados;

import grupo5.donaciones.models.entities.donaciones.Estado;
import grupo5.donaciones.models.entities.itemsNormalizados.EstadoNormalizacion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * Copia del bien original en cada ítem (D4 del ADR de desnormalización): foto de lo donado al
 * momento de normalizar.
 */
@Entity
@Table(name = "item_donacion_normalizado")
@Getter
@Setter
public class ItemDonacionNormalizadoEntity {
  @Id private UUID id;

  @Column(name = "id_donacion", nullable = false)
  private UUID donacionId;

  @Column(name = "cantidad", nullable = false)
  private Long cantidad;

  @Column(name = "segmentado", nullable = false)
  private boolean segmentado;

  // Del DER; el dominio no tiene nombre del bien, solo descripción
  @Column(name = "bien_nombre")
  private String bienNombre;

  @Column(name = "bien_descripcion", nullable = false)
  private String bienDescripcion;

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

  @Column(name = "id_subcategoria", nullable = false)
  private UUID subcategoriaId;

  @Column(name = "confianza_normalizacion", nullable = false)
  private Double confianza;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado_normalizacion", nullable = false, length = 20)
  private EstadoNormalizacion estadoNormalizacion;
}
