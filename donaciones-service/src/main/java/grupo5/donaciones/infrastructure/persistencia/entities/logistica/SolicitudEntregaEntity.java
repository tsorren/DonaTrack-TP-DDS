package grupo5.donaciones.infrastructure.persistencia.entities.logistica;

import grupo5.donaciones.models.entities.logistica.EstadoSolicitudEntrega;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "solicitud_entrega")
@Getter
@Setter
public class SolicitudEntregaEntity {
  @Id private UUID id;

  // FK a donacion_independiente en la base; en el dominio, una referencia por id
  @Column(name = "id_donacion_independiente", nullable = false)
  private UUID donacionIndependienteId;

  @Column(name = "proveedor_actual")
  private String proveedorActual;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado", nullable = false, length = 20)
  private EstadoSolicitudEntrega estado;

  @Column(name = "ronda", nullable = false)
  private Integer ronda;

  @Column(name = "fecha_creacion", nullable = false)
  private LocalDateTime fechaCreacion;

  // D-37: provisoria, pendiente de discusión (alternativa: tabla hija)
  @JdbcTypeCode(SqlTypes.ARRAY)
  @Column(name = "proveedores_descartados", nullable = false)
  private List<String> proveedoresDescartados = new ArrayList<>();
}
