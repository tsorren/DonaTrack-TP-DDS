package grupo5.donaciones.infrastructure.persistencia.entities.donaciones;

import grupo5.donaciones.models.entities.donaciones.EstadoDonacion;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "donacion")
@Getter
@Setter
public class DonacionEntity {
  @Id private UUID id;

  // FK a donante en la base; en el dominio es una referencia por id a otro agregado
  @Column(name = "id_donante", nullable = false)
  private UUID donanteId;

  @Column(name = "descripcion", nullable = false)
  private String descripcion;

  @Column(name = "fecha", nullable = false)
  private LocalDateTime fecha;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado_donacion", nullable = false, length = 20)
  private EstadoDonacion estado;

  @ManyToOne
  @JoinColumn(name = "id_deposito_donde_esta")
  private DepositoEntity deposito;

  @OneToMany(mappedBy = "donacion", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<ItemDonacionEntity> items = new ArrayList<>();

  @OneToMany(mappedBy = "donacion", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("timestamp ASC")
  private List<CambioEstadoDonacionEntity> historial = new ArrayList<>();
}
